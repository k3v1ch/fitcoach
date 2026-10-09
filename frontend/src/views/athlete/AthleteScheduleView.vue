<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader
        title="Моё расписание"
        subtitle="Ваш персональный календарь тренировок и спаррингов"
        :show-search="false"
      >
        <template #actions>
          <div v-if="groupChip" class="coach-card">
            <span>{{ groupChip }}</span>
            <span class="online-dot"></span>
          </div>
        </template>
      </PageHeader>

      <StateBlock v-if="pageState" :kind="pageState.kind" :message="pageState.message" />

      <template v-else>
        <!-- Управление календарём -->
        <div class="calendar-control">
          <div class="calendar-nav">
            <button class="nav-btn" :title="viewMode === 'week' ? 'Предыдущая неделя' : 'Предыдущий месяц'" @click="prevPeriod">
              <BaseIcon name="chevron-left" :size="14" color="#152421" />
            </button>
            <span class="current-month">{{ currentPeriodLabel }}</span>
            <button class="nav-btn" :title="viewMode === 'week' ? 'Следующая неделя' : 'Следующий месяц'" @click="nextPeriod">
              <BaseIcon name="chevron-right" :size="14" color="#152421" />
            </button>
          </div>
          <div class="view-switcher">
            <button
              class="switch-btn"
              :class="{ active: viewMode === 'week' }"
              @click="viewMode = 'week'"
            >Неделя</button>
            <button
              class="switch-btn"
              :class="{ active: viewMode === 'month' }"
              @click="viewMode = 'month'"
            >Месяц</button>
          </div>
        </div>

        <StateBlock v-if="loading" kind="loading" />
        <StateBlock v-else-if="error" kind="error" :message="errorText(error)" />

        <!-- Вид: НЕДЕЛЯ -->
        <div v-else-if="viewMode === 'week'" class="schedule-grid">
          <div class="table-header">
            <div
              v-for="day in days"
              :key="day.key"
              class="day-header"
              :class="{ active: day.active }"
            >
              <span>{{ day.label }} {{ day.num }}</span>
            </div>
          </div>

          <div class="slots-row">
            <div v-for="day in days" :key="day.key" class="slot-column">
              <div v-if="!day.slots.length" class="empty-slot">Нет тренировок</div>
              <div
                v-for="slot in day.slots"
                :key="slot.id"
                class="event-slot"
                :style="{ background: slot.bg, borderColor: slot.border }"
                @click="openTraining(slot.id)"
              >
                <div class="slot-time" :style="{ color: slot.timeColor }">{{ slot.time }}</div>
                <div class="slot-title">{{ slot.title }}</div>
                <div class="slot-location" :style="{ color: slot.locColor }">{{ slot.location }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Вид: МЕСЯЦ -->
        <div v-else class="month-grid">
          <div v-for="name in WEEKDAYS" :key="name" class="month-weekday">{{ name }}</div>
          <div
            v-for="day in monthDays"
            :key="day.key"
            class="month-cell"
            :class="{ today: day.today, hasEvent: day.events.length, blank: day.blank }"
          >
            <template v-if="!day.blank">
              <div class="month-num">{{ day.num }}</div>
              <div
                v-for="event in day.events"
                :key="event.id"
                class="month-event"
                :class="{ cancelled: event.cancelled }"
                :title="event.hint"
                @click="openTraining(event.id)"
              >
                <span class="month-event-time">{{ event.time }}</span>
                <span class="month-event-title">{{ event.title }}</span>
              </div>
              <span v-if="day.more" class="month-more">ещё {{ day.more }}</span>
            </template>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { trainingsApi } from '../../api/trainings'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { myAthletes, selectedAthleteId, loadMyAthletes, getOrganizationId } from '../../utils/session'
import {
  formatDate, formatTime, formatWeekday, toIsoDate, toIsoDateTime, periodRange, addDays, errorText
} from '../../utils/format'
import { label } from '../../utils/labels'

const NO_CARD = 'Карточка спортсмена ещё не создана тренером'
const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь']
const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']
// Цвета карточек: сегодняшние — акцент, остальные по статусу тренировки
const PALETTE = {
  today: { bg: '#B7F34B', border: '#B7F34B', timeColor: '#102522', locColor: '#102522' },
  PLANNED: { bg: '#DDECFB', border: '#E3EAE8', timeColor: '#35678E', locColor: '#35678E' },
  COMPLETED: { bg: '#E9F7D5', border: '#E3EAE8', timeColor: '#2D5B24', locColor: '#2D5B24' },
  CANCELLED: { bg: '#F4F7F8', border: '#E3EAE8', timeColor: '#D64545', locColor: '#D64545' }
}

const router = useRouter()
const viewMode = ref('week')
const anchor = ref(new Date())

// Границы видимого периода: неделя с понедельника или календарный месяц; to не включается
const range = computed(() => periodRange(viewMode.value === 'week' ? 'week' : 'month', anchor.value))

const currentPeriodLabel = computed(() => {
  const { from, to } = range.value
  if (viewMode.value === 'month') return `${MONTHS[from.getMonth()]} ${from.getFullYear()}`
  const last = addDays(to, -1)
  return from.getMonth() === last.getMonth()
    ? `${from.getDate()} – ${formatDate(last)}`
    : `${formatDate(from, { withYear: from.getFullYear() !== last.getFullYear() })} – ${formatDate(last)}`
})

const prevPeriod = () => shiftPeriod(-1)
const nextPeriod = () => shiftPeriod(1)

function shiftPeriod(step) {
  const d = anchor.value
  anchor.value = viewMode.value === 'week'
    ? addDays(d, step * 7)
    : new Date(d.getFullYear(), d.getMonth() + step, 1)
}

// ─────────── Своя карточка ───────────
const athletesReady = ref(false)
const athletesError = ref(null)

const pageState = computed(() => {
  if (!athletesReady.value) return { kind: 'loading', message: '' }
  if (athletesError.value) return { kind: 'error', message: errorText(athletesError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CARD }
  return null
})

// ─────────── Тренировки периода ───────────
const trainings = ref([])
const loading = ref(false)
const error = ref(null)

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  error.value = null
  if (!athleteId) {
    trainings.value = []
    loading.value = false
    return
  }
  loading.value = true
  try {
    const { from, to } = range.value
    // GET /trainings: [from, to) в datetime — занятия групп спортсмена за период
    const list = await fetchAll(p => trainingsApi.list(getOrganizationId(), p), {
      athleteId, from: toIsoDateTime(from), to: toIsoDateTime(to)
    })
    if (seq !== loadSeq) return
    trainings.value = list
  } catch (e) {
    if (seq !== loadSeq) return
    trainings.value = []
    error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// ─────────── Подписи: группы спортсмена и площадки ───────────
const groups = ref([])
const venues = ref([])

async function loadGroups() {
  const athleteId = selectedAthleteId.value
  if (!athleteId) {
    groups.value = []
    return
  }
  try {
    const list = await fetchAll(p => groupsApi.list(getOrganizationId(), p), { athleteId, status: 'ACTIVE' })
    if (athleteId === selectedAthleteId.value) groups.value = list
  } catch (_) {
    groups.value = []
  }
}

async function loadVenues() {
  try {
    venues.value = await fetchAll(p => dictionariesApi.list(getOrganizationId(), 'venues', p))
  } catch (_) {
    venues.value = []
  }
}

const groupById = computed(() => new Map(groups.value.map(g => [g.id, g])))
const venueById = computed(() => new Map(venues.value.map(v => [v.id, v])))

const groupChip = computed(() => {
  const names = groups.value.map(g => g.name).filter(Boolean)
  if (!names.length) return ''
  if (names.length === 1) return `Группа: ${names[0]}`
  return names.length === 2 ? `Группы: ${names.join(', ')}` : `Группы: ${names[0]} и ещё ${names.length - 1}`
})

onMounted(async () => {
  loadVenues()
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (e) {
    athletesError.value = e
  } finally {
    athletesReady.value = true
  }
  if (!athletesError.value) {
    load()
    loadGroups()
  }
})

watch(selectedAthleteId, () => {
  if (!athletesReady.value || athletesError.value) return
  load()
  loadGroups()
})

watch(() => `${viewMode.value}:${toIsoDate(range.value.from)}`, () => {
  if (athletesReady.value && !athletesError.value) load()
})

// ─────────── Сетка недели ───────────
const trainingsByDay = computed(() => {
  const map = new Map()
  const sorted = [...trainings.value].sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt))
  for (const t of sorted) {
    const key = toIsoDate(new Date(t.startsAt))
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(t)
  }
  return map
})

const days = computed(() => {
  const todayKey = toIsoDate(new Date())
  const from = range.value.from
  return Array.from({ length: 7 }, (_, i) => {
    const d = addDays(from, i)
    const key = toIsoDate(d)
    return {
      key,
      label: formatWeekday(d),
      num: d.getDate(),
      active: key === todayKey,
      slots: (trainingsByDay.value.get(key) || []).map(toSlot)
    }
  })
})

function toSlot(t) {
  const start = new Date(t.startsAt)
  const end = new Date(t.endsAt)
  const isToday = toIsoDate(start) === toIsoDate(new Date())
  const palette = t.status === 'PLANNED' && isToday ? PALETTE.today : PALETTE[t.status] || PALETTE.PLANNED
  const place = [venueById.value.get(t.venueId)?.name, groupById.value.get(t.groupId)?.name].filter(Boolean).join(' · ')
  const location = t.status === 'CANCELLED'
    ? `${label('trainingStatus', 'CANCELLED')}${t.cancelReason ? `: ${t.cancelReason}` : ''}`
    : place || label('trainingStatus', t.status)
  return {
    id: t.id,
    time: `${formatTime(start)} - ${formatTime(end)}`,
    title: t.title,
    location,
    ...palette
  }
}

// ─────────── Сетка месяца ───────────
const monthDays = computed(() => {
  const first = range.value.from
  const lead = (first.getDay() + 6) % 7 // пустые клетки до первого числа (неделя с понедельника)
  const total = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate()
  const todayKey = toIsoDate(new Date())
  const cells = Array.from({ length: lead }, (_, i) => ({ key: `blank-${i}`, blank: true, events: [] }))
  for (let n = 1; n <= total; n++) {
    const key = toIsoDate(new Date(first.getFullYear(), first.getMonth(), n))
    const list = trainingsByDay.value.get(key) || []
    cells.push({
      key,
      num: n,
      today: key === todayKey,
      events: list.slice(0, 2).map(t => ({
        id: t.id,
        time: formatTime(t.startsAt),
        title: t.title,
        cancelled: t.status === 'CANCELLED',
        hint: `${formatTime(t.startsAt)} – ${formatTime(t.endsAt)} ${t.title}${t.status === 'CANCELLED' ? ' (отменена)' : ''}`
      })),
      more: Math.max(0, list.length - 2)
    })
  }
  return cells
})

const openTraining = (id) => {
  router.push(`/athlete/schedule/${id}`)
}

// Все страницы списка Page<T> (size ≤ 100), не больше maxPages запросов
async function fetchAll(request, params = {}, maxPages = 10) {
  const first = await request({ ...params, page: 0, size: 100 })
  const pages = Math.min(first?.totalPages || 1, maxPages)
  const rest = await Promise.all(
    Array.from({ length: pages - 1 }, (_, i) => request({ ...params, page: i + 1, size: 100 }))
  )
  return rest.reduce((all, page) => all.concat(page?.items || []), first?.items || [])
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

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
  background: #A44450; border-radius: 50%;
}
.coach-card {
  height: 44px; padding: 0 16px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #6D7D79;
}
.online-dot { width: 6px; height: 6px; background: #B7F34B; border-radius: 50%; }

.calendar-control {
  display: flex; justify-content: space-between; align-items: center;
  background: white; border: 1px solid #E3EAE8; border-radius: 16px;
  padding: 16px;
}
.calendar-nav { display: flex; align-items: center; gap: 12px; }
.nav-btn {
  width: 36px; height: 36px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 8px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}
.nav-btn:hover { background: #F4F7F8; }
.current-month { font-size: 16px; font-weight: 700; color: #152421; }

.view-switcher {
  display: flex; gap: 4px; padding: 4px;
  background: #F4F7F8; border-radius: 8px;
}
.switch-btn {
  padding: 6px 16px; border: none; background: transparent;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  border-radius: 6px; cursor: pointer;
}
.switch-btn.active { background: white; color: #152421; }

/* Неделя */
.schedule-grid { display: flex; flex-direction: column; gap: 16px; }
.table-header { display: flex; gap: 12px; padding: 0 16px; }
.day-header {
  flex: 1; padding: 12px;
  background: white; border: 1px solid #E3EAE8; border-radius: 12px;
  text-align: center; font-size: 14px; font-weight: 700; color: #152421;
}
.day-header.active { background: #102522; color: #B7F34B; }

.slots-row { display: flex; gap: 12px; padding: 0 16px; }
.slot-column { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 12px; }
.empty-slot { text-align: center; color: #98A6A2; font-size: 12px; font-weight: 600; padding: 8px 0; }

.event-slot {
  padding: 12px; border-radius: 12px; border: 1px solid;
  display: flex; flex-direction: column; gap: 8px;
  cursor: pointer; transition: transform 0.15s;
}
.event-slot:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(23,52,46,0.1); }
.slot-time { font-size: 11px; font-weight: 700; }
.slot-title { font-size: 13px; font-weight: 700; color: #152421; overflow-wrap: anywhere; }
.slot-location { font-size: 11px; font-weight: 400; overflow-wrap: anywhere; }

/* Месяц */
.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  background: white;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #E3EAE8;
}
.month-weekday { text-align: center; font-size: 12px; font-weight: 700; color: #98A6A2; padding: 4px 0; }
.month-cell {
  min-height: 90px;
  padding: 8px;
  background: #F4F7F8;
  border-radius: 10px;
  display: flex; flex-direction: column; gap: 6px;
  min-width: 0;
}
.month-cell.blank { background: transparent; }
.month-cell.today { background: #102522; }
.month-cell.today .month-num { color: #B7F34B; }
.month-cell.hasEvent { background: #E9F7D5; }
.month-cell.today.hasEvent { background: #102522; }
.month-num { font-size: 13px; font-weight: 700; color: #152421; }
.month-event {
  padding: 4px 6px; background: #B7F34B; border-radius: 6px;
  display: flex; flex-direction: column; gap: 2px; cursor: pointer;
}
.month-event.cancelled { background: #FCE2E5; }
.month-event.cancelled .month-event-title { text-decoration: line-through; }
.month-event-time { font-size: 9px; font-weight: 700; color: #102522; }
.month-event-title { font-size: 10px; font-weight: 600; color: #152421; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.month-more { font-size: 10px; font-weight: 600; color: #6D7D79; }
.month-cell.today .month-more { color: #98A6A2; }
</style>
