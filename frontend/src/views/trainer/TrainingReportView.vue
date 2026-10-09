<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Заполнение отчёта по тренировке"
        :subtitle="subtitle"
        :show-back="true"
        :show-search="false"
        @back="$router.push('/trainer/trainings')"
      />

      <StateBlock v-if="loading" kind="loading" />
      <StateBlock v-else-if="loadError" kind="error" :message="errorText(loadError)" />

      <div v-else class="report-form">
        <div class="meta-row">
          <div class="field">
            <label>ДАТА ПРОВЕДЕНИЯ</label>
            <input :value="dateLabel" class="field-input" type="text" readonly />
          </div>
          <div class="field">
            <label>ВРЕМЯ</label>
            <input :value="timeLabel" class="field-input" type="text" readonly />
          </div>
          <div class="field">
            <label>МЕСТО (ЗАЛ)</label>
            <input :value="venueName" class="field-input" type="text" readonly />
          </div>
        </div>

        <p v-if="isCancelled" class="notice notice-red">
          Тренировка отменена{{ training.cancelReason ? `: ${training.cancelReason}` : '' }}. Отчёт и отметки недоступны.
        </p>

        <div class="inputs-block">
          <div class="field">
            <label for="report-topic">ТЕМА ТРЕНИРОВКИ</label>
            <input id="report-topic" v-model="form.topic" class="field-input" type="text" :readonly="!canEditReport" />
          </div>
          <div class="field">
            <label for="report-content">ФАКТИЧЕСКОЕ ПРОВЕДЕНИЕ</label>
            <textarea id="report-content" v-model="form.actualContent" class="field-textarea" rows="4" :readonly="!canEditReport"></textarea>
          </div>
          <div class="field">
            <label for="report-comment">КОММЕНТАРИЙ ТРЕНЕРА</label>
            <textarea id="report-comment" v-model="form.comment" class="field-textarea" rows="2" :readonly="!canEditReport"></textarea>
          </div>
          <p v-if="reportNote" class="notice">{{ reportNote }}</p>
        </div>

        <div v-if="!isCancelled || rows.length" class="attendance-segment">
          <h3>Присутствие и результаты</h3>
          <p v-if="marksNote" class="notice">{{ marksNote }}</p>
          <StateBlock
            v-if="!rows.length && !isCancelled"
            kind="empty"
            message="В составе группы на дату тренировки нет спортсменов"
          />
          <div v-for="athlete in rows" :key="athlete.athleteId" class="attendance-row">
            <div class="athlete-info">
              <button
                class="checkbox"
                :class="{ checked: athlete.status === 'PRESENT' }"
                :disabled="!canEditAttendance"
                :title="label('attendanceStatus', athlete.status)"
                @click="togglePresent(athlete)"
              >
                <BaseIcon v-if="athlete.status === 'PRESENT'" name="check" :size="12" color="#102522" />
              </button>
              <span>{{ athlete.name }}</span>
            </div>
            <div class="mark-controls">
              <input
                v-if="(athlete.status === 'SICK' || athlete.status === 'ABSENT') && (canEditAttendance || athlete.reason)"
                v-model="athlete.reason"
                class="reason-input"
                type="text"
                placeholder="Причина"
                :aria-label="`Причина: ${athlete.name}`"
                :readonly="!canEditAttendance"
              />
              <select
                v-model="athlete.status"
                class="reason-select"
                :aria-label="`Отметка: ${athlete.name}`"
                :disabled="!canEditAttendance"
              >
                <option value="UNMARKED" disabled>{{ label('attendanceStatus', 'UNMARKED') }}</option>
                <option v-for="o in STATUS_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
          </div>
        </div>

        <p v-if="saveError" class="form-error">{{ saveError }}</p>
        <div class="actions-row">
          <span class="report-status" :class="{ saved: !!saveNote }">{{ saveNote || statusText }}</span>
          <template v-if="canEditReport || canEditAttendance">
            <button class="btn-outline" :disabled="!!saving" @click="handleDraft">
              {{ saving === 'draft' ? 'Сохранение…' : 'Сохранить черновик' }}
            </button>
            <button v-if="canEditReport" class="btn-primary" :disabled="!!saving" @click="handleSubmit">
              {{ saving === 'close' ? 'Закрытие…' : 'Закрыть отчёт' }}
            </button>
          </template>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { api } from '../../api/index'
import { trainingsApi } from '../../api/trainings'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { financeApi } from '../../api/finance'
import { getOrganizationId, hasPermission, hasRole } from '../../utils/session'
import { formatDate, formatTime, formatDateTime, toIsoDate, addDays, errorText } from '../../utils/format'
import { label, options } from '../../utils/labels'

const route = useRoute()
const STATUS_OPTIONS = options('attendanceStatus').filter(o => o.value !== 'UNMARKED')

const loading = ref(true)
const loadError = ref(null)
const training = ref(null)
const report = ref(null)
// true — сервер вернул раздел report (TrainingDetail) или отчёт сохранён в этой сессии
const reportKnown = ref(false)
const groupName = ref('')
const venueName = ref('—')
// { athleteId, name, status, reason, comment, savedStatus, savedReason }
const rows = ref([])
const marksNote = ref('')
const form = reactive({ topic: '', actualContent: '', comment: '' })
const saving = ref('') // '' | 'draft' | 'close'
const saveError = ref('')
const saveNote = ref('')

// ─────────── состояние ───────────

const isCancelled = computed(() => training.value?.status === 'CANCELLED')
const isClosed = computed(() => report.value?.status === 'CLOSED' || training.value?.status === 'COMPLETED')
// Менять можно только запланированную тренировку с незакрытым отчётом; запись — только тренеру с правом
const editable = computed(() => training.value?.status === 'PLANNED' && !isClosed.value && hasRole('TRAINER'))
const canEditReport = computed(() => editable.value && hasPermission('trainingReports.write'))
const canEditAttendance = computed(() => editable.value && hasPermission('attendance.write'))

const subtitle = computed(() => {
  const t = training.value
  if (!t) return ''
  return [`Отчёт за ${formatDate(t.startsAt, { withYear: false })}`, groupName.value, t.title].filter(Boolean).join(' · ')
})
const dateLabel = computed(() => {
  const t = training.value
  if (!t) return '—'
  const weekday = new Date(t.startsAt).toLocaleDateString('ru-RU', { weekday: 'long' })
  return `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)}, ${formatDate(t.startsAt)}`
})
const timeLabel = computed(() => (training.value
  ? `${formatTime(training.value.startsAt)} – ${formatTime(training.value.endsAt)}`
  : '—'))

const statusText = computed(() => {
  const t = training.value
  if (!t) return ''
  if (isCancelled.value) return label('trainingStatus', 'CANCELLED')
  const r = report.value
  if (isClosed.value) return r?.closedAt ? `Отчёт закрыт ${formatDateTime(r.closedAt)}` : 'Отчёт закрыт'
  if (r) return `${label('reportStatus', r.status)} · сохранён ${formatDateTime(r.updatedAt)}`
  return reportKnown.value ? 'Отчёт ещё не сохранялся' : ''
})

// GET /trainings/{id} в текущем бэкенде отдаёт Training без раздела report — честно предупреждаем
const reportNote = computed(() => {
  if (reportKnown.value || isCancelled.value || !training.value) return ''
  if (isClosed.value) return 'Текст закрытого отчёта здесь не показывается: API тренировки пока не возвращает отчёт.'
  return canEditReport.value
    ? 'Ранее сохранённый черновик здесь не показывается: API тренировки пока не возвращает отчёт. Новое сохранение заменит его.'
    : 'Текст отчёта здесь не показывается: API тренировки пока не возвращает отчёт.'
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

function makeRow(athleteId, name, status = 'UNMARKED', reason = null, comment = null) {
  const savedReason = status === 'PRESENT' ? '' : (reason || '')
  return { athleteId, name, status, reason: reason || '', comment, savedStatus: status, savedReason }
}

const sortRows = list => list.sort((a, b) => a.name.localeCompare(b.name, 'ru'))

// Attendance[] (из TrainingDetail или ответа PUT /attendance) → строки формы
function setRows(attendance) {
  rows.value = sortRows((attendance || []).map(a =>
    makeRow(String(a.athleteId), a.athleteName || '—', a.status || 'UNMARKED', a.reason, a.comment ?? null)))
}

// Участники — состав группы на дату занятия (дата начала в UTC, как считает сервер)
// плюс спортсмены, у которых уже есть строка посещаемости; отметки — из отчёта ATTENDANCE
async function rosterWithMarks(org, t, athletes) {
  const day = new Date(t.startsAt).toISOString().slice(0, 10)
  const names = new Map(athletes.map(a => [a.athleteId, a.fullName]))
  const result = new Map()
  athletes
    .filter(a => a.joinedOn <= day && (!a.leftOn || a.leftOn >= day))
    .forEach(a => result.set(a.athleteId, makeRow(a.athleteId, a.fullName)))
  try {
    const marks = (await reportRows(org, toIsoDate(addDays(t.startsAt, -1)), toIsoDate(addDays(t.startsAt, 1))))
      .filter(r => String(r.training_id) === t.id)
    for (const m of marks) {
      const athleteId = String(m.athlete_id)
      const name = names.get(athleteId) || result.get(athleteId)?.name || '—'
      result.set(athleteId, makeRow(athleteId, name, m.attendance_status || 'UNMARKED', m.reason))
    }
  } catch (e) {
    marksNote.value = `Сохранённые отметки не загрузились (${errorText(e)}) — проверьте их перед сохранением.`
  }
  return sortRows([...result.values()])
}

function applyReport(res) {
  // Контракт (6.9, №031): {report, trainingStatus}; текущий бэкенд отдаёт сам TrainingReport
  const r = res && Object.prototype.hasOwnProperty.call(res, 'report') ? res.report : res
  report.value = r || null
  if (r) {
    reportKnown.value = true
    form.topic = r.topic || ''
    form.actualContent = r.actualContent || ''
    form.comment = r.comment || ''
  }
}

async function load() {
  loading.value = true
  loadError.value = null
  saveError.value = ''
  saveNote.value = ''
  marksNote.value = ''
  Object.assign(form, { topic: '', actualContent: '', comment: '' })
  try {
    const org = getOrganizationId()
    const res = await trainingsApi.get(org, route.params.id)
    // Контракт (6.8, №029): TrainingDetail {trainingId, training, report, attendance};
    // текущий бэкенд отдаёт сам Training — тогда отчёт неизвестен, а отметки читаем из отчёта ATTENDANCE
    const isDetail = !!res && Object.prototype.hasOwnProperty.call(res, 'training')
    const t = isDetail ? res.training : res
    training.value = t
    report.value = null
    reportKnown.value = isDetail
    applyReport(isDetail ? res.report : null)

    const [groupDetail, venueList] = await Promise.all([
      t?.groupId ? groupsApi.get(org, t.groupId, { includeFormer: true }) : null,
      t?.venueId ? fetchAll(p => dictionariesApi.list(org, 'venues', p)).catch(() => []) : []
    ])
    groupName.value = groupDetail?.group?.name || ''
    venueName.value = venueList.find(v => v.id === t?.venueId)?.name || '—'

    if (isDetail && Array.isArray(res.attendance)) setRows(res.attendance)
    else if (t && !isCancelled.value) rows.value = await rosterWithMarks(org, t, groupDetail?.athletes || [])
    else rows.value = []
  } catch (e) {
    loadError.value = e
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, id => { if (id) load() })
onMounted(load)

// ─────────── отметки ───────────

function togglePresent(athlete) {
  if (!canEditAttendance.value) return
  athlete.status = athlete.status === 'PRESENT' ? 'ABSENT' : 'PRESENT'
}

const reasonOf = row => (row.status === 'PRESENT' ? '' : (row.reason || '').trim())

// Отправляем только новые и изменённые отметки — остальные записи сервер не трогает
function changedItems() {
  return rows.value
    .filter(r => r.status !== 'UNMARKED' && (r.status !== r.savedStatus || reasonOf(r) !== r.savedReason))
    .map(r => ({ athleteId: r.athleteId, status: r.status, reason: reasonOf(r) || null, comment: r.comment ?? null }))
}

// TrainingController.attendance принимает массив AttendanceWrite и возвращает Attendance[];
// общий trainingsApi.saveAttendance шлёт {items} — поэтому локальный вызов (см. итог, shared_needs)
async function putAttendance(org, trainingId, items) {
  const res = await api.put(`/organizations/${org}/trainings/${trainingId}/attendance`, items)
  return Array.isArray(res) ? res : (res?.items || [])
}

// ─────────── сохранение ───────────

async function handleDraft() {
  if (saving.value) return
  saveError.value = ''
  saveNote.value = ''
  const topic = form.topic.trim()
  const content = form.actualContent.trim()
  const withReport = canEditReport.value && !!(topic || content || form.comment.trim())
  if (withReport && (!topic || !content)) {
    saveError.value = 'Для черновика заполните тему и фактическое проведение.'
    return
  }
  const items = canEditAttendance.value ? changedItems() : []
  if (!withReport && !items.length) {
    saveError.value = 'Нет изменений: заполните отчёт или отметьте спортсменов.'
    return
  }
  saving.value = 'draft'
  try {
    const org = getOrganizationId()
    if (items.length) setRows(await putAttendance(org, training.value.id, items))
    if (withReport) {
      applyReport(await trainingsApi.saveReport(org, training.value.id, {
        topic, actualContent: content, comment: form.comment.trim() || null, status: 'DRAFT'
      }))
    }
    saveNote.value = withReport ? 'Черновик сохранён' : 'Отметки сохранены'
  } catch (e) {
    saveError.value = errorText(e)
  } finally {
    saving.value = ''
  }
}

// Закрытие: тема и содержание, отметки всех участников, тренировка уже закончилась (6.9, №031)
async function handleSubmit() {
  if (saving.value) return
  saveError.value = ''
  saveNote.value = ''
  const topic = form.topic.trim()
  const content = form.actualContent.trim()
  if (!topic || !content) {
    saveError.value = 'Для закрытия отчёта заполните тему и фактическое проведение.'
    return
  }
  if (rows.value.some(r => r.status === 'UNMARKED')) {
    saveError.value = 'Перед закрытием отчёта отметьте всех спортсменов.'
    return
  }
  if (new Date(training.value.endsAt).getTime() > Date.now()) {
    saveError.value = 'Отчёт можно закрыть только после окончания тренировки.'
    return
  }
  if (!window.confirm('Закрыть отчёт? После закрытия отчёт и посещаемость изменить нельзя.')) return
  saving.value = 'close'
  try {
    const org = getOrganizationId()
    const items = canEditAttendance.value ? changedItems() : []
    if (items.length) setRows(await putAttendance(org, training.value.id, items))
    const res = await trainingsApi.saveReport(org, training.value.id, {
      topic, actualContent: content, comment: form.comment.trim() || null, status: 'CLOSED'
    })
    applyReport(res)
    training.value = { ...training.value, status: res?.trainingStatus || 'COMPLETED' }
    saveNote.value = 'Отчёт закрыт'
  } catch (e) {
    saveError.value = errorText(e)
  } finally {
    saving.value = ''
  }
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.report-form {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 24px;
}

.meta-row {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.field { display: flex; flex-direction: column; gap: 6px; }
.field label {
  font-size: 11px; font-weight: 700; color: #98A6A2;
  text-transform: uppercase;
}
.field-input, .field-textarea {
  padding: 12px; background: #F4F7F8;
  border-radius: 12px; border: 1px solid #E3EAE8;
  font-size: 14px; color: #152421;
  font-family: inherit; outline: none;
  transition: border-color 0.2s;
}
.field-input:focus, .field-textarea:focus {
  border-color: #B7F34B;
  background: white;
}
.field-input[readonly]:focus, .field-textarea[readonly]:focus { border-color: #E3EAE8; background: #F4F7F8; }
.field-textarea { resize: vertical; }

.inputs-block { display: flex; flex-direction: column; gap: 16px; }

.notice {
  padding: 10px 14px; border-radius: 12px;
  background: #F4F7F8; font-size: 13px; color: #6D7D79;
}
.notice-red { background: #FCE2E5; color: #D64545; }

.attendance-segment { display: flex; flex-direction: column; gap: 12px; }
.attendance-segment h3 { font-size: 14px; font-weight: 700; color: #152421; }

.attendance-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0; border-bottom: 1px solid #E3EAE8;
}
.athlete-info { display: flex; align-items: center; gap: 12px; }

.checkbox {
  width: 20px; height: 20px;
  border: 1px solid #E3EAE8; border-radius: 6px;
  display: flex; justify-content: center; align-items: center;
  background: white; cursor: pointer; padding: 0;
}
.checkbox.checked { background: #B7F34B; border-color: #B7F34B; }
.checkbox:disabled { cursor: default; }

.mark-controls { display: flex; align-items: center; gap: 8px; }
.reason-input {
  width: 200px; padding: 6px 12px;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 13px; color: #152421; outline: none; font-family: inherit;
}
.reason-input:focus { border-color: #B7F34B; }
.reason-input[readonly] { color: #6D7D79; background: #F9FBFA; border-color: #E3EAE8; }

.reason-select {
  padding: 6px 12px;
  border: 1px solid #E3EAE8;
  border-radius: 8px;
  font-size: 13px;
  color: #152421;
  background: white;
  cursor: pointer;
  outline: none;
}
.reason-select:disabled { cursor: default; color: #6D7D79; background: #F9FBFA; }

.form-error { font-size: 13px; color: #D64545; }

.actions-row { display: flex; justify-content: flex-end; align-items: center; gap: 12px; }
.report-status { margin-right: auto; font-size: 13px; color: #6D7D79; }
.report-status.saved { color: #2E8B57; font-weight: 600; }
.btn-outline {
  padding: 12px 24px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 14px; font-weight: 600; color: #152421;
  cursor: pointer;
}
.btn-primary {
  padding: 12px 24px; background: #B7F34B;
  border: none; border-radius: 12px;
  font-size: 14px; font-weight: 700; color: #102522;
  cursor: pointer;
}
.btn-outline:disabled, .btn-primary:disabled { opacity: .6; cursor: not-allowed; }
</style>
