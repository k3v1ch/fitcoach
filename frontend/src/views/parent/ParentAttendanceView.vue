<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        title="Посещаемость занятий"
        subtitle="Отслеживайте визиты, отработки и пропущенные тренировки"
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

      <div v-if="pageState" class="card">
        <StateBlock :kind="pageState.kind" :message="pageState.message" />
      </div>

      <template v-else>
        <section class="metrics-grid">
          <MetricCard
            label="Общая статистика"
            :value="overallValue"
            :note="overallNote"
            icon="check"
            color="#E9F7D5"
            icon-color="#2E8B57"
          />
          <MetricCard
            label="Пропуски"
            :value="missedValue"
            :note="missedNote"
            icon="alert"
            color="#FCE2E5"
            icon-color="#D64545"
          />
          <!-- Отработок (компенсационных занятий) в API нет -->
          <MetricCard
            label="Доступно отработок"
            value="—"
            note="Раздел появится позже"
            icon="plus-circle"
            color="#FFF1D6"
            icon-color="#F2B705"
          />
        </section>

        <section class="details-grid">
          <!-- Календарь визитов -->
          <div class="card heatmap-card">
            <div class="heatmap-head">
              <h3>Календарь визитов: {{ monthLabel }}</h3>
              <div class="month-nav">
                <button class="nav-btn" aria-label="Предыдущий месяц" @click="shiftMonth(-1)">
                  <BaseIcon name="chevron-left" :size="14" color="#152421" />
                </button>
                <button class="nav-btn" aria-label="Следующий месяц" :disabled="isCurrentMonth" @click="shiftMonth(1)">
                  <BaseIcon name="chevron-right" :size="14" color="#152421" />
                </button>
              </div>
            </div>
            <StateBlock v-if="loading" kind="loading" />
            <StateBlock v-else-if="error" kind="error" :message="errorText(error)" />
            <template v-else>
              <div class="heatmap">
                <div class="heatmap-header">
                  <span v-for="d in ['Пн','Вт','Ср','Чт','Пт','Сб','Вс']" :key="d">{{ d }}</span>
                </div>
                <div v-for="(week, wi) in weeks" :key="wi" class="heatmap-row">
                  <div
                    v-for="day in week"
                    :key="day.key"
                    class="heatmap-day"
                    :class="day.status"
                    :title="day.title || null"
                  ></div>
                </div>
              </div>
              <div class="legend">
                <div class="legend-item"><span class="dot green"></span> {{ label('attendanceStatus', 'PRESENT') }}</div>
                <div class="legend-item"><span class="dot yellow"></span> {{ label('attendanceStatus', 'SICK') }}</div>
                <div class="legend-item"><span class="dot red"></span> {{ label('attendanceStatus', 'ABSENT') }}</div>
                <div class="legend-item"><span class="dot unmarked"></span> {{ label('attendanceStatus', 'UNMARKED') }}</div>
              </div>
            </template>
          </div>

          <!-- История посещений -->
          <div class="card table-card">
            <h3>История посещений</h3>
            <StateBlock v-if="loading" kind="loading" />
            <StateBlock v-else-if="error" kind="error" :message="errorText(error)" />
            <StateBlock v-else-if="!history.length" kind="empty" :message="`За ${monthName} прошедших занятий нет`" />
            <div v-else class="table">
              <div class="table-header">
                <div class="col date">ДАТА</div>
                <div class="col training">ТРЕНИРОВКА</div>
                <div class="col coach">ГРУППА</div>
                <div class="col status">СТАТУС</div>
              </div>
              <div v-for="row in history" :key="row.key" class="table-row">
                <div class="col date">{{ row.date }}</div>
                <div class="col training">{{ row.training }}</div>
                <div class="col coach">{{ row.group }}</div>
                <div class="col status">
                  <span class="status-badge" :class="row.statusClass" :title="row.hint || null">{{ row.status }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import MetricCard from '../../components/layout/MetricCard.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { attendanceApi } from '../../api/attendance'
import { groupsApi } from '../../api/groups'
import { announcementsApi } from '../../api/announcements'
import {
  myAthletes, selectedAthleteId, selectedAthlete, loadMyAthletes, selectAthlete, getOrganizationId
} from '../../utils/session'
import { formatDate, formatTime, toIsoDate, fullName, initials, errorText } from '../../utils/format'
import { label, tone } from '../../utils/labels'

const NO_CHILD = 'Ребёнок ещё не привязан — обратитесь к тренеру'
const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь']

function plural(n, one, few, many) {
  const m10 = n % 10
  const m100 = n % 100
  if (m10 === 1 && m100 !== 11) return one
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return few
  return many
}

// ─────────── месяц ───────────

const today = new Date()
const monthStart = ref(new Date(today.getFullYear(), today.getMonth(), 1))

const isCurrentMonth = computed(() => {
  const now = new Date()
  return monthStart.value.getFullYear() === now.getFullYear() && monthStart.value.getMonth() === now.getMonth()
})

function shiftMonth(delta) {
  const m = monthStart.value
  monthStart.value = new Date(m.getFullYear(), m.getMonth() + delta, 1)
}

const monthLabel = computed(() => {
  const m = monthStart.value
  return m.getFullYear() === new Date().getFullYear() ? MONTHS[m.getMonth()] : `${MONTHS[m.getMonth()]} ${m.getFullYear()}`
})
const monthName = computed(() => monthLabel.value.toLowerCase())

// ─────────── загрузка ───────────

const childrenReady = ref(false)
const childrenError = ref(null)

const entries = ref([])   // AttendanceEntry[] за месяц
const summary = ref(null) // AttendanceSummary
const loading = ref(false)
const error = ref(null)

const groups = ref([])
const hasUnread = ref(false)

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  entries.value = []
  summary.value = null
  error.value = null
  if (!athleteId) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const org = getOrganizationId()
    const from = monthStart.value
    const last = new Date(from.getFullYear(), from.getMonth() + 1, 0)
    // GET /attendance: журнал прошедших неотменённых занятий; summary считается по всему периоду
    const items = []
    let first = null
    for (let page = 0; page < 10; page++) {
      const res = await attendanceApi.list(org, {
        athleteId, from: toIsoDate(from), to: toIsoDate(last), page, size: 100
      })
      if (seq !== loadSeq) return
      first = first || res
      items.push(...(res?.items || []))
      if (page + 1 >= (res?.totalPages || 0)) break
    }
    entries.value = items
    summary.value = first?.summary || null
  } catch (e) {
    if (seq === loadSeq) error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// Названия групп для таблицы (родителю видны текущие группы ребёнка) и отметка о новых объявлениях
async function loadReference() {
  try {
    const org = getOrganizationId()
    const [groupPage, unread] = await Promise.allSettled([
      groupsApi.list(org, { size: 100 }),
      announcementsApi.list(org, { status: 'PUBLISHED', unread: true, size: 1 })
    ])
    groups.value = groupPage.status === 'fulfilled' ? (groupPage.value?.items || []) : []
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

watch([selectedAthleteId, monthStart], () => {
  if (childrenReady.value && !childrenError.value) load()
})

const pageState = computed(() => {
  if (!childrenReady.value) return { kind: 'loading', message: '' }
  if (childrenError.value) return { kind: 'error', message: errorText(childrenError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CHILD }
  return null
})

// ─────────── метрики: процент — по отмеченным занятиям, PRESENT / (PRESENT + SICK + ABSENT) ───────────

const marked = computed(() => {
  const s = summary.value
  return s ? s.present + s.sick + s.absent : 0
})

const overallValue = computed(() => {
  if (loading.value) return '…'
  if (error.value || !summary.value) return '—'
  const p = summary.value.attendancePercent
  return p === null || p === undefined ? 'Нет отметок' : `${Math.round(Number(p))}% посещений`
})

const overallNote = computed(() => {
  if (loading.value) return ''
  if (error.value) return 'Не удалось загрузить посещаемость'
  const s = summary.value
  if (!s) return ''
  if (!marked.value) {
    return s.trainingCount
      ? `Прошло занятий: ${s.trainingCount}, отметок пока нет`
      : `За ${monthName.value} занятий не было`
  }
  const base = `Посещено ${s.present} из ${marked.value} ${plural(marked.value, 'занятия', 'занятий', 'занятий')} за ${monthName.value}`
  return s.unmarked ? `${base} · не отмечено: ${s.unmarked}` : base
})

const missedValue = computed(() => {
  if (loading.value) return '…'
  if (error.value || !summary.value) return '—'
  const n = summary.value.sick + summary.value.absent
  return `${n} ${plural(n, 'занятие', 'занятия', 'занятий')}`
})

const missedNote = computed(() => {
  const s = summary.value
  if (loading.value || !s) return ''
  return `${label('attendanceStatus', 'SICK')}: ${s.sick} · ${label('attendanceStatus', 'ABSENT')}: ${s.absent}`
})

// ─────────── календарь визитов ───────────

// Цвет дня: пропуск важнее болезни, болезнь — присутствия; только неотмеченные занятия — отдельный цвет
function dayStatus(list) {
  if (!list.length) return 'gray'
  const has = status => list.some(e => e.status === status)
  if (has('ABSENT')) return 'red'
  if (has('SICK')) return 'yellow'
  if (has('PRESENT')) return 'green'
  return 'unmarked'
}

const weeks = computed(() => {
  const from = monthStart.value
  const daysInMonth = new Date(from.getFullYear(), from.getMonth() + 1, 0).getDate()
  const byDay = new Map()
  for (const e of entries.value) {
    const key = toIsoDate(new Date(e.startsAt))
    if (!byDay.has(key)) byDay.set(key, [])
    byDay.get(key).push(e)
  }
  const cells = []
  const lead = (from.getDay() + 6) % 7 // неделя с понедельника
  for (let i = 0; i < lead; i++) cells.push({ key: `lead-${i}`, status: 'empty' })
  for (let d = 1; d <= daysInMonth; d++) {
    const date = new Date(from.getFullYear(), from.getMonth(), d)
    const key = toIsoDate(date)
    const list = byDay.get(key) || []
    const details = list.length
      ? list.map(e => `${formatTime(e.startsAt)} ${e.trainingTitle} — ${label('attendanceStatus', e.status)}`).join('\n')
      : 'занятий нет'
    cells.push({ key, status: dayStatus(list), title: `${formatDate(date, { withYear: false })}: ${details}` })
  }
  while (cells.length % 7) cells.push({ key: `tail-${cells.length}`, status: 'empty' })
  const rows = []
  for (let i = 0; i < cells.length; i += 7) rows.push(cells.slice(i, i + 7))
  return rows
})

// ─────────── история ───────────

const groupById = computed(() => new Map(groups.value.map(g => [g.id, g])))

const history = computed(() => entries.value.map(e => ({
  key: `${e.trainingId}-${e.athleteId}`,
  date: formatDate(e.startsAt, { withYear: false }),
  training: e.trainingTitle,
  group: groupById.value.get(e.groupId)?.name || '—',
  status: label('attendanceStatus', e.status),
  statusClass: `status-${tone(e.status)}`,
  hint: [e.reason, e.comment].filter(Boolean).join(' · ')
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

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.details-grid {
  display: grid;
  grid-template-columns: 450px 1fr;
  gap: 24px;
}
@media (max-width: 1200px) { .details-grid { grid-template-columns: 1fr; } }

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

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 16px;
}
.card h3 { font-size: 16px; font-weight: 700; color: #152421; }

.heatmap-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.month-nav { display: flex; gap: 8px; }
.nav-btn {
  padding: 8px; background: white; border: 1px solid #E3EAE8;
  border-radius: 8px; cursor: pointer;
  display: flex; justify-content: center; align-items: center;
}
.nav-btn:disabled { opacity: 0.4; cursor: default; }

.heatmap { display: flex; flex-direction: column; gap: 8px; }
.heatmap-header {
  display: grid; grid-template-columns: repeat(7, 1fr);
  gap: 8px; font-size: 11px; font-weight: 700;
  color: #6D7D79; text-align: center;
}
.heatmap-row {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px;
}
.heatmap-day {
  height: 40px; border-radius: 8px;
}
.heatmap-day.green { background: #2E8B57; }
.heatmap-day.yellow { background: #F2B705; }
.heatmap-day.red { background: #D64545; }
.heatmap-day.gray { background: #F4F7F8; }
.heatmap-day.unmarked { background: #C9D3D0; }
.heatmap-day.empty { background: transparent; }

.legend { display: flex; flex-direction: column; gap: 8px; }
.legend-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; color: #152421;
}
.dot { width: 16px; height: 16px; border-radius: 4px; }
.dot.green { background: #2E8B57; }
.dot.yellow { background: #F2B705; }
.dot.red { background: #D64545; }
.dot.unmarked { background: #C9D3D0; }

.table { display: flex; flex-direction: column; gap: 0; }
.table-header, .table-row {
  display: grid;
  grid-template-columns: 120px 180px 150px 1fr;
  gap: 12px; padding: 12px 0;
  border-bottom: 1px solid #E3EAE8;
}
.table-header {
  font-size: 11px; font-weight: 700;
  color: #6D7D79; text-transform: uppercase;
  padding-bottom: 8px;
}
.table-row { font-size: 13px; align-items: center; }
.col.date { font-weight: 600; color: #152421; }
.col.training { color: #6D7D79; }
.col.coach { color: #6D7D79; }
.col.status { display: flex; }

.status-badge {
  padding: 4px 10px; border-radius: 99px;
  font-size: 11px; font-weight: 700;
}
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF1D6; color: #8B6914; }
.status-red { background: #FCE2E5; color: #D64545; }
.status-blue { background: #DDECFB; color: #3B82F6; }
.status-gray { background: #F4F7F8; color: #888888; }
</style>
