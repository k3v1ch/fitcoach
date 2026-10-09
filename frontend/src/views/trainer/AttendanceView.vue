<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        v-model="searchQuery"
        title="Журнал посещаемости"
        :subtitle="subtitle"
        search-placeholder="Поиск спортсмена..."
      >
        <template #actions>
          <BaseButton v-if="canMark" :disabled="!groupId" @click="openMarking">
            <BaseIcon name="check" :size="16" color="#102522" />
            Отметить посещаемость
          </BaseButton>
          <BaseButton
            v-if="canExport"
            :loading="exporting"
            title="Отчёт ATTENDANCE за выбранный месяц по всей организации"
            @click="handleExport"
          >
            <BaseIcon name="plus" :size="16" color="#102522" />
            Экспорт в CSV
          </BaseButton>
        </template>
      </PageHeader>

      <!-- Группа и месяц -->
      <div class="selector-bar">
        <div class="selector">
          <span>Группа:</span>
          <select v-model="groupId" :disabled="!groups.length">
            <option v-if="!groups.length" value="">Групп нет</option>
            <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
        </div>
        <div class="selector">
          <span>Месяц:</span>
          <button class="nav-btn" @click="shiftMonth(-1)">
            <BaseIcon name="chevron-left" :size="14" color="#152421" />
          </button>
          <span class="period-label">{{ monthLabel }}</span>
          <button class="nav-btn" @click="shiftMonth(1)">
            <BaseIcon name="chevron-right" :size="14" color="#152421" />
          </button>
        </div>
      </div>
      <p v-if="actionError" class="notice">{{ actionError }}</p>

      <!-- Статистика -->
      <div class="stats-strip">
        <StatCard label="Занятий проведено" :value="stats.trainings" :subtext="`За ${monthLabel.toLowerCase()}`" />
        <StatCard label="Средняя явка" :value="stats.percent" subtext="Без учёта неотмеченных" />
        <StatCard label="Пропуски по болезни" :value="stats.sick" :subtext="`За ${monthLabel.toLowerCase()}`" />
      </div>

      <!-- Таблица посещаемости -->
      <div class="ledger-card">
        <h3>График присутствия</h3>
        <StateBlock v-if="groupsLoading" kind="loading" />
        <StateBlock v-else-if="groupsError" kind="error" :message="errorText(groupsError)" />
        <StateBlock v-else-if="!groups.length" kind="empty" message="Групп пока нет — создайте группу и зачислите спортсменов" />
        <StateBlock v-else-if="loading" kind="loading" />
        <StateBlock v-else-if="error" kind="error" :message="errorText(error)" />
        <StateBlock v-else-if="!ledger.length" kind="empty" message="За этот месяц нет проведённых тренировок группы" />
        <template v-else>
          <div class="ledger-header">
            <div class="athlete-name">СПОРТСМЕН</div>
            <div class="dates">
              <span
                v-for="t in columns"
                :key="t.id"
                class="date-head"
                :title="`${t.title} · ${formatTime(t.startsAt)} — открыть журнал тренировки`"
                @click="openReport(t.id)"
              >{{ shortDate(t.startsAt) }}</span>
            </div>
            <div class="percent">% ЯВКИ</div>
          </div>
          <div v-for="athlete in filteredLedger" :key="athlete.athleteId" class="ledger-row">
            <div class="athlete-name">{{ athlete.name }}</div>
            <div class="dates">
              <span v-for="(mark, i) in athlete.marks" :key="i" class="mark" :class="mark.color" :title="mark.title">
                {{ mark.label }}
              </span>
            </div>
            <div class="percent">{{ athlete.percent === null ? '—' : `${athlete.percent}%` }}</div>
          </div>
          <StateBlock v-if="!filteredLedger.length" kind="empty" message="Спортсмены не найдены" />
          <p class="ledger-note">✓ — был, б — болел, н — не был, – — не отмечен. % явки — без учёта неотмеченных.</p>
        </template>
      </div>

      <!-- Массовая отметка -->
      <BaseModal
        v-model="markOpen"
        title="Отметка посещаемости"
        :submit-label="markSaving ? 'Сохранение…' : 'Сохранить отметки'"
        :width="620"
        @submit="submitMarking"
      >
        <div class="form-field">
          <label class="form-label" for="mark-training">Тренировка группы {{ groupName }}</label>
          <select id="mark-training" v-model="markTrainingId" class="form-select" :disabled="!markableTrainings.length">
            <option value="" disabled>Выберите тренировку</option>
            <option v-for="t in markableTrainings" :key="t.id" :value="t.id">{{ trainingLabel(t) }}</option>
          </select>
        </div>
        <StateBlock
          v-if="!markableTrainings.length"
          kind="empty"
          message="В этом месяце нет начавшихся запланированных тренировок группы"
        />
        <StateBlock v-else-if="markLoading" kind="loading" />
        <template v-else-if="markRows.length">
          <div class="mark-toolbar">
            <span class="form-hint">Отмечено {{ markedCount }} из {{ markRows.length }}</span>
            <button type="button" class="link-btn" @click="markAll('PRESENT')">Все присутствуют</button>
          </div>
          <div class="mark-list">
            <div v-for="row in markRows" :key="row.athleteId" class="mark-row">
              <span class="mark-name">{{ row.name }}</span>
              <input
                v-if="row.status === 'SICK' || row.status === 'ABSENT'"
                v-model="row.reason"
                class="mark-reason"
                type="text"
                placeholder="Причина"
              />
              <select v-model="row.status" class="mark-select" :aria-label="`Отметка: ${row.name}`">
                <option value="UNMARKED" disabled>{{ label('attendanceStatus', 'UNMARKED') }}</option>
                <option v-for="o in STATUS_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
          </div>
        </template>
        <StateBlock v-else-if="markTrainingId" kind="empty" message="В составе группы на дату тренировки нет спортсменов" />
        <p v-if="markNote" class="form-hint">{{ markNote }}</p>
        <p v-if="markError" class="form-error">{{ markError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import StatCard from '../../components/layout/StatCard.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { api } from '../../api/index'
import { trainingsApi } from '../../api/trainings'
import { attendanceApi } from '../../api/attendance'
import { groupsApi } from '../../api/groups'
import { financeApi } from '../../api/finance'
import { getOrganizationId, hasPermission, hasRole } from '../../utils/session'
import { periodRange, addDays, toIsoDate, toIsoDateTime, formatDate, formatTime, errorText } from '../../utils/format'
import { label, options } from '../../utils/labels'

const router = useRouter()

const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь']
const STATUS_OPTIONS = options('attendanceStatus').filter(o => o.value !== 'UNMARKED')
const MARKS = {
  PRESENT: { label: '✓', color: 'green' },
  SICK: { label: 'б', color: 'yellow' },
  ABSENT: { label: 'н', color: 'red' },
  UNMARKED: { label: '–', color: 'gray' }
}

const searchQuery = ref('')
const groups = ref([])
const groupsLoading = ref(true)
const groupsError = ref(null)
const groupId = ref('')
const anchor = ref(new Date())

const trainings = ref([])   // тренировки группы за месяц (Training[])
const roster = ref([])      // состав группы с историей (GroupAthlete[])
const records = ref([])     // отметки журнала (Attendance[])
const summary = ref(null)   // AttendanceSummary
const loading = ref(false)
const error = ref(null)
const actionError = ref('')
const exporting = ref(false)

const canMark = computed(() => hasRole('TRAINER') && hasPermission('attendance.write'))
const canExport = computed(() => hasPermission('reports.read') && hasPermission('reports.export'))

const range = computed(() => periodRange('month', anchor.value))
// Даты журнала включают обе границы
const period = computed(() => ({ from: toIsoDate(range.value.from), to: toIsoDate(addDays(range.value.to, -1)) }))
const monthLabel = computed(() => `${MONTHS[anchor.value.getMonth()]} ${anchor.value.getFullYear()}`)
const groupName = computed(() => groups.value.find(g => g.id === groupId.value)?.name || '')
const subtitle = computed(() => (groupName.value
  ? `Статистика группы ${groupName.value} · ${monthLabel.value}`
  : monthLabel.value))

const pad = n => String(n).padStart(2, '0')
const shortDate = iso => {
  const d = new Date(iso)
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}`
}
const trainingLabel = t => `${formatDate(t.startsAt, { withYear: false })}, ${formatTime(t.startsAt)} · ${t.title}`

function plural(n, [one, few, many]) {
  const mod100 = Math.abs(n) % 100
  const mod10 = mod100 % 10
  if (mod100 > 10 && mod100 < 20) return many
  if (mod10 > 1 && mod10 < 5) return few
  if (mod10 === 1) return one
  return many
}

// ─────────── журнал ───────────

const columns = computed(() => {
  const ids = new Set(records.value.map(r => r.trainingId))
  return trainings.value
    .filter(t => ids.has(t.id))
    .sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt))
})

const ledger = computed(() => {
  const names = new Map(roster.value.map(a => [a.athleteId, a.fullName]))
  const athletes = new Map()
  for (const r of records.value) {
    if (!athletes.has(r.athleteId)) {
      athletes.set(r.athleteId, { athleteId: r.athleteId, name: r.athleteName || names.get(r.athleteId) || '—', marks: new Map() })
    }
    athletes.get(r.athleteId).marks.set(r.trainingId, r.status)
  }
  return [...athletes.values()].map(a => {
    let present = 0
    let counted = 0
    const marks = columns.value.map(t => {
      const status = a.marks.get(t.id)
      if (!status) return { label: '', color: 'none', title: 'Не в составе на эту дату' }
      if (status === 'PRESENT') present++
      if (status !== 'UNMARKED') counted++
      return { ...(MARKS[status] || MARKS.UNMARKED), title: `${shortDate(t.startsAt)} · ${label('attendanceStatus', status)}` }
    })
    // Процент = present / (present + sick + absent); UNMARKED не входит в знаменатель
    return { athleteId: a.athleteId, name: a.name, marks, percent: counted ? Math.round((present / counted) * 100) : null }
  }).sort((x, y) => x.name.localeCompare(y.name, 'ru'))
})

// GET /attendance не ищет по имени — фильтруем загруженный журнал
const filteredLedger = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  return q ? ledger.value.filter(a => a.name.toLowerCase().includes(q)) : ledger.value
})

const stats = computed(() => {
  const s = summary.value
  if (!s) return { trainings: '—', percent: '—', sick: '—' }
  const percent = s.attendancePercent === null || s.attendancePercent === undefined
    ? '—'
    : `${Number(s.attendancePercent).toLocaleString('ru-RU', { maximumFractionDigits: 1 })}%`
  return {
    trainings: `${s.trainingCount} ${plural(s.trainingCount, ['тренировка', 'тренировки', 'тренировок'])}`,
    percent,
    sick: `${s.sick} ${plural(s.sick, ['случай', 'случая', 'случаев'])}`
  }
})

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

// Строки отчёта ATTENDANCE (training_id, starts_at, athlete_id, attendance_status, reason) за даты [from, to]
async function reportRows(org, from, to) {
  const result = []
  for (let page = 0; page < 30; page++) {
    const res = await financeApi.reports(org, 'ATTENDANCE', { from, to, page, size: 100 })
    result.push(...(res?.rows || []))
    if (page + 1 >= (res?.totalPages || 0)) break
  }
  return result
}

// Состав группы на дату занятия — как на сервере: дата начала в UTC, joinedOn ≤ дата ≤ leftOn
function rosterOn(athletes, startsAt) {
  const day = new Date(startsAt).toISOString().slice(0, 10)
  return athletes.filter(a => a.joinedOn <= day && (!a.leftOn || a.leftOn >= day))
}

async function loadGroups() {
  groupsLoading.value = true
  groupsError.value = null
  try {
    const list = await fetchAll(p => groupsApi.list(getOrganizationId(), { status: 'ACTIVE', ...p }))
    groups.value = list.sort((a, b) => a.name.localeCompare(b.name, 'ru'))
    if (!groups.value.some(g => g.id === groupId.value)) groupId.value = groups.value[0]?.id || ''
  } catch (e) {
    groupsError.value = e
  } finally {
    groupsLoading.value = false
  }
}

// Журнал по контракту (6.9, №033): AttendanceList {items, …, summary}
async function journalFromApi(org, params) {
  const items = []
  let journalSummary = null
  for (let page = 0; page < 30; page++) {
    const res = await attendanceApi.list(org, { ...params, page, size: 100 })
    items.push(...(res?.items || []))
    journalSummary = journalSummary || res?.summary || null
    if (page + 1 >= (res?.totalPages || 0)) break
  }
  return { items, summary: journalSummary }
}

// Пока GET /attendance не выкачен — тот же журнал из отчёта ATTENDANCE по тем же правилам:
// неотменённые занятия, закончившиеся к моменту запроса; участники — по истории состава
async function journalFromReport(org, trainingList, athletes, { from, to }) {
  const now = Date.now()
  const held = trainingList.filter(t => t.status !== 'CANCELLED' && new Date(t.endsAt).getTime() <= now)
  const heldIds = new Set(held.map(t => t.id))
  const marks = new Map()
  const rows = await reportRows(org, toIsoDate(addDays(from, -1)), toIsoDate(addDays(to, 1)))
  for (const r of rows) {
    const trainingId = String(r.training_id)
    if (!heldIds.has(trainingId)) continue
    if (!marks.has(trainingId)) marks.set(trainingId, new Map())
    marks.get(trainingId).set(String(r.athlete_id), { status: r.attendance_status, reason: r.reason })
  }
  const names = new Map(athletes.map(a => [a.athleteId, a.fullName]))
  const items = []
  for (const t of held) {
    const own = marks.get(t.id) || new Map()
    const ids = new Set(rosterOn(athletes, t.startsAt).map(a => a.athleteId))
    own.forEach((_, athleteId) => ids.add(athleteId))
    ids.forEach(athleteId => items.push({
      trainingId: t.id,
      athleteId,
      athleteName: names.get(athleteId) || '—',
      status: own.get(athleteId)?.status || 'UNMARKED',
      reason: own.get(athleteId)?.reason ?? null,
      comment: null
    }))
  }
  const count = status => items.filter(i => i.status === status).length
  const present = count('PRESENT')
  const sick = count('SICK')
  const absent = count('ABSENT')
  return {
    items,
    summary: {
      from,
      to,
      trainingCount: held.length,
      participantRecords: items.length,
      present,
      sick,
      absent,
      unmarked: count('UNMARKED'),
      attendancePercent: present + sick + absent ? (present / (present + sick + absent)) * 100 : null
    }
  }
}

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  actionError.value = ''
  if (!groupId.value) {
    trainings.value = []
    roster.value = []
    records.value = []
    summary.value = null
    return
  }
  loading.value = true
  error.value = null
  try {
    const org = getOrganizationId()
    const { from, to } = range.value
    const dates = { ...period.value }
    const group = groupId.value
    const [trainingList, detail] = await Promise.all([
      fetchAll(p => trainingsApi.list(org, { from: toIsoDateTime(from), to: toIsoDateTime(to), groupId: group, ...p })),
      groupsApi.get(org, group, { includeFormer: true })
    ])
    const athletes = detail?.athletes || []
    let journal
    try {
      journal = await journalFromApi(org, { from: dates.from, to: dates.to, groupId: group })
    } catch (e) {
      if (e.status !== 404 && e.status !== 405) throw e
      journal = await journalFromReport(org, trainingList, athletes, dates)
    }
    if (seq !== loadSeq) return
    trainings.value = trainingList
    roster.value = athletes
    records.value = journal.items
    summary.value = journal.summary
  } catch (e) {
    if (seq === loadSeq) {
      error.value = e
      records.value = []
      summary.value = null
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

watch([groupId, anchor], load)
onMounted(loadGroups)

function shiftMonth(step) {
  anchor.value = new Date(anchor.value.getFullYear(), anchor.value.getMonth() + step, 1)
}

const openReport = id => router.push(`/trainer/trainings/${id}/report`)

// ─────────── массовая отметка ───────────

const markOpen = ref(false)
const markTrainingId = ref('')
const markRows = ref([])
const markLoading = ref(false)
const markSaving = ref(false)
const markError = ref('')
const markNote = ref('')

// Отмечать можно запланированные тренировки, начавшиеся не позже сегодняшнего дня (свежие — первыми)
const markableTrainings = computed(() => {
  const now = new Date()
  const limit = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1)
  return trainings.value
    .filter(t => t.status === 'PLANNED' && new Date(t.startsAt) < limit)
    .sort((a, b) => new Date(b.startsAt) - new Date(a.startsAt))
})
const markedCount = computed(() => markRows.value.filter(r => r.status !== 'UNMARKED').length)

function openMarking() {
  markError.value = ''
  markNote.value = ''
  markOpen.value = true
  const keep = markableTrainings.value.some(t => t.id === markTrainingId.value)
  const next = keep ? markTrainingId.value : (markableTrainings.value[0]?.id || '')
  if (next === markTrainingId.value) loadMarkRows()
  else markTrainingId.value = next
}

watch(markTrainingId, () => {
  if (markOpen.value) loadMarkRows()
})

let markSeq = 0
async function loadMarkRows() {
  const seq = ++markSeq
  markError.value = ''
  markNote.value = ''
  const t = trainings.value.find(x => x.id === markTrainingId.value)
  if (!t) {
    markRows.value = []
    return
  }
  markLoading.value = true
  try {
    // Текущие отметки: из журнала, если занятие уже в нём, иначе — из отчёта ATTENDANCE за день
    const known = new Map(records.value.filter(r => r.trainingId === t.id).map(r => [r.athleteId, r]))
    if (!known.size) {
      try {
        const rows = await reportRows(getOrganizationId(), toIsoDate(addDays(t.startsAt, -1)), toIsoDate(addDays(t.startsAt, 1)))
        rows
          .filter(r => String(r.training_id) === t.id)
          .forEach(r => known.set(String(r.athlete_id), { status: r.attendance_status, reason: r.reason, comment: null }))
      } catch (e) {
        if (seq === markSeq) markNote.value = `Сохранённые отметки не загрузились (${errorText(e)}).`
      }
    }
    if (seq !== markSeq) return
    const names = new Map(roster.value.map(a => [a.athleteId, a.fullName]))
    const ids = new Set(rosterOn(roster.value, t.startsAt).map(a => a.athleteId))
    known.forEach((_, athleteId) => ids.add(athleteId))
    markRows.value = [...ids].map(athleteId => {
      const m = known.get(athleteId)
      const status = m?.status || 'UNMARKED'
      const reason = m?.reason || ''
      return {
        athleteId,
        name: m?.athleteName || names.get(athleteId) || '—',
        status,
        reason,
        comment: m?.comment ?? null,
        savedStatus: status,
        savedReason: status === 'PRESENT' ? '' : reason
      }
    }).sort((a, b) => a.name.localeCompare(b.name, 'ru'))
  } finally {
    if (seq === markSeq) markLoading.value = false
  }
}

function markAll(status) {
  markRows.value.forEach(row => { row.status = status })
}

const reasonOf = row => (row.status === 'PRESENT' ? '' : (row.reason || '').trim())

// TrainingController.attendance принимает массив AttendanceWrite и возвращает Attendance[];
// общий trainingsApi.saveAttendance шлёт {items} — поэтому локальный вызов (см. итог, shared_needs)
async function putAttendance(org, trainingId, items) {
  const res = await api.put(`/organizations/${org}/trainings/${trainingId}/attendance`, items)
  return Array.isArray(res) ? res : (res?.items || [])
}

async function submitMarking() {
  if (markSaving.value) return
  const t = trainings.value.find(x => x.id === markTrainingId.value)
  if (!t) {
    markError.value = 'Выберите тренировку.'
    return
  }
  // Только новые и изменённые отметки — остальные записи сервер не меняет
  const items = markRows.value
    .filter(r => r.status !== 'UNMARKED' && (r.status !== r.savedStatus || reasonOf(r) !== r.savedReason))
    .map(r => ({ athleteId: r.athleteId, status: r.status, reason: reasonOf(r) || null, comment: r.comment ?? null }))
  if (!items.length) {
    markError.value = 'Нет новых отметок для сохранения.'
    return
  }
  markSaving.value = true
  markError.value = ''
  try {
    await putAttendance(getOrganizationId(), t.id, items)
    markOpen.value = false
    await load()
  } catch (e) {
    markError.value = errorText(e)
  } finally {
    markSaving.value = false
  }
}

// ─────────── экспорт ───────────

// Бэкенд выгружает только CSV и пока без фильтра по группе — весь отчёт ATTENDANCE за месяц
async function handleExport() {
  if (exporting.value) return
  actionError.value = ''
  exporting.value = true
  try {
    const { from, to } = period.value
    const blob = await financeApi.exportReport(getOrganizationId(), 'ATTENDANCE', { from, to })
    if (!blob || !blob.size) {
      actionError.value = 'За выбранный месяц нет отметок для выгрузки.'
      return
    }
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `attendance_${from}_${to}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    actionError.value = errorText(e)
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
/* стили аналогичны другим страницам */
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.selector-bar {
  display: flex; gap: 24px; flex-wrap: wrap; align-items: center;
  background: white; padding: 16px; border-radius: 12px;
  border: 1px solid #E3EAE8;
}
.selector { display: flex; align-items: center; gap: 8px; }
.selector > span { font-size: 14px; color: #6D7D79; }
.selector select {
  padding: 8px 12px; border: 1px solid #E3EAE8;
  border-radius: 8px; background: #F4F7F8;
  font-size: 14px; font-weight: 600; color: #152421;
  cursor: pointer; outline: none; font-family: inherit;
}
.selector .period-label { min-width: 130px; text-align: center; font-size: 14px; font-weight: 700; color: #152421; }
.nav-btn {
  padding: 6px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  cursor: pointer; display: flex;
  justify-content: center; align-items: center;
}
.nav-btn:hover { background: #F4F7F8; }
.notice { font-size: 13px; color: #D64545; }

.stats-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }

.ledger-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
  overflow-x: auto;
}
.ledger-card h3 { font-size: 18px; font-weight: 700; color: #152421; }

.ledger-header, .ledger-row {
  display: flex; align-items: center; gap: 16px;
  min-width: 800px;
  padding: 12px 16px;
  border-radius: 12px;
}
.ledger-header { background: #F4F7F8; font-size: 12px; font-weight: 700; color: #6D7D79; }
.ledger-row { border-bottom: 1px solid #E3EAE8; }

.athlete-name { width: 180px; font-size: 14px; font-weight: 700; color: #152421; }
.dates { flex: 1; display: flex; justify-content: center; gap: 12px; }
/* подпись даты шириной с отметку — колонки совпадают со строками */
.date-head { width: 24px; display: inline-flex; justify-content: center; white-space: nowrap; cursor: pointer; }
.date-head:hover { color: #152421; }
.mark {
  width: 24px; height: 24px; border-radius: 50%;
  display: flex; justify-content: center; align-items: center;
  font-size: 11px; font-weight: 700; color: white;
  flex-shrink: 0;
}
.mark.green { background: #2E8B57; }
.mark.red { background: #D64545; }
.mark.yellow { background: #F2B705; }
.mark.gray { background: #888888; }
.mark.none { background: transparent; }
.percent { width: 80px; text-align: right; font-size: 14px; font-weight: 700; color: #152421; }
.ledger-note { font-size: 12px; color: #98A6A2; }

/* Модалка отметки */
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.form-select {
  width: 100%; min-height: 40px; padding: 8px 12px;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 14px; color: #152421; background: white;
  cursor: pointer; outline: none; font-family: inherit;
}
.form-hint { font-size: 12px; color: #98A6A2; }
.form-error { font-size: 13px; color: #D64545; }
.mark-toolbar { display: flex; justify-content: space-between; align-items: center; }
.link-btn {
  padding: 6px 12px; background: #F4F7F8;
  border: none; border-radius: 8px;
  font-size: 13px; font-weight: 600; color: #152421; cursor: pointer;
}
.mark-list { display: flex; flex-direction: column; }
.mark-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 0; border-bottom: 1px solid #E3EAE8;
}
.mark-name { flex: 1; min-width: 0; font-size: 14px; font-weight: 600; color: #152421; }
.mark-reason, .mark-select {
  padding: 6px 12px; border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 13px; color: #152421; background: white; outline: none; font-family: inherit;
}
.mark-reason { width: 160px; }
.mark-select { cursor: pointer; }
</style>
