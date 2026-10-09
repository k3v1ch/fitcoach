<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        :title="greeting"
        :subtitle="subtitle"
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
            <span v-if="unreadCount > 0" class="dot"></span>
          </router-link>
        </template>
      </PageHeader>

      <div v-if="pageState" class="card">
        <StateBlock :kind="pageState.kind" :message="pageState.message" />
      </div>

      <template v-else>
        <!-- Метрики -->
        <section class="metrics-grid">
          <MetricCard
            label="Ближайшее событие"
            :value="nextEventValue"
            :note="nextEventNote"
            icon="clock"
            color="#E9F7D5"
            icon-color="#2E8B57"
          />
          <MetricCard
            label="Посещаемость"
            :value="attendanceValue"
            :note="attendanceNote"
            icon="check"
            color="#DDECFB"
            icon-color="#35678E"
          />
          <MetricCard
            label="Объявления"
            :value="announcementsValue"
            :note="announcementsNote"
            icon="bell"
            color="#FFF1D6"
            icon-color="#F2B705"
          />
          <MetricCard
            label="Задолженность"
            :value="debtValue"
            :note="debtNote"
            icon="credit-card"
            :color="debtTone.bg"
            :icon-color="debtTone.fg"
          />
        </section>

        <!-- Основной контент -->
        <section class="content-grid">
          <div class="left-column">
            <!-- Расписание на ближайшие дни -->
            <div class="card">
              <div class="card-header">
                <div>
                  <h3>Ближайшее расписание</h3>
                  <p>{{ weekRangeLabel }}</p>
                </div>
                <router-link to="/parent/schedule" class="link">Смотреть всё расписание →</router-link>
              </div>
              <div class="week-row">
                <div v-for="day in week" :key="day.key" class="day-block">
                  <div class="day-label" :class="{ active: day.active }">
                    <span class="day-name">{{ day.label }}</span>
                    <span class="day-num">{{ day.num }}</span>
                  </div>
                  <div class="event-mini" :class="day.color" :title="day.hint || null">
                    <span class="event-name">{{ day.event }}</span>
                    <span class="event-time">{{ day.time }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Быстрые действия -->
            <div class="card">
              <h3>Быстрые действия</h3>
              <div class="actions-row">
                <button class="action-btn green" @click="router.push('/parent/payments')">
                  <BaseIcon name="credit-card" :size="16" color="#102522" />
                  Оплатить абонемент
                </button>
                <button class="action-btn dark" @click="router.push('/parent/contact')">
                  <BaseIcon name="message" :size="16" color="#B7F34B" />
                  Связаться с тренером
                </button>
              </div>
            </div>
          </div>

          <!-- Объявления -->
          <aside class="card announcements-card">
            <div class="card-header">
              <h3>Объявления секции</h3>
              <span v-if="pendingCount > 0" class="badge-red">
                {{ pendingCount }} {{ plural(pendingCount, 'ждёт ответа', 'ждут ответа', 'ждут ответа') }}
              </span>
            </div>
            <StateBlock v-if="annLoading" kind="loading" />
            <StateBlock v-else-if="annError" kind="error" :message="errorText(annError)" />
            <StateBlock v-else-if="!announcementItems.length" kind="empty" message="Объявлений пока нет" />
            <div v-else class="announcements-list">
              <div
                v-for="a in announcementItems"
                :key="a.id"
                class="announcement-item"
                :class="a.class"
                @click="router.push('/parent/announcements')"
              >
                <div class="announcement-meta">
                  <span>{{ a.date }}</span>
                  <span class="author">{{ a.author }}</span>
                </div>
                <div class="announcement-title">{{ a.title }}</div>
                <div class="announcement-desc">{{ a.description }}</div>
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
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import MetricCard from '../../components/layout/MetricCard.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { dashboardApi } from '../../api/dashboard'
import { trainingsApi } from '../../api/trainings'
import { attendanceApi } from '../../api/attendance'
import { announcementsApi } from '../../api/announcements'
import {
  currentUser, myAthletes, selectedAthleteId, selectedAthlete, loadMyAthletes, selectAthlete, getOrganizationId
} from '../../utils/session'
import {
  parseDate, formatDate, formatTime, formatWeekday, formatMoney, toIsoDate, toIsoDateTime, addDays,
  fullName, initials, errorText
} from '../../utils/format'
import { label } from '../../utils/labels'

const router = useRouter()

const NO_CHILD = 'Ребёнок ещё не привязан — обратитесь к тренеру'
const WEEK_DAYS = 5         // «Ближайшее расписание»: сегодня и ещё 4 дня
const EVENT_HORIZON = 30    // ближайшее событие — в пределах 30 дней
const ATTENDANCE_DAYS = 30  // посещаемость — за последние 30 дней

// ─────────── помощники ───────────

const startOfDay = d => new Date(d.getFullYear(), d.getMonth(), d.getDate())

function plural(n, one, few, many) {
  const m10 = n % 10
  const m100 = n % 100
  if (m10 === 1 && m100 !== 11) return one
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return few
  return many
}

// «Сегодня» / «Завтра» / «Вчера» / «5 октября»
function dayWord(value) {
  const d = parseDate(value)
  const diff = Math.round((startOfDay(d) - startOfDay(new Date())) / 86400000)
  if (diff === 0) return 'Сегодня'
  if (diff === 1) return 'Завтра'
  if (diff === -1) return 'Вчера'
  return formatDate(d, { withYear: d.getFullYear() !== new Date().getFullYear() })
}

function relativeDateTime(value) {
  const d = parseDate(value)
  if (!d || isNaN(d)) return '—'
  const word = dayWord(d)
  return ['Сегодня', 'Вчера', 'Завтра'].includes(word) ? `${word}, ${formatTime(d)}` : word
}

const shorten = (text, max) => (text && text.length > max ? `${text.slice(0, max - 1).trimEnd()}…` : text || '')
const capitalize = s => (s ? s.charAt(0).toUpperCase() + s.slice(1) : s)

// ─────────── выбранный ребёнок ───────────

const childrenReady = ref(false)
const childrenError = ref(null)

// ─────────── данные ребёнка: dashboard, тренировки ближайших дней, посещаемость ───────────

const anchor = ref(startOfDay(new Date()))
const dashboard = ref(null)
const trainings = ref([])
const attendance = ref(null) // AttendanceSummary
const loading = ref(false)
const error = ref(null)

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  dashboard.value = null
  trainings.value = []
  attendance.value = null
  error.value = null
  if (!athleteId) {
    loading.value = false
    return
  }
  loading.value = true
  anchor.value = startOfDay(new Date())
  try {
    const org = getOrganizationId()
    const start = anchor.value
    const [dash, trainingPage, journal] = await Promise.all([
      // GET /dashboard: from/to — даты; ближайшие тренировки и мероприятия ребёнка, долг по начислениям
      dashboardApi.get(org, 'PARENT', {
        athleteId,
        from: toIsoDate(start),
        to: toIsoDate(addDays(start, EVENT_HORIZON))
      }),
      // GET /trainings: [from, to) в datetime — занятия ребёнка на ближайшие дни, включая прошедшие сегодня
      trainingsApi.list(org, {
        athleteId,
        from: toIsoDateTime(start),
        to: toIsoDateTime(addDays(start, WEEK_DAYS)),
        size: 100
      }),
      // GET /attendance: summary по прошедшим занятиям периода (отменённые не учитываются)
      attendanceApi.list(org, {
        athleteId,
        from: toIsoDate(addDays(start, -ATTENDANCE_DAYS)),
        to: toIsoDate(start),
        size: 1
      })
    ])
    if (seq !== loadSeq) return
    dashboard.value = dash
    trainings.value = trainingPage?.items || []
    attendance.value = journal?.summary || null
  } catch (e) {
    if (seq === loadSeq) error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// ─────────── объявления: адресованы родителю, от выбранного ребёнка не зависят ───────────

const announcements = ref([])
const unreadCount = ref(0)
const pendingCount = ref(0)
const annLoading = ref(true)
const annError = ref(null)

async function loadAnnouncements() {
  annLoading.value = true
  annError.value = null
  try {
    const org = getOrganizationId()
    const [latest, unread, needResponse] = await Promise.all([
      announcementsApi.list(org, { status: 'PUBLISHED', size: 3 }),
      announcementsApi.list(org, { status: 'PUBLISHED', unread: true, size: 1 }),
      announcementsApi.list(org, { status: 'PUBLISHED', requiresResponse: true, size: 100 })
    ])
    const now = Date.now()
    announcements.value = latest?.items || []
    unreadCount.value = unread?.totalElements || 0
    // Ждут ответа: согласие запрошено, ответа нет и срок ещё не прошёл
    pendingCount.value = (needResponse?.items || []).filter(a =>
      !a.myResponse && (!a.responseDeadline || new Date(a.responseDeadline).getTime() > now)).length
  } catch (e) {
    annError.value = e
  } finally {
    annLoading.value = false
  }
}

onMounted(async () => {
  loadAnnouncements()
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (e) {
    childrenError.value = e
  } finally {
    childrenReady.value = true
  }
  if (!childrenError.value) load()
})

watch(selectedAthleteId, () => {
  if (childrenReady.value && !childrenError.value) load()
})

// ─────────── состояние страницы ───────────

const pageState = computed(() => {
  if (!childrenReady.value) return { kind: 'loading', message: '' }
  if (childrenError.value) return { kind: 'error', message: errorText(childrenError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CHILD }
  if (loading.value) return { kind: 'loading', message: '' }
  if (error.value) return { kind: 'error', message: errorText(error.value) }
  return null
})

// ─────────── заголовок ───────────

const greeting = computed(() => {
  const name = currentUser.value?.fullName?.trim()
  return name ? `Добро пожаловать, ${name}!` : 'Добро пожаловать!'
})

const todayCount = computed(() => {
  const key = toIsoDate(anchor.value)
  return trainings.value.filter(t => t.status !== 'CANCELLED' && toIsoDate(new Date(t.startsAt)) === key).length
})

const subtitle = computed(() => {
  const date = capitalize(new Date().toLocaleDateString('ru-RU', { weekday: 'long', day: 'numeric', month: 'long' }))
  const child = selectedAthlete.value
  if (!child || pageState.value) return date
  const name = child.firstName || fullName(child)
  const n = todayCount.value
  return n
    ? `${date} · ${name}: сегодня ${n} ${plural(n, 'тренировка', 'тренировки', 'тренировок')}`
    : `${date} · ${name}: сегодня тренировок нет`
})

// ─────────── метрики ───────────

// Ближайшее: тренировка (startsAt) или мероприятие (startsAt; у денежного сбора — срок collectionDueOn)
const nextItem = computed(() => {
  const d = dashboard.value
  if (!d) return null
  const now = new Date()
  const items = [
    ...(d.nextTrainings || []).map(t => ({
      title: t.title, kind: 'Тренировка', at: new Date(t.startsAt), timed: true
    })),
    ...(d.nextEvents || []).map(e => (e.startsAt
      ? { title: e.title, kind: label('eventType', e.type), at: new Date(e.startsAt), timed: true }
      : { title: e.title, kind: label('eventType', e.type), at: parseDate(e.collectionDueOn), timed: false }))
  ].filter(i => i.at && !isNaN(i.at) && (!i.timed || i.at >= now))
  items.sort((a, b) => a.at - b.at)
  return items[0] || null
})

const nextEventValue = computed(() => {
  const item = nextItem.value
  if (!item) return 'Нет событий'
  return item.timed
    ? `${dayWord(item.at)} в ${formatTime(item.at)}`
    : `До ${formatDate(item.at, { withYear: false })}`
})

const nextEventNote = computed(() => {
  const item = nextItem.value
  return item ? `${item.title} · ${item.kind}` : `В ближайшие ${EVENT_HORIZON} дней ничего не запланировано`
})

// Процент — по отмеченным занятиям: PRESENT / (PRESENT + SICK + ABSENT)
const attendanceValue = computed(() => {
  const p = attendance.value?.attendancePercent
  return p === null || p === undefined ? '—' : `${Math.round(Number(p))}%`
})

const attendanceNote = computed(() => {
  const s = attendance.value
  if (!s) return ''
  const marked = s.present + s.sick + s.absent
  return marked
    ? `${s.present} из ${marked} ${plural(marked, 'занятия', 'занятий', 'занятий')} посещено за ${ATTENDANCE_DAYS} дней`
    : `За ${ATTENDANCE_DAYS} дней отметок посещения нет`
})

const announcementsValue = computed(() => {
  if (annLoading.value) return '…'
  if (annError.value) return '—'
  const n = unreadCount.value
  return n ? `${n} ${plural(n, 'новое', 'новых', 'новых')}` : 'Нет новых'
})

const announcementsNote = computed(() => {
  if (annLoading.value) return ''
  if (annError.value) return 'Не удалось загрузить объявления'
  const n = pendingCount.value
  return n ? `${n} ${plural(n, 'требует', 'требуют', 'требуют')} вашего ответа` : 'Ответов не требуется'
})

// finance = null, если у пользователя нет права на начисления
const finance = computed(() => dashboard.value?.finance || null)

const debtValue = computed(() => (finance.value ? formatMoney(finance.value.outstandingAmount ?? 0) : '—'))

const debtNote = computed(() => {
  const f = finance.value
  if (!f) return 'Начисления недоступны'
  if (Number(f.overdueAmount || 0) > 0) return `Просрочено: ${formatMoney(f.overdueAmount)}`
  return Number(f.outstandingAmount || 0) > 0 ? 'Просроченных начислений нет' : 'Все начисления оплачены'
})

const debtTone = computed(() => {
  const f = finance.value
  if (f && Number(f.overdueAmount || 0) > 0) return { bg: '#FCE2E5', fg: '#D64545' }
  if (f && Number(f.outstandingAmount || 0) > 0) return { bg: '#FFF1D6', fg: '#F2B705' }
  return { bg: '#E9F7D5', fg: '#2E8B57' }
})

// ─────────── ближайшие дни ───────────

const weekRangeLabel = computed(() => {
  const from = anchor.value
  const to = addDays(from, WEEK_DAYS - 1)
  return from.getMonth() === to.getMonth()
    ? `${from.getDate()} – ${formatDate(to, { withYear: false })}`
    : `${formatDate(from, { withYear: false })} – ${formatDate(to, { withYear: false })}`
})

const week = computed(() => {
  const events = (dashboard.value?.nextEvents || []).filter(e => e.startsAt)
  return Array.from({ length: WEEK_DAYS }, (_, i) => {
    const day = addDays(anchor.value, i)
    const key = toIsoDate(day)
    const items = [
      ...trainings.value
        .filter(t => t.status !== 'CANCELLED' && toIsoDate(new Date(t.startsAt)) === key)
        .map(t => ({ title: t.title, at: new Date(t.startsAt), event: false })),
      ...events
        .filter(e => toIsoDate(new Date(e.startsAt)) === key)
        .map(e => ({ title: e.title, at: new Date(e.startsAt), event: true }))
    ].sort((a, b) => a.at - b.at)
    const first = items[0]
    return {
      key,
      label: formatWeekday(day),
      num: day.getDate(),
      active: i === 0,
      event: first ? first.title : 'Нет занятий',
      time: first ? formatTime(first.at) + (items.length > 1 ? ` · ещё ${items.length - 1}` : '') : '—',
      color: !first ? 'gray' : first.event ? 'yellow' : i === 0 ? 'lime' : 'green',
      hint: items.map(x => `${formatTime(x.at)} ${x.title}`).join('\n')
    }
  })
})

// ─────────── объявления ───────────

const announcementItems = computed(() => announcements.value.map(a => ({
  id: a.id,
  date: relativeDateTime(a.publishedAt || a.createdAt),
  author: a.category || '',
  title: a.title,
  description: shorten(a.text, 140),
  class: a.requiresResponse && !a.myResponse ? 'red' : 'gray'
})))
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

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}
@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }

.left-column { display: flex; flex-direction: column; gap: 24px; }
.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 16px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }
.card-header p { font-size: 12px; color: #6D7D79; }

.link { color: #2E8B57; font-size: 13px; font-weight: 600; text-decoration: none; }

.week-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.day-block { display: flex; flex-direction: column; gap: 8px; }
.day-label {
  padding: 8px; background: #F4F7F8; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
}
.day-label.active { background: #102522; }
.day-label.active .day-name { color: white; }
.day-label.active .day-num { color: #B7F34B; }
.day-name { font-size: 11px; font-weight: 600; color: #6D7D79; }
.day-num { font-size: 14px; font-weight: 700; color: #152421; }

.event-mini {
  padding: 8px; border-radius: 8px;
  display: flex; flex-direction: column; gap: 4px;
}
.event-mini.green { background: #E9F7D5; }
.event-mini.gray { background: #F4F7F8; }
.event-mini.lime { background: #B7F34B; }
.event-mini.yellow { background: #FFF1D6; }
.event-name { font-size: 11px; font-weight: 700; color: #152421; }
.event-time { font-size: 10px; color: #6D7D79; }

.actions-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.action-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 16px; border: none; border-radius: 12px;
  font-size: 13px; font-weight: 700; cursor: pointer;
}
.action-btn.green { background: #B7F34B; color: #102522; }
.action-btn.dark { background: #102522; color: white; }

.announcements-card { align-self: start; }
.badge-red {
  padding: 2px 8px; background: #FCE2E5; color: #D64545;
  border-radius: 99px; font-size: 11px; font-weight: 700;
}
.announcements-list { display: flex; flex-direction: column; gap: 16px; }
.announcement-item {
  padding: 12px; border-radius: 12px;
  display: flex; flex-direction: column; gap: 10px;
  cursor: pointer;
}
.announcement-item.red { background: #FCE2E5; }
.announcement-item.gray { background: #F4F7F8; }
.announcement-meta { display: flex; justify-content: space-between; font-size: 11px; color: #6D7D79; }
.announcement-meta .author { color: #152421; font-weight: 600; }
.announcement-title { font-size: 13px; font-weight: 700; color: #152421; }
.announcement-desc { font-size: 12px; color: #6D7D79; line-height: 1.4; }
</style>
