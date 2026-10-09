<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        title="Расписание тренировок"
        subtitle="Полный календарь спортивных занятий и мероприятий"
        :show-search="false"
      >
        <template #actions>
          <div v-if="selectedAthlete" class="child-selector">
            <div class="child-avatar">{{ initials(fullName(selectedAthlete)) }}</div>
            <span>{{ fullName(selectedAthlete) }}</span>
            <template v-if="myAthletes.length > 1">
              <BaseIcon name="chevron-down" :size="14" />
              <select
                class="child-select-overlay"
                aria-label="Выбрать ребёнка"
                :value="selectedAthleteId"
                @change="selectAthlete($event.target.value)"
              >
                <option v-for="a in myAthletes" :key="a.id" :value="a.id">{{ fullName(a) }}</option>
              </select>
            </template>
          </div>
          <router-link to="/parent/announcements" class="icon-btn" aria-label="Объявления">
            <BaseIcon name="bell" :size="19" color="#152421" />
            <span v-if="hasUnread" class="dot"></span>
          </router-link>
        </template>
      </PageHeader>

      <div class="control-bar">
        <div class="tabs">
          <button
            class="tab-btn"
            :class="{ active: view === 'upcoming' }"
            @click="view = 'upcoming'"
          >
            Предстоящие
          </button>
          <button
            class="tab-btn"
            :class="{ active: view === 'past' }"
            @click="view = 'past'"
          >
            Прошедшие
          </button>
        </div>
        <div class="month-control">
          <button class="nav-btn" aria-label="Предыдущая неделя" @click="prevWeek">
            <BaseIcon name="chevron-left" :size="14" color="#152421" />
          </button>
          <span class="month-label">{{ weekLabel }}</span>
          <button class="nav-btn" aria-label="Следующая неделя" @click="nextWeek">
            <BaseIcon name="chevron-right" :size="14" color="#152421" />
          </button>
        </div>
      </div>

      <div v-if="pageState" class="empty-state">
        <StateBlock :kind="pageState.kind" :message="pageState.message" />
      </div>

      <div v-else-if="view === 'upcoming'" class="calendar-card">
        <div class="calendar-header">
          <div class="time-col"></div>
          <div v-for="day in currentWeek" :key="day.key" class="col-header" :class="{ active: day.active }">
            <div class="day-name">{{ day.name }}</div>
            <div class="day-num">{{ day.num }}</div>
          </div>
        </div>

        <StateBlock v-if="!slots.length" kind="empty" message="На этой неделе предстоящих занятий и мероприятий нет" />

        <div v-for="slot in slots" :key="slot.key" class="calendar-row">
          <div class="time-col">{{ slot.time }}</div>
          <div class="events-row">
            <div
              v-for="(cell, i) in slot.cells"
              :key="i"
              class="event-cell"
              :class="cell ? cell.color : 'empty'"
              :title="cell ? cell.hint : null"
            >
              <template v-if="cell">
                <div class="event-title">{{ cell.title }}</div>
                <div class="event-meta">{{ cell.meta }}</div>
              </template>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <p>Прошедшие тренировки за эту неделю</p>
        <StateBlock v-if="!pastItems.length" kind="empty" message="На этой неделе прошедших тренировок нет" />
        <div v-else class="past-list">
          <div v-for="item in pastItems" :key="item.id" class="past-item">
            <span class="past-date">{{ item.date }}</span>
            <span>{{ item.title }}</span>
            <span class="past-status" :class="item.statusClass" :title="item.hint || null">{{ item.status }}</span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { trainingsApi } from '../../api/trainings'
import { eventsApi } from '../../api/events'
import { attendanceApi } from '../../api/attendance'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { announcementsApi } from '../../api/announcements'
import {
  myAthletes, selectedAthleteId, selectedAthlete, loadMyAthletes, selectAthlete, getOrganizationId
} from '../../utils/session'
import {
  formatDate, formatTime, formatWeekday, toIsoDate, toIsoDateTime, periodRange, addDays,
  fullName, initials, errorText
} from '../../utils/format'
import { label, tone } from '../../utils/labels'

const NO_CHILD = 'Ребёнок ещё не привязан — обратитесь к тренеру'
const ALL_DAY = 'Весь день'

const view = ref('upcoming')
const weekOffset = ref(0)

const weekStart = computed(() => addDays(periodRange('week', new Date()).from, weekOffset.value * 7))

const prevWeek = () => weekOffset.value--
const nextWeek = () => weekOffset.value++

// ─────────── загрузка ───────────

const childrenReady = ref(false)
const childrenError = ref(null)

const trainings = ref([])
const events = ref([])
const marks = ref(new Map()) // trainingId → запись журнала посещаемости ребёнка
const loading = ref(false)
const error = ref(null)

const groups = ref([])
const venues = ref([])
const hasUnread = ref(false)

// Все страницы списка Page<T> (size не больше 100)
async function fetchAll(request, maxPages = 10) {
  const items = []
  for (let page = 0; page < maxPages; page++) {
    const res = await request({ page, size: 100 })
    items.push(...(res?.items || []))
    if (page + 1 >= (res?.totalPages || 0)) break
  }
  return items
}

// GET /events пока не применяет athleteId (сервер отдаёт мероприятия всех детей родителя),
// поэтому при нескольких детях оставляем только те, где участвует выбранный ребёнок.
async function onlyChildEvents(org, list, athleteId) {
  if (!list.length || myAthletes.value.length < 2) return list
  const keep = await Promise.all(list.map(async e => {
    const res = await eventsApi.participants(org, e.id)
    const people = Array.isArray(res) ? res : (res?.items || [])
    return people.some(p => p.athleteId === athleteId)
  }))
  return list.filter((_, i) => keep[i])
}

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  trainings.value = []
  events.value = []
  marks.value = new Map()
  error.value = null
  if (!athleteId) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const org = getOrganizationId()
    const from = weekStart.value
    const to = addDays(from, 7)
    const today = new Date()
    const lastDay = addDays(to, -1)
    const journalTo = lastDay < today ? lastDay : today
    const [trainingList, eventList, journal] = await Promise.all([
      // GET /trainings: [from, to) в datetime — занятия групп ребёнка за неделю
      fetchAll(p => trainingsApi.list(org, {
        athleteId, from: toIsoDateTime(from), to: toIsoDateTime(to), ...p
      })),
      // GET /events без from/to: сервер фильтрует период по COALESCE(collectionDueOn, startsAt), и сбор
      // со сроком оплаты выпал бы из своей недели. Родителю видны только мероприятия его детей — список
      // небольшой, пересечение с неделей проверяем ниже по startsAt/endsAt.
      fetchAll(p => eventsApi.list(org, { athleteId, ...p })),
      // GET /attendance: отметки ребёнка по прошедшим занятиям недели
      from <= today
        ? fetchAll(p => attendanceApi.list(org, {
          athleteId, from: toIsoDate(from), to: toIsoDate(journalTo), ...p
        }))
        : Promise.resolve([])
    ])
    const weekEvents = eventList.filter(e => {
      if (!e.startsAt || e.status === 'DRAFT') return false
      const start = new Date(e.startsAt)
      const end = e.endsAt ? new Date(e.endsAt) : start
      return start < to && end >= from
    })
    const childEvents = await onlyChildEvents(org, weekEvents, athleteId)
    if (seq !== loadSeq) return
    trainings.value = trainingList
    events.value = childEvents
    marks.value = new Map(journal.map(m => [m.trainingId, m]))
  } catch (e) {
    if (seq === loadSeq) error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// Названия групп и площадок для подписей и отметка о новых объявлениях; без них расписание всё равно работает
async function loadReference() {
  try {
    const org = getOrganizationId()
    const [groupList, venueList, unread] = await Promise.allSettled([
      fetchAll(p => groupsApi.list(org, p)),
      fetchAll(p => dictionariesApi.list(org, 'venues', p)),
      announcementsApi.list(org, { status: 'PUBLISHED', unread: true, size: 1 })
    ])
    groups.value = groupList.status === 'fulfilled' ? groupList.value : []
    venues.value = venueList.status === 'fulfilled' ? venueList.value : []
    hasUnread.value = unread.status === 'fulfilled' && (unread.value?.totalElements || 0) > 0
  } catch (_) {
    // нет активной организации — основной экран покажет ошибку сам
  }
}

onMounted(async () => {
  loadReference()
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (e) {
    childrenError.value = e
  } finally {
    childrenReady.value = true
  }
  if (!childrenError.value) load()
})

watch([selectedAthleteId, weekOffset], () => {
  if (childrenReady.value && !childrenError.value) load()
})

const pageState = computed(() => {
  if (!childrenReady.value) return { kind: 'loading', message: '' }
  if (childrenError.value) return { kind: 'error', message: errorText(childrenError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CHILD }
  if (loading.value) return { kind: 'loading', message: '' }
  if (error.value) return { kind: 'error', message: errorText(error.value) }
  return null
})

// ─────────── неделя ───────────

const weekLabel = computed(() => {
  const from = weekStart.value
  const last = addDays(from, 6)
  return from.getMonth() === last.getMonth()
    ? `${from.getDate()} – ${formatDate(last)}`
    : `${formatDate(from, { withYear: from.getFullYear() !== last.getFullYear() })} – ${formatDate(last)}`
})

const currentWeek = computed(() => {
  const todayKey = toIsoDate(new Date())
  return Array.from({ length: 7 }, (_, i) => {
    const d = addDays(weekStart.value, i)
    const key = toIsoDate(d)
    return { key, name: formatWeekday(d), num: d.getDate(), active: key === todayKey }
  })
})

// ─────────── сетка «Предстоящие» ───────────

const groupById = computed(() => new Map(groups.value.map(g => [g.id, g])))
const venueById = computed(() => new Map(venues.value.map(v => [v.id, v])))

const sameDay = (a, b) => toIsoDate(a) === toIsoDate(b)

// Элементы сетки: незавершённые тренировки (включая отменённые — родителю важно это видеть) и мероприятия
const upcomingItems = computed(() => {
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const list = []
  for (const t of trainings.value) {
    const start = new Date(t.startsAt)
    const end = new Date(t.endsAt)
    if (end <= now) continue
    const cancelled = t.status === 'CANCELLED'
    const meta = cancelled
      ? `${label('trainingStatus', 'CANCELLED')}${t.cancelReason ? `: ${t.cancelReason}` : ''}`
      : [venueById.value.get(t.venueId)?.name, groupById.value.get(t.groupId)?.name].filter(Boolean).join(' · ')
    const time = `${formatTime(start)} – ${formatTime(end)}`
    list.push({
      start,
      end,
      allDay: false,
      time,
      title: t.title,
      meta: meta || label('trainingStatus', t.status),
      color: cancelled ? 'red' : t.status === 'COMPLETED' ? 'blue' : 'green',
      hint: `${time} ${t.title}${meta ? ` · ${meta}` : ''}`
    })
  }
  for (const e of events.value) {
    const start = new Date(e.startsAt)
    const end = e.endsAt ? new Date(e.endsAt) : start
    // Без времени окончания мероприятие показываем до конца дня начала
    if (e.endsAt ? end <= now : start < todayStart) continue
    const meta = [label('eventType', e.type), e.location].filter(Boolean).join(' · ')
    const time = e.endsAt ? `${formatTime(start)} – ${formatTime(end)}` : formatTime(start)
    list.push({
      start,
      end,
      allDay: !sameDay(start, end),
      time,
      title: e.title,
      meta: e.status === 'CANCELLED' ? label('eventStatus', 'CANCELLED') : meta,
      color: e.status === 'CANCELLED' ? 'red' : 'yellow',
      hint: `${time} ${e.title}${meta ? ` · ${meta}` : ''}`
    })
  }
  return list
})

// Строки — интервалы времени, столбцы — дни недели (многодневные мероприятия — в строке «Весь день»)
const slots = computed(() => {
  const days = currentWeek.value.map(d => d.key)
  const rows = new Map()
  for (const item of upcomingItems.value) {
    const key = item.allDay ? ALL_DAY : item.time
    if (!rows.has(key)) {
      rows.set(key, {
        key,
        time: key,
        order: item.allDay ? -1 : item.start.getHours() * 60 + item.start.getMinutes(),
        cells: days.map(() => null)
      })
    }
    const row = rows.get(key)
    const dayIndexes = item.allDay
      ? days.map((_, i) => i).filter(i => {
        const dayStart = addDays(weekStart.value, i)
        return item.start < addDays(dayStart, 1) && item.end >= dayStart
      })
      : [days.indexOf(toIsoDate(item.start))].filter(i => i >= 0)
    for (const i of dayIndexes) {
      const cell = row.cells[i]
      if (!cell) {
        row.cells[i] = { title: item.title, meta: item.meta, color: item.color, hint: item.hint, more: 0 }
      } else {
        cell.more++
        cell.hint += `\n${item.hint}`
      }
    }
  }
  return [...rows.values()]
    .sort((a, b) => a.order - b.order || a.key.localeCompare(b.key))
    .map(row => ({
      ...row,
      cells: row.cells.map(c => (c && c.more ? { ...c, meta: `${c.meta} · ещё ${c.more}` } : c))
    }))
})

// ─────────── «Прошедшие»: тренировки недели, которые уже закончились, и отметка ребёнка ───────────

const pastItems = computed(() => {
  const now = new Date()
  return trainings.value
    .filter(t => new Date(t.endsAt) <= now)
    .sort((a, b) => new Date(b.startsAt) - new Date(a.startsAt))
    .map(t => {
      const cancelled = t.status === 'CANCELLED'
      const mark = marks.value.get(t.id)
      const status = mark?.status || 'UNMARKED'
      return {
        id: t.id,
        date: `${formatDate(t.startsAt, { withYear: false })}, ${formatTime(t.startsAt)}`,
        title: t.title,
        status: cancelled ? label('trainingStatus', 'CANCELLED') : label('attendanceStatus', status),
        statusClass: `status-${cancelled ? 'gray' : tone(status)}`,
        hint: cancelled ? t.cancelReason : [mark?.reason, mark?.comment].filter(Boolean).join(' · ')
      }
    })
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.child-selector {
  position: relative;
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
}
.child-avatar {
  width: 24px; height: 24px;
  background: #DDECFB; color: #35678E;
  border-radius: 99px;
  display: flex; justify-content: center; align-items: center;
  font-size: 10px; font-weight: 700;
}
/* Прозрачный нативный список поверх плашки ребёнка — переключение без смены вида */
.child-select-overlay {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  opacity: 0; cursor: pointer;
}

.control-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.tabs { display: flex; gap: 12px; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }

.month-control { display: flex; align-items: center; gap: 12px; }
.nav-btn {
  padding: 8px; background: white; border: 1px solid #E3EAE8;
  border-radius: 8px; cursor: pointer;
  display: flex; justify-content: center; align-items: center;
}
.month-label { font-size: 15px; font-weight: 700; color: #152421; }

.calendar-card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
  overflow-x: auto;
}
.calendar-header {
  display: flex; gap: 12px; min-width: 900px;
}
.col-header {
  flex: 1; padding: 12px;
  background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.col-header.active { background: #B7F34B; }
.day-name { font-size: 12px; font-weight: 600; color: #6D7D79; }
.day-num { font-size: 18px; font-weight: 800; color: #152421; }

.calendar-row {
  display: flex; gap: 12px; min-width: 900px;
  align-items: center;
}
.time-col {
  width: 100px; font-size: 11px; font-weight: 700;
  color: #6D7D79; text-align: center;
  flex-shrink: 0;
}
.events-row {
  flex: 1; display: flex; gap: 12px;
}
.event-cell {
  flex: 1; height: 96px; border-radius: 12px;
  padding: 12px;
  display: flex; flex-direction: column; gap: 6px;
  outline: 1px solid #E3EAE8; outline-offset: -1px;
  min-width: 0; overflow: hidden;
}
.event-cell.empty { background: transparent; outline-color: transparent; }
.event-cell.green { background: #E9F7D5; }
.event-cell.lime { background: #B7F34B; }
.event-cell.yellow { background: #FFF1D6; }
.event-cell.blue { background: #DDECFB; }
.event-cell.red { background: #FCE2E5; }
.event-title { font-size: 12px; font-weight: 700; color: #152421; }
.event-meta { font-size: 11px; color: #6D7D79; }

.icon-btn {
  position: relative;
  width: 44px; height: 44px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}
.icon-btn .dot {
  position: absolute; top: 11px; right: 10px;
  width: 8px; height: 8px;
  background: #D64545; border-radius: 50%;
}

.empty-state {
  background: white; border-radius: 20px; padding: 40px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 16px;
}
.empty-state p { color: #6D7D79; font-size: 14px; }
.past-list { display: flex; flex-direction: column; gap: 12px; }
.past-item {
  display: flex; justify-content: space-between;
  padding: 12px 16px; background: #F4F7F8;
  border-radius: 12px; font-size: 14px;
}
.past-date { color: #6D7D79; }
.past-status { color: #2E8B57; font-weight: 700; }
.past-status.status-green { color: #2E8B57; }
.past-status.status-yellow { color: #8B6914; }
.past-status.status-red { color: #D64545; }
.past-status.status-blue { color: #3B82F6; }
.past-status.status-gray { color: #888888; }
</style>
