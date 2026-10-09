<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        :title="greeting"
        :subtitle="todayText"
        :show-search="false"
      >
        <template #actions>
          <BaseButton v-if="canSchedule" @click="goCreate('/trainer/trainings')">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать тренировку
          </BaseButton>
        </template>
      </PageHeader>

      <section class="stats-grid">
        <StatCard label="Активных спортсменов" :value="stats.athletes" :subtext="stats.athletesSub" />
        <StatCard label="Тренировок сегодня" :value="stats.trainings" :subtext="stats.trainingsSub" />
        <StatCard label="Посещаемость" :value="stats.attendance" :subtext="stats.attendanceSub" />
        <StatCard label="Ближайший сбор" :value="stats.camp" :subtext="stats.campSub" />
        <StatCard label="Предстоящих событий" :value="stats.events" :subtext="stats.eventsSub" />
      </section>

      <section class="content-grid">
        <div class="card schedule-card">
          <h2>Расписание на сегодня</h2>
          <StateBlock v-if="todayLoading" kind="loading" />
          <StateBlock v-else-if="todayError" kind="error" :message="todayError" />
          <div v-else-if="todayTrainings.length" class="schedule-list">
            <div
              v-for="item in todayTrainings"
              :key="item.id"
              class="schedule-item"
              role="link"
              tabindex="0"
              @click="openTraining(item.id)"
              @keydown.enter="openTraining(item.id)"
            >
              <div class="schedule-time">{{ formatTime(item.startsAt) }}</div>
              <div class="schedule-info">
                <div class="schedule-title">{{ item.title }}</div>
                <div class="schedule-location">{{ scheduleMeta(item) }}</div>
              </div>
            </div>
          </div>
          <template v-else>
            <StateBlock kind="empty" message="Сегодня тренировок нет" />
            <template v-if="nextTrainings.length">
              <h3 class="schedule-subtitle">Ближайшие тренировки</h3>
              <div class="schedule-list">
                <div
                  v-for="item in nextTrainings"
                  :key="item.id"
                  class="schedule-item"
                  role="link"
                  tabindex="0"
                  @click="openTraining(item.id)"
                  @keydown.enter="openTraining(item.id)"
                >
                  <div class="schedule-time">{{ formatTime(item.startsAt) }}</div>
                  <div class="schedule-info">
                    <div class="schedule-title">{{ item.title }}</div>
                    <div class="schedule-location">{{ formatWeekday(item.startsAt) }}, {{ formatDate(item.startsAt, { withYear: false }) }} · {{ groupName(item.groupId) }}</div>
                  </div>
                </div>
              </div>
            </template>
          </template>
        </div>

        <div class="right-column">
          <div class="card actions-card">
            <h2>Последние действия</h2>
            <StateBlock v-if="dashboardLoading" kind="loading" />
            <StateBlock v-else-if="dashboardError" kind="error" :message="dashboardError" />
            <StateBlock v-else-if="!activities.length" kind="empty" message="Действий пока нет" />
            <ul v-else class="activity-list">
              <li v-for="act in activities" :key="act.id" class="activity-item">
                <span class="dot"></span>
                <span class="activity-text">{{ act.actorName }}: {{ act.title }}</span>
                <span class="activity-time">{{ formatDateTime(act.createdAt) }}</span>
              </li>
            </ul>
          </div>

          <div class="card quick-actions">
            <h2>Быстрые действия</h2>
            <button v-if="canSchedule" class="quick-btn" @click="goCreate('/trainer/trainings')">
              ⚡ Создать тренировку
            </button>
            <button v-if="canAthletes" class="quick-btn" @click="goCreate('/trainer/athletes')">
              ⚡ Добавить спортсмена
            </button>
            <button v-if="canCharges" class="quick-btn" @click="goCreate('/trainer/finance/charges')">
              ⚡ Начислить оплату
            </button>
            <button v-if="canEvents" class="quick-btn" @click="goCreate('/trainer/events')">
              ⚡ Создать событие
            </button>
            <p v-if="!hasQuickActions" class="quick-empty">Для вашей роли действия на запись недоступны.</p>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import StatCard from '../../components/layout/StatCard.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { dashboardApi } from '../../api/dashboard'
import { trainingsApi } from '../../api/trainings'
import { attendanceApi } from '../../api/attendance'
import { eventsApi } from '../../api/events'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { getOrganizationId, hasPermission, hasRole, currentUser } from '../../utils/session'
import {
  errorText, formatDate, formatTime, formatDateTime, formatWeekday, toIsoDate, toIsoDateTime, periodRange, addDays, parseDate
} from '../../utils/format'
import { label } from '../../utils/labels'

const router = useRouter()
// Сводка за 30 дней вперёд: счётчики событий, ближайшие тренировки и мероприятия
const HORIZON_DAYS = 30

const today = new Date()
const isTrainer = computed(() => hasRole('TRAINER'))
const view = computed(() => (isTrainer.value || !hasRole('AGENCY') ? 'TRAINER' : 'AGENCY'))
const canWrite = (code) => isTrainer.value && hasPermission(code)
const canSchedule = computed(() => canWrite('schedule.write'))
const canAthletes = computed(() => canWrite('athletes.write'))
const canCharges = computed(() => canWrite('charges.write'))
const canEvents = computed(() => canWrite('events.write'))
const hasQuickActions = computed(() => canSchedule.value || canAthletes.value || canCharges.value || canEvents.value)

// ─────────── Заголовок ───────────
const greeting = computed(() => {
  const name = currentUser.value?.fullName?.trim()
  return name ? `Добро пожаловать, ${name}!` : 'Добро пожаловать!'
})
const todayText = computed(() => {
  const weekday = today.toLocaleDateString('ru-RU', { weekday: 'long' })
  return `Сегодня ${capitalize(weekday)}, ${formatDate(today).replace(/\s*г\.$/, '')}`
})

// ─────────── Сводка организации (GET /dashboard) ───────────
const dashboard = ref(null)
const dashboardLoading = ref(true)
const dashboardError = ref('')

async function loadDashboard() {
  dashboardLoading.value = true
  dashboardError.value = ''
  try {
    dashboard.value = await dashboardApi.get(getOrganizationId(), view.value, {
      from: toIsoDate(today),
      to: toIsoDate(addDays(today, HORIZON_DAYS)),
      activityPage: 0,
      activitySize: 6
    })
  } catch (e) {
    dashboard.value = null
    dashboardError.value = errorText(e)
  } finally {
    dashboardLoading.value = false
  }
}

const activities = computed(() => dashboard.value?.recentActivities?.items || [])
const nextTrainings = computed(() => dashboard.value?.nextTrainings || [])

// ─────────── Расписание на сегодня (GET /trainings за местные сутки) ───────────
const todayTrainings = ref([])
const todayLoading = ref(true)
const todayError = ref('')

async function loadToday() {
  const { from, to } = periodRange('day', today)
  todayLoading.value = true
  todayError.value = ''
  try {
    const items = await fetchAll(params => trainingsApi.list(getOrganizationId(), params), {
      from: toIsoDateTime(from),
      to: toIsoDateTime(to)
    })
    // Тренировка, начавшаяся вчера и закончившаяся сегодня, в «расписание на сегодня» не попадает
    todayTrainings.value = items
      .filter(t => parseDate(t.startsAt) >= from)
      .sort((a, b) => parseDate(a.startsAt) - parseDate(b.startsAt))
  } catch (e) {
    todayTrainings.value = []
    todayError.value = errorText(e)
  } finally {
    todayLoading.value = false
  }
}

// Названия групп и площадок для строк расписания
const groups = ref([])
const venues = ref([])

async function loadReference() {
  const [groupList, venueList] = await Promise.allSettled([
    fetchAll(params => groupsApi.list(getOrganizationId(), params)),
    fetchAll(params => dictionariesApi.list(getOrganizationId(), 'venues', params))
  ])
  groups.value = groupList.status === 'fulfilled' ? groupList.value : []
  venues.value = venueList.status === 'fulfilled' ? venueList.value : []
}

const groupName = (id) => groups.value.find(g => g.id === id)?.name || 'Группа'
const venueName = (id) => venues.value.find(v => v.id === id)?.name || null

function scheduleMeta(training) {
  const parts = [venueName(training.venueId), groupName(training.groupId)]
  if (training.status !== 'PLANNED') parts.push(label('trainingStatus', training.status))
  return parts.filter(Boolean).join(' · ')
}

const openTraining = (id) => router.push(`/trainer/trainings/${id}/report`)

// ─────────── Посещаемость за 30 дней (сводка журнала GET /attendance) ───────────
const attendance = ref({ loading: true, percent: null, failed: false })

async function loadAttendance() {
  try {
    const page = await attendanceApi.list(getOrganizationId(), {
      from: toIsoDate(addDays(today, -(HORIZON_DAYS - 1))),
      to: toIsoDate(today),
      page: 0,
      size: 1
    })
    attendance.value = { loading: false, percent: page?.summary?.attendancePercent ?? null, failed: false }
  } catch (_) {
    attendance.value = { loading: false, percent: null, failed: true }
  }
}

// ─────────── Ближайший опубликованный сбор (CAMP) ───────────
const camp = ref({ loading: true, event: null, failed: false })

async function loadCamp() {
  try {
    const items = await fetchAll(params => eventsApi.list(getOrganizationId(), params), {
      type: 'CAMP',
      status: 'PUBLISHED',
      from: toIsoDate(today)
    })
    const now = new Date()
    const nearest = items
      .filter(e => e.startsAt && parseDate(e.endsAt || e.startsAt) > now)
      .sort((a, b) => parseDate(a.startsAt) - parseDate(b.startsAt))[0] || null
    camp.value = { loading: false, event: nearest, failed: false }
  } catch (_) {
    camp.value = { loading: false, event: null, failed: true }
  }
}

function campWhen(event) {
  const start = parseDate(event.startsAt)
  if (start <= new Date()) return 'идёт сейчас'
  const days = Math.round((dayStart(start) - dayStart(new Date())) / 86400000)
  if (days <= 0) return 'сегодня'
  if (days === 1) return 'завтра'
  return `через ${days} ${plural(days, 'день', 'дня', 'дней')}`
}

// ─────────── Карточки статистики ───────────
const stats = computed(() => {
  const d = dashboard.value
  const counters = d?.counters
  const wait = '…'
  const none = '—'

  // Только число: подпись карточки уже говорит «Активных спортсменов», а длинное слово переносится
  const athletes = dashboardLoading.value ? wait : counters ? String(Number(counters.athletes) || 0) : none
  const groupsCount = counters?.groups
  const athletesSub = counters ? `${groupsCount} ${plural(groupsCount, 'группа', 'группы', 'групп')} в организации` : 'в организации'

  const active = todayTrainings.value.filter(t => t.status !== 'CANCELLED')
  const ahead = active.filter(t => t.status === 'PLANNED' && parseDate(t.startsAt) > new Date()).length
  const trainings = todayLoading.value ? wait : todayError.value ? none : countText(active.length, 'занятие', 'занятия', 'занятий')
  const trainingsSub = ahead ? `ещё ${ahead} впереди` : 'в расписании'

  const a = attendance.value
  const attendanceValue = a.loading ? wait : a.percent === null ? none : `${Math.round(Number(a.percent))}%`
  const attendanceSub = a.failed ? 'нет данных' : a.percent === null && !a.loading ? 'нет отметок за 30 дней' : 'за последние 30 дней'

  const c = camp.value
  const campValue = c.loading ? wait : c.event ? campWhen(c.event) : none
  const campSub = c.failed ? 'нет данных' : c.event ? c.event.title : 'не запланирован'

  const nearestEvent = (d?.nextEvents || [])[0]
  const events = dashboardLoading.value ? wait : counters ? countText(counters.upcomingEvents, 'событие', 'события', 'событий') : none
  const eventsSub = nearestEvent ? `ближайшее: ${nearestEvent.title}` : `в ближайшие ${HORIZON_DAYS} дней`

  return {
    athletes, athletesSub,
    trainings, trainingsSub,
    attendance: attendanceValue, attendanceSub,
    camp: campValue, campSub,
    events, eventsSub
  }
})

// ─────────── Быстрые действия: формы создания на своих экранах ───────────
// TrainingsView и EventsView открывают форму по ?create=1
const goCreate = (path) => router.push({ path, query: { create: '1' } })

onMounted(() => {
  loadDashboard()
  loadToday()
  loadReference()
  loadAttendance()
  loadCamp()
})

// ─────────── Помощники ───────────
// Все страницы списка (size ≤ 100 по контракту), не больше maxPages запросов
async function fetchAll(request, params = {}, maxPages = 10) {
  const first = await request({ ...params, page: 0, size: 100 })
  const pages = Math.min(first.totalPages || 1, maxPages)
  const rest = await Promise.all(
    Array.from({ length: pages - 1 }, (_, i) => request({ ...params, page: i + 1, size: 100 }))
  )
  return rest.reduce((all, page) => all.concat(page.items || []), first.items || [])
}

function countText(n, one, few, many) {
  const value = Number(n) || 0
  return `${value} ${plural(value, one, few, many)}`
}

function plural(n, one, few, many) {
  const mod10 = n % 10
  const mod100 = n % 100
  if (mod10 === 1 && mod100 !== 11) return one
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few
  return many
}

function dayStart(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function capitalize(text) {
  return text ? text.charAt(0).toUpperCase() + text.slice(1) : ''
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace {
  flex: 1; padding: 28px 32px 32px;
  display: flex; flex-direction: column; gap: 24px;
  overflow-y: auto;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}
.content-grid {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 24px;
}
@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }
.card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.card h2 { font-size: 18px; font-weight: 700; color: #152421; }
.schedule-subtitle { font-size: 14px; font-weight: 700; color: #6D7D79; }
.schedule-list { display: flex; flex-direction: column; gap: 12px; }
.schedule-item {
  display: flex; align-items: center; gap: 16px;
  padding: 12px; background: #F4F7F8; border-radius: 12px;
  cursor: pointer; transition: background 0.2s;
}
.schedule-item:hover { background: #E9F0EE; }
.schedule-time { width: 60px; font-weight: 700; color: #152421; }
.schedule-title { font-weight: 600; color: #152421; font-size: 14px; }
.schedule-location { font-size: 12px; color: #6D7D79; }
.right-column { display: flex; flex-direction: column; gap: 24px; }
.activity-list { list-style: none; display: flex; flex-direction: column; gap: 12px; }
.activity-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #6D7D79;
}
.activity-text { flex: 1; min-width: 0; }
.activity-time { font-size: 11px; color: #98A6A2; white-space: nowrap; }
.dot {
  width: 8px; height: 8px;
  background: #B7F34B; border-radius: 50%;
  flex-shrink: 0;
}
.quick-btn {
  text-align: left;
  background: #F4F7F8;
  border: 1px solid #E3EAE8;
  border-radius: 10px;
  padding: 12px 16px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
  transition: background 0.2s;
}
.quick-btn:hover { background: #E9F0EE; }
.quick-empty { font-size: 13px; color: #6D7D79; }
</style>
