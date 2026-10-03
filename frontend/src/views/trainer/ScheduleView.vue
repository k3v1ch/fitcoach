<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        v-model="searchQuery"
        title="Расписание занятий"
        subtitle="Календарь тренировок и сборов"
        search-placeholder="Поиск по названию..."
      >
        <template #actions>
          <BaseButton v-if="canCreate" @click="handleAddEvent">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить событие
          </BaseButton>
        </template>
      </PageHeader>

      <div class="schedule-controls">
        <div class="view-tabs">
          <button
            v-for="view in views"
            :key="view.id"
            class="view-tab"
            :class="{ active: currentView === view.id }"
            @click="switchView(view.id)"
          >
            {{ view.label }}
          </button>
        </div>
        <div class="filters">
          <select v-model="filters.groupId" class="filter-select" aria-label="Группа">
            <option value="">Все группы</option>
            <option v-for="g in groupOptions" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <select v-model="filters.coachId" class="filter-select" aria-label="Тренер">
            <option value="">Все тренеры</option>
            <option v-for="c in coaches" :key="c.userId" :value="c.userId">{{ c.fullName }}</option>
          </select>
        </div>
        <div class="date-nav">
          <button class="nav-btn" @click="prevPeriod">
            <BaseIcon name="chevron-left" :size="14" color="#152421" />
          </button>
          <span class="date-range">{{ currentDateLabel }}</span>
          <button class="nav-btn" @click="nextPeriod">
            <BaseIcon name="chevron-right" :size="14" color="#152421" />
          </button>
        </div>
      </div>

      <!-- Неделя -->
      <div v-if="currentView === 'week'" class="schedule-card">
        <StateBlock v-if="loading" kind="loading" />
        <StateBlock v-else-if="error" kind="error" :message="errorText(error)" />
        <template v-else>
          <div class="schedule-header">
            <div class="time-col">ВРЕМЯ</div>
            <div
              v-for="day in weekDays"
              :key="day.key"
              class="day-col day-link"
              :class="{ today: day.isToday }"
              @click="openDay(day.date)"
            >{{ day.label }}</div>
          </div>
          <div v-if="weekEvents.some(cell => cell.length)" class="schedule-row">
            <div class="time-col">СОБЫТИЯ</div>
            <div v-for="(cell, i) in weekEvents" :key="i" class="day-col">
              <div v-for="item in cell" :key="item.key" class="event" :class="item.color" :title="item.tooltip">
                <span class="event-text">{{ item.title }}</span>
              </div>
            </div>
          </div>
          <div v-for="slot in schedule" :key="slot.time" class="schedule-row">
            <div class="time-col">{{ slot.time }}</div>
            <div v-for="(cell, i) in slot.cells" :key="i" class="day-col">
              <div
                v-for="item in cell"
                :key="item.key"
                class="event clickable"
                :class="item.color"
                :title="item.tooltip"
                @click="openItem(item)"
              >
                <span class="event-text">{{ item.time }} {{ item.title }}</span>
              </div>
            </div>
          </div>
          <StateBlock v-if="!weekHasItems" kind="empty" message="На этой неделе тренировок и мероприятий нет" />
          <div class="legend">
            <span class="legend-title">Обозначения:</span>
            <span v-for="item in LEGEND" :key="item.label" class="legend-item">
              <span class="dot" :style="{ background: item.color }"></span> {{ item.label }}
            </span>
          </div>
        </template>
      </div>

      <!-- Месяц -->
      <div v-else-if="currentView === 'month'" class="schedule-card">
        <h3>Месяц: {{ currentDateLabel }}</h3>
        <StateBlock v-if="loading" kind="loading" />
        <StateBlock v-else-if="error" kind="error" :message="errorText(error)" />
        <template v-else>
          <div class="month-grid">
            <div v-for="name in DAY_NAMES" :key="name" class="month-head">{{ name }}</div>
            <div
              v-for="cell in monthCells"
              :key="cell.key"
              class="month-cell"
              :class="{ outside: !cell.inMonth, today: cell.isToday }"
            >
              <button class="day-number" @click="openDay(cell.date)">{{ cell.date.getDate() }}</button>
              <div
                v-for="item in cell.items.slice(0, MONTH_CELL_LIMIT)"
                :key="item.key"
                class="event small"
                :class="[item.color, { clickable: item.kind === 'training' }]"
                :title="item.tooltip"
                @click="openItem(item)"
              >
                <span class="event-text">{{ item.time }} {{ item.title }}</span>
              </div>
              <button v-if="cell.items.length > MONTH_CELL_LIMIT" class="more-link" @click="openDay(cell.date)">
                ещё {{ cell.items.length - MONTH_CELL_LIMIT }}
              </button>
            </div>
          </div>
          <StateBlock v-if="!trainingItems.length && !eventItems.length" kind="empty" message="В этом месяце тренировок и мероприятий нет" />
          <div class="legend">
            <span class="legend-title">Обозначения:</span>
            <span v-for="item in LEGEND" :key="item.label" class="legend-item">
              <span class="dot" :style="{ background: item.color }"></span> {{ item.label }}
            </span>
          </div>
        </template>
      </div>

      <!-- День -->
      <div v-else class="schedule-card">
        <h3>День: {{ currentDateLabel }}</h3>
        <StateBlock v-if="loading" kind="loading" />
        <StateBlock v-else-if="error" kind="error" :message="errorText(error)" />
        <StateBlock v-else-if="!dayItems.length" kind="empty" message="В этот день тренировок и мероприятий нет" />
        <div v-else class="day-list">
          <div
            v-for="item in dayItems"
            :key="item.key"
            class="day-item"
            :class="{ clickable: item.kind === 'training' }"
            :title="item.tooltip"
            @click="openItem(item)"
          >
            <div class="time-col">{{ item.timeRange }}</div>
            <div class="day-info">
              <div class="day-title">{{ item.title }}</div>
              <div v-if="item.details" class="day-sub">{{ item.details }}</div>
            </div>
            <span class="event badge" :class="item.color">{{ item.badge }}</span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { trainingsApi } from '../../api/trainings'
import { eventsApi } from '../../api/events'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { organizationsApi } from '../../api/organizations'
import { getOrganizationId, hasPermission } from '../../utils/session'
import { parseDate, formatDate, formatTime, toIsoDate, toIsoDateTime, periodRange, addDays, errorText } from '../../utils/format'
import { label, tone } from '../../utils/labels'

const router = useRouter()

const views = [
  { id: 'week', label: 'Неделя' },
  { id: 'month', label: 'Месяц' },
  { id: 'day', label: 'День' }
]
const DAY_NAMES = ['ПН', 'ВТ', 'СР', 'ЧТ', 'ПТ', 'СБ', 'ВС']
const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь']
const MONTHS_GENITIVE = ['Января', 'Февраля', 'Марта', 'Апреля', 'Мая', 'Июня', 'Июля', 'Августа', 'Сентября', 'Октября', 'Ноября', 'Декабря']
const MONTH_CELL_LIMIT = 3
// Цвет плашки: тренировка — по статусу (tone), мероприятие — жёлтый
const LEGEND = [
  { label: label('trainingStatus', 'PLANNED'), color: '#35678E' },
  { label: label('trainingStatus', 'COMPLETED'), color: '#2E8B57' },
  { label: label('trainingStatus', 'CANCELLED'), color: '#D64545' },
  { label: 'Мероприятие', color: '#F2B705' }
]

const currentView = ref('week')
const currentDate = ref(new Date())
const searchQuery = ref('')
const filters = reactive({ groupId: '', coachId: '' })

const groups = ref([])
const venues = ref([])
const coaches = ref([])
const trainings = ref([])
const events = ref([])
const loading = ref(false)
const error = ref(null)

const canCreate = computed(() => hasPermission('schedule.write'))

// ─────────── загрузка ───────────

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

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  loading.value = true
  error.value = null
  try {
    const org = getOrganizationId()
    const { from, to } = periodRange(currentView.value, currentDate.value)
    const q = searchQuery.value.trim() || undefined
    const [trainingList, eventList] = await Promise.all([
      // GET /trainings: [from, to) в datetime, возвращаются пересекающие период занятия
      fetchAll(p => trainingsApi.list(org, {
        from: toIsoDateTime(from),
        to: toIsoDateTime(to),
        q,
        groupId: filters.groupId || undefined,
        coachId: filters.coachId || undefined,
        ...p
      })),
      // GET /events фильтрует по дате начала — берём с запасом назад, чтобы увидеть уже идущие сборы
      hasPermission('events.read')
        ? fetchAll(p => eventsApi.list(org, {
          from: toIsoDate(addDays(from, -31)),
          to: toIsoDate(addDays(to, -1)),
          q,
          ...p
        }))
        : Promise.resolve([])
    ])
    if (seq !== loadSeq) return
    trainings.value = trainingList
    events.value = eventList
  } catch (e) {
    if (seq === loadSeq) error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// Группы, площадки и тренеры — для фильтров и подписей; без них календарь всё равно работает
async function loadReference() {
  const org = getOrganizationId()
  const [groupList, venueList, coachList] = await Promise.allSettled([
    fetchAll(p => groupsApi.list(org, p)),
    fetchAll(p => dictionariesApi.list(org, 'venues', p)),
    hasPermission('members.read')
      ? fetchAll(p => organizationsApi.members(org, { role: 'TRAINER', status: 'ACTIVE', ...p }))
      : Promise.resolve([])
  ])
  groups.value = groupList.status === 'fulfilled' ? groupList.value : []
  venues.value = venueList.status === 'fulfilled' ? venueList.value : []
  coaches.value = coachList.status === 'fulfilled'
    ? [...coachList.value].sort((a, b) => (a.fullName || '').localeCompare(b.fullName || '', 'ru'))
    : []
}

watch([currentView, currentDate, () => filters.groupId, () => filters.coachId], load)

let searchTimer = null
watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(load, 300)
})
onBeforeUnmount(() => clearTimeout(searchTimer))

onMounted(() => {
  loadReference()
  load()
})

// ─────────── элементы календаря ───────────

const groupById = computed(() => new Map(groups.value.map(g => [g.id, g])))
const venueById = computed(() => new Map(venues.value.map(v => [v.id, v])))
const groupOptions = computed(() => groups.value
  .filter(g => g.status === 'ACTIVE')
  .sort((a, b) => a.name.localeCompare(b.name, 'ru')))

const sameDay = (a, b) => toIsoDate(a) === toIsoDate(b)
const pad = n => String(n).padStart(2, '0')
const shortDate = d => `${pad(d.getDate())}.${pad(d.getMonth() + 1)}`

const trainingItems = computed(() => trainings.value.map(t => {
  const start = new Date(t.startsAt)
  const end = new Date(t.endsAt)
  const group = groupById.value.get(t.groupId)?.name
  const venue = venueById.value.get(t.venueId)?.name
  const status = label('trainingStatus', t.status)
  const timeRange = `${formatTime(start)}–${formatTime(end)}`
  return {
    key: `t-${t.id}`,
    kind: 'training',
    id: t.id,
    title: t.title,
    start,
    end,
    time: formatTime(start),
    timeRange,
    color: tone(t.status),
    badge: status,
    details: [group, venue].filter(Boolean).join(' · '),
    tooltip: [t.title, timeRange, group, venue, status, t.cancelReason && `Причина отмены: ${t.cancelReason}`]
      .filter(Boolean).join(' · ')
  }
}))

const eventItems = computed(() => {
  // При выбранной группе показываем мероприятия её секции
  const sectionId = filters.groupId ? groupById.value.get(filters.groupId)?.sectionId : null
  return events.value
    .filter(e => !sectionId || e.sectionId === sectionId)
    .map(e => {
      // Денежный сбор не имеет времени — показываем его в день срока collectionDueOn
      const allDay = !e.startsAt
      const start = allDay ? parseDate(e.collectionDueOn) : new Date(e.startsAt)
      if (!start || isNaN(start)) return null
      const end = allDay || !e.endsAt ? start : new Date(e.endsAt)
      const type = label('eventType', e.type)
      const status = label('eventStatus', e.status)
      const timeRange = allDay
        ? 'Весь день'
        : sameDay(start, end) ? `${formatTime(start)}–${formatTime(end)}` : `${shortDate(start)}–${shortDate(end)}`
      return {
        key: `e-${e.id}`,
        kind: 'event',
        id: e.id,
        title: e.title,
        start,
        end,
        time: '',
        timeRange,
        color: e.status === 'CANCELLED' ? 'red' : 'yellow',
        badge: `${type} · ${status}`,
        details: [type, e.location].filter(Boolean).join(' · '),
        tooltip: [type, e.title, allDay ? `срок ${formatDate(e.collectionDueOn)}` : timeRange, e.location, status]
          .filter(Boolean).join(' · ')
      }
    })
    .filter(Boolean)
})

function trainingsOn(date) {
  const key = toIsoDate(date)
  return trainingItems.value.filter(item => toIsoDate(item.start) === key)
}

// Мероприятие попадает во все дни, которые пересекает
function eventsOn(date) {
  const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const dayEnd = addDays(dayStart, 1)
  return eventItems.value.filter(item => item.start < dayEnd && (item.end > dayStart || item.start >= dayStart))
}

// ─────────── неделя ───────────

const weekDays = computed(() => {
  const { from } = periodRange('week', currentDate.value)
  const todayKey = toIsoDate(new Date())
  return DAY_NAMES.map((name, i) => {
    const date = addDays(from, i)
    return { key: toIsoDate(date), date, isToday: toIsoDate(date) === todayKey, label: `${name} ${date.getDate()}` }
  })
})

const weekEvents = computed(() => weekDays.value.map(day => eventsOn(day.date)))

// Строки по два часа (как в макете), диапазон расширяется под фактические занятия
const slotOf = item => Math.floor(item.start.getHours() / 2) * 2
const schedule = computed(() => {
  const cells = weekDays.value.map(day => trainingsOn(day.date))
  const slots = cells.flat().map(slotOf)
  const first = Math.min(8, ...slots)
  const last = Math.max(18, ...slots)
  const rows = []
  for (let hour = first; hour <= last; hour += 2) {
    rows.push({
      time: `${pad(hour)}:00`,
      cells: cells.map(list => list.filter(item => slotOf(item) === hour))
    })
  }
  return rows
})

const weekHasItems = computed(() =>
  schedule.value.some(row => row.cells.some(cell => cell.length)) || weekEvents.value.some(cell => cell.length))

// ─────────── месяц и день ───────────

const monthCells = computed(() => {
  const { from, to } = periodRange('month', currentDate.value)
  const start = addDays(from, -((from.getDay() + 6) % 7))
  const lastDay = addDays(to, -1)
  const end = addDays(lastDay, 6 - ((lastDay.getDay() + 6) % 7))
  const todayKey = toIsoDate(new Date())
  const cells = []
  for (let date = start; date <= end; date = addDays(date, 1)) {
    const inMonth = date.getMonth() === from.getMonth()
    cells.push({
      key: toIsoDate(date),
      date,
      inMonth,
      isToday: toIsoDate(date) === todayKey,
      items: inMonth ? [...eventsOn(date), ...trainingsOn(date)] : []
    })
  }
  return cells
})

const dayItems = computed(() => [...eventsOn(currentDate.value), ...trainingsOn(currentDate.value)])

// ─────────── навигация ───────────

const currentDateLabel = computed(() => {
  const d = currentDate.value
  if (currentView.value === 'week') {
    const { from } = periodRange('week', d)
    const end = addDays(from, 6)
    const startPart = from.getMonth() === end.getMonth()
      ? `${from.getDate()}`
      : `${from.getDate()} ${MONTHS_GENITIVE[from.getMonth()]}`
    return `${startPart} — ${end.getDate()} ${MONTHS_GENITIVE[end.getMonth()]} ${end.getFullYear()}`
  }
  if (currentView.value === 'month') {
    return `${MONTHS[d.getMonth()]} ${d.getFullYear()}`
  }
  const weekday = d.toLocaleDateString('ru-RU', { weekday: 'long' })
  return `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)}, ${formatDate(d)}`
})

const switchView = (id) => {
  currentView.value = id
}

function shiftPeriod(step) {
  const d = currentDate.value
  if (currentView.value === 'week') currentDate.value = addDays(d, 7 * step)
  else if (currentView.value === 'month') currentDate.value = new Date(d.getFullYear(), d.getMonth() + step, 1)
  else currentDate.value = addDays(d, step)
}
const prevPeriod = () => shiftPeriod(-1)
const nextPeriod = () => shiftPeriod(1)

function openDay(date) {
  currentDate.value = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  currentView.value = 'day'
}

function openItem(item) {
  if (item.kind === 'training') router.push(`/trainer/trainings/${item.id}/report`)
}

// Форма тренировки живёт на экране «Тренировки»: открываем её с датой из календаря
const handleAddEvent = () => router.push({
  path: '/trainer/trainings',
  query: { create: '1', date: toIsoDate(currentDate.value) }
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.schedule-controls {
  display: flex; justify-content: space-between;
  align-items: center; flex-wrap: wrap; gap: 16px;
}
.view-tabs { display: flex; gap: 8px; }
.view-tab {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.view-tab.active { background: #102522; color: white; border-color: #102522; }

.filters { display: flex; gap: 8px; flex-wrap: wrap; }
.filter-select {
  padding: 8px 12px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer; outline: none; font-family: inherit;
}

.date-nav { display: flex; align-items: center; gap: 16px; }
.nav-btn {
  padding: 8px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  cursor: pointer; display: flex;
  justify-content: center; align-items: center;
}
.nav-btn:hover { background: #F4F7F8; }
.date-range { font-size: 16px; font-weight: 700; color: #152421; }

.schedule-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
  overflow-x: auto;
}
.schedule-card h3 { font-size: 16px; font-weight: 700; color: #152421; }
.schedule-header, .schedule-row {
  display: flex; gap: 12px;
  min-width: 900px;
}
.schedule-header { padding-bottom: 12px; border-bottom: 1px solid #E3EAE8; }
.time-col {
  width: 80px; font-size: 13px; font-weight: 700;
  color: #6D7D79; flex-shrink: 0;
}
.day-col {
  flex: 1; min-width: 0; text-align: center;
  font-size: 12px; font-weight: 700; color: #6D7D79;
}
.day-link { cursor: pointer; }
.day-link:hover, .day-col.today { color: #152421; }
.schedule-row {
  padding: 16px 0; border-bottom: 1px solid #E3EAE8;
  align-items: center;
}
.schedule-row .day-col {
  min-height: 40px; border-radius: 8px;
  border: 1px solid #E3EAE8;
  display: flex; flex-direction: column; justify-content: center; gap: 2px;
}
.event {
  width: 100%; min-height: 38px; border-radius: 8px;
  display: flex; justify-content: center; align-items: center;
  font-size: 12px; font-weight: 700;
  padding: 0 6px; overflow: hidden;
}
.event-text { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.event.clickable { cursor: pointer; }
.event.clickable:hover { filter: brightness(0.96); }
.event.green { background: #E9F7D5; color: #2E8B57; }
.event.red { background: #FCE2E5; color: #D64545; }
.event.yellow { background: #FFF8E6; color: #F2B705; }
.event.blue { background: #DDECFB; color: #35678E; }
.event.gray { background: #F4F7F8; color: #888888; }

.legend {
  display: flex; gap: 24px; flex-wrap: wrap;
  padding-top: 12px; align-items: center;
}
.legend-title { font-size: 13px; font-weight: 600; color: #152421; }
.legend-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #6D7D79;
}
.dot { width: 12px; height: 12px; border-radius: 50%; }

/* Месяц */
.month-grid {
  display: grid; grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px; min-width: 900px;
}
.month-head { font-size: 12px; font-weight: 700; color: #6D7D79; text-align: center; padding-bottom: 4px; }
.month-cell {
  min-height: 104px; padding: 6px;
  border: 1px solid #E3EAE8; border-radius: 8px;
  display: flex; flex-direction: column; gap: 4px;
}
.month-cell.outside { background: #F9FBFA; }
.month-cell.today { border-color: #152421; }
.day-number {
  align-self: flex-start; padding: 0;
  background: none; border: none;
  font-size: 12px; font-weight: 700; color: #152421; cursor: pointer;
}
.month-cell.outside .day-number { color: #98A6A2; }
.event.small { min-height: 0; padding: 3px 6px; font-size: 11px; justify-content: flex-start; }
.more-link {
  align-self: flex-start; padding: 0;
  background: none; border: none;
  font-size: 11px; font-weight: 600; color: #6D7D79; cursor: pointer;
}

/* День */
.day-list { display: flex; flex-direction: column; }
.day-item {
  display: flex; align-items: center; gap: 16px;
  padding: 14px 8px; border-bottom: 1px solid #E3EAE8;
}
.day-item.clickable { cursor: pointer; }
.day-item.clickable:hover { background: #F9FBFA; }
.day-item .time-col { width: 120px; }
.day-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.day-title { font-size: 14px; font-weight: 700; color: #152421; }
.day-sub { font-size: 13px; color: #6D7D79; }
.event.badge {
  width: auto; min-height: 0; flex-shrink: 0;
  padding: 4px 12px; border-radius: 999px;
}
</style>
