<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader
        :title="greeting"
        :subtitle="subtitle"
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
        <!-- Quick info -->
        <section class="metrics-grid">
          <MetricCard
            label="Следующая тренировка"
            :value="nextTrainingValue"
            :note="nextTrainingNote"
            icon="activity"
            color="#E9F7D5"
            icon-color="#2E8B57"
          />
          <MetricCard
            label="Ближайшее событие"
            :value="nextEventValue"
            :note="nextEventNote"
            icon="award"
            color="#FFF1D6"
            icon-color="#A36A16"
          />
        </section>

        <!-- Content Split -->
        <section class="content-grid">
          <!-- Левая колонка: Расписание на неделю -->
          <div class="left-column">
            <div class="card">
              <div class="card-header">
                <h3>Расписание на эту неделю</h3>
                <router-link to="/athlete/schedule" class="link">Показать всё →</router-link>
              </div>
              <StateBlock v-if="weekLoading" kind="loading" />
              <StateBlock v-else-if="weekError" kind="error" :message="errorText(weekError)" />
              <div v-else class="week-mini">
                <div
                  v-for="day in week"
                  :key="day.key"
                  class="day-block"
                  :class="{ active: day.active, clickable: day.trainingId }"
                  :title="day.hint"
                  @click="openTraining(day.trainingId)"
                >
                  <div class="day-label">
                    <span class="day-name">{{ day.label }}</span>
                    <span class="day-num">{{ day.num }}</span>
                  </div>
                  <div class="day-event">
                    <span class="event-time">{{ day.time }}</span>
                    <span class="event-name">{{ day.event }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Правая колонка: События и Сборы -->
          <aside class="right-column">
            <div class="card">
              <h3>События и Сборы</h3>
              <StateBlock v-if="dashboardLoading" kind="loading" />
              <StateBlock v-else-if="dashboardError" kind="error" :message="errorText(dashboardError)" />
              <StateBlock v-else-if="!events.length" kind="empty" message="Ближайших мероприятий и сборов нет" />
              <div v-else class="events-list">
                <router-link v-for="event in events" :key="event.id" to="/athlete/events" class="event-card">
                  <div class="event-meta">
                    <span class="event-tag">{{ event.tag }}</span>
                    <span class="event-date">{{ event.date }}</span>
                  </div>
                  <div class="event-title">{{ event.title }}</div>
                  <div v-if="event.location" class="event-location">{{ event.location }}</div>
                </router-link>
              </div>
            </div>
          </aside>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import MetricCard from '../../components/layout/MetricCard.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { dashboardApi } from '../../api/dashboard'
import { trainingsApi } from '../../api/trainings'
import { eventsApi } from '../../api/events'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import {
  currentUser, myAthletes, selectedAthleteId, selectedAthlete, loadMyAthletes, getOrganizationId
} from '../../utils/session'
import {
  formatDate, formatTime, formatWeekday, toIsoDate, toIsoDateTime, periodRange, addDays, parseDate, errorText
} from '../../utils/format'
import { label } from '../../utils/labels'

const NO_CARD = 'Карточка спортсмена ещё не создана тренером'
// Горизонт «ближайших» тренировок и мероприятий на обзоре (GET /dashboard: from/to — даты)
const HORIZON_DAYS = 90

const router = useRouter()

// ─────────── Своя карточка ───────────
const athletesReady = ref(false)
const athletesError = ref(null)

const pageState = computed(() => {
  if (!athletesReady.value) return { kind: 'loading', message: '' }
  if (athletesError.value) return { kind: 'error', message: errorText(athletesError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CARD }
  return null
})

// ─────────── Данные обзора ───────────
const dashboard = ref(null)
const dashboardLoading = ref(false)
const dashboardError = ref(null)

const weekTrainings = ref([])
const weekLoading = ref(false)
const weekError = ref(null)

const eventDetails = ref(new Map()) // id → Event (место проведения; в Dashboard его нет)
const groups = ref([])
const venues = ref([])

const weekRange = periodRange('week', new Date())

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  dashboard.value = null
  weekTrainings.value = []
  eventDetails.value = new Map()
  groups.value = []
  dashboardError.value = null
  weekError.value = null
  if (!athleteId) {
    dashboardLoading.value = false
    weekLoading.value = false
    return
  }
  dashboardLoading.value = true
  weekLoading.value = true
  try {
    const org = getOrganizationId()
    const today = new Date()
    const horizon = addDays(today, HORIZON_DAYS)
    const [dash, week, eventList, groupList] = await Promise.allSettled([
      // GET /dashboard?view=ATHLETE: ближайшие тренировки и мероприятия спортсмена
      dashboardApi.get(org, 'ATHLETE', { athleteId, from: toIsoDate(today), to: toIsoDate(horizon) }),
      // GET /trainings: [from, to) — занятия групп спортсмена на этой неделе
      fetchAll(p => trainingsApi.list(org, p), {
        athleteId, from: toIsoDateTime(weekRange.from), to: toIsoDateTime(weekRange.to)
      }),
      // GET /events за тот же период — место проведения для карточек мероприятий
      fetchAll(p => eventsApi.list(org, p), { athleteId, from: toIsoDate(today), to: toIsoDate(horizon) }),
      // Текущие группы спортсмена — для подписей
      fetchAll(p => groupsApi.list(org, p), { athleteId, status: 'ACTIVE' })
    ])
    if (seq !== loadSeq) return
    if (dash.status === 'fulfilled') dashboard.value = dash.value
    else dashboardError.value = dash.reason
    if (week.status === 'fulfilled') weekTrainings.value = week.value
    else weekError.value = week.reason
    if (eventList.status === 'fulfilled') eventDetails.value = new Map(eventList.value.map(e => [e.id, e]))
    if (groupList.status === 'fulfilled') groups.value = groupList.value
  } catch (e) {
    if (seq !== loadSeq) return
    dashboardError.value = e
    weekError.value = e
  } finally {
    if (seq === loadSeq) {
      dashboardLoading.value = false
      weekLoading.value = false
    }
  }
}

// Площадки — общий справочник организации; без них подписи обходятся названием группы
async function loadVenues() {
  try {
    venues.value = await fetchAll(p => dictionariesApi.list(getOrganizationId(), 'venues', p))
  } catch (_) {
    venues.value = []
  }
}

onMounted(async () => {
  loadVenues()
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (e) {
    athletesError.value = e
  } finally {
    athletesReady.value = true
  }
  if (!athletesError.value) load()
})

watch(selectedAthleteId, () => {
  if (athletesReady.value && !athletesError.value) load()
})

// ─────────── Шапка ───────────
const greeting = computed(() => {
  const name = selectedAthlete.value?.firstName?.trim()
    || currentUser.value?.fullName?.trim().split(/\s+/)[0]
  return name ? `Добро пожаловать, ${name}!` : 'Добро пожаловать!'
})

const subtitle = computed(() => {
  const today = new Date()
  const weekday = today.toLocaleDateString('ru-RU', { weekday: 'long' })
  const dateText = `${capitalize(weekday)}, ${formatDate(today, { withYear: false })}`
  if (pageState.value || weekLoading.value || weekError.value) return dateText
  const key = toIsoDate(today)
  const n = weekTrainings.value.filter(t => t.status !== 'CANCELLED' && toIsoDate(new Date(t.startsAt)) === key).length
  return n
    ? `${dateText} · У вас ${n} ${plural(n, 'тренировка', 'тренировки', 'тренировок')} сегодня`
    : `${dateText} · Сегодня тренировок нет`
})

const groupById = computed(() => new Map(groups.value.map(g => [g.id, g])))
const venueById = computed(() => new Map(venues.value.map(v => [v.id, v])))

const groupChip = computed(() => {
  const names = groups.value.map(g => g.name).filter(Boolean)
  if (!names.length) return ''
  if (names.length === 1) return `Группа: ${names[0]}`
  return names.length === 2 ? `Группы: ${names.join(', ')}` : `Группы: ${names[0]} и ещё ${names.length - 1}`
})

// ─────────── Метрики ───────────
const nextTraining = computed(() => dashboard.value?.nextTrainings?.[0] || null)

const nextTrainingValue = computed(() => {
  if (dashboardLoading.value) return '…'
  if (dashboardError.value || !nextTraining.value) return '—'
  return whenShort(nextTraining.value.startsAt)
})

const nextTrainingNote = computed(() => {
  if (dashboardLoading.value) return 'Загрузка…'
  if (dashboardError.value) return 'Не удалось загрузить'
  const t = nextTraining.value
  if (!t) return 'Ближайших тренировок нет'
  // В DashboardTraining нет площадки: берём её из недельного расписания, иначе — группу
  const full = weekTrainings.value.find(w => w.id === t.id)
  const place = venueById.value.get(full?.venueId)?.name || groupById.value.get(t.groupId)?.name
  return [t.title, place].filter(Boolean).join(' · ')
})

const nextEvent = computed(() => dashboard.value?.nextEvents?.[0] || null)

const nextEventValue = computed(() => {
  if (dashboardLoading.value) return '…'
  if (dashboardError.value || !nextEvent.value) return '—'
  const e = nextEvent.value
  return e.startsAt
    ? formatDate(e.startsAt, { withYear: false })
    : `до ${formatDate(e.collectionDueOn, { withYear: false })}`
})

const nextEventNote = computed(() => {
  if (dashboardLoading.value) return 'Загрузка…'
  if (dashboardError.value) return 'Не удалось загрузить'
  const e = nextEvent.value
  return e ? `${label('eventType', e.type)} · ${e.title}` : 'Ближайших мероприятий нет'
})

// ─────────── Неделя ───────────
const week = computed(() => {
  const todayKey = toIsoDate(new Date())
  return Array.from({ length: 7 }, (_, i) => {
    const d = addDays(weekRange.from, i)
    const key = toIsoDate(d)
    const list = weekTrainings.value
      .filter(t => toIsoDate(new Date(t.startsAt)) === key)
      .sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt))
    const active = list.filter(t => t.status !== 'CANCELLED')
    const main = active[0] || list[0] || null
    let time = '--:--'
    let event = 'Выходной'
    if (main) {
      time = formatTime(main.startsAt)
      event = main.status === 'CANCELLED' ? `Отменена: ${main.title}` : main.title
      const more = (active.length || list.length) - 1
      if (more > 0) event += ` +${more}`
    }
    return {
      key,
      label: formatWeekday(d),
      num: d.getDate(),
      time,
      event,
      active: key === todayKey,
      trainingId: main?.id || null,
      hint: list.map(t => `${formatTime(t.startsAt)} ${t.title}${t.status === 'CANCELLED' ? ' (отменена)' : ''}`).join('\n')
    }
  })
})

function openTraining(id) {
  if (id) router.push(`/athlete/schedule/${id}`)
}

// ─────────── Мероприятия ───────────
const events = computed(() => (dashboard.value?.nextEvents || []).map(e => {
  const full = eventDetails.value.get(e.id)
  let location = ''
  if (e.type === 'FUNDRAISER') location = e.collectionDueOn ? `Срок сбора: ${formatDate(e.collectionDueOn)}` : ''
  else if (full?.location) location = `Локация: ${full.location}`
  return {
    id: e.id,
    tag: label('eventType', e.type),
    date: eventDate(e, full),
    title: e.title,
    location
  }
}))

function eventDate(e, full) {
  if (!e.startsAt) return e.collectionDueOn ? `до ${formatDate(e.collectionDueOn, { withYear: false })}` : '—'
  const start = new Date(e.startsAt)
  const end = full?.endsAt ? new Date(full.endsAt) : null
  if (end && toIsoDate(end) !== toIsoDate(start)) {
    return `${formatDate(start, { withYear: false })} – ${formatDate(end, { withYear: false })}`
  }
  return `${formatDate(start, { withYear: false })}, ${formatTime(start)}`
}

// ─────────── Помощники ───────────
// «17:00» сегодня, «Завтра, 17:00», иначе «5 октября, 17:00»
function whenShort(value) {
  const d = parseDate(value)
  const time = formatTime(d)
  const now = new Date()
  if (toIsoDate(d) === toIsoDate(now)) return time
  if (toIsoDate(d) === toIsoDate(addDays(now, 1))) return `Завтра, ${time}`
  return `${formatDate(d, { withYear: false })}, ${time}`
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

function plural(n, one, few, many) {
  const mod10 = n % 10
  const mod100 = n % 100
  if (mod10 === 1 && mod100 !== 11) return one
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few
  return many
}

function capitalize(text) {
  return text ? text.charAt(0).toUpperCase() + text.slice(1) : ''
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
.online-dot {
  width: 6px; height: 6px;
  background: #B7F34B; border-radius: 50%;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}
@media (max-width: 1024px) { .content-grid { grid-template-columns: 1fr; } }

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card h3 { font-size: 20px; font-weight: 700; color: #152421; }
.link { color: #35678E; font-size: 13px; font-weight: 600; text-decoration: none; }

.week-mini {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}
.day-block {
  padding: 8px;
  background: #F4F7F8;
  border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; flex-direction: column; gap: 8px;
}
.day-block.clickable { cursor: pointer; }
.day-block.active {
  background: #102522;
  border-color: #B7F34B;
}
.day-label { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.day-name { font-size: 12px; font-weight: 600; color: #6D7D79; }
.day-num { font-size: 16px; font-weight: 700; color: #152421; }
.day-block.active .day-name { color: #B7F34B; }
.day-block.active .day-num { color: white; }
.day-event {
  min-height: 42px; padding: 4px;
  background: white; border-radius: 8px;
  display: flex; flex-direction: column; gap: 2px;
}
.day-block.active .day-event { background: #19332F; }
.event-time { font-size: 10px; font-weight: 700; color: #152421; }
.day-block.active .event-time { color: #B7F34B; }
.event-name { font-size: 9px; font-weight: 500; color: #6D7D79; }

.events-list { display: flex; flex-direction: column; gap: 12px; }
.event-card {
  padding: 16px; background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; gap: 12px;
  text-decoration: none;
}
.event-meta { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.event-tag { font-size: 12px; font-weight: 700; color: #2D5B24; }
.event-date { font-size: 12px; font-weight: 600; color: #6D7D79; text-align: right; }
.event-title { font-size: 15px; font-weight: 700; color: #152421; }
.event-location { font-size: 13px; color: #6D7D79; }
</style>
