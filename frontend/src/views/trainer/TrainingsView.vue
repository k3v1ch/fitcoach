<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Тренировки"
        subtitle="Управление планом тренировочного процесса"
        v-model="searchQuery"
        search-placeholder="Поиск тренировки..."
      >
        <template #actions>
          <BaseButton v-if="canWrite" @click="openCreate()">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать тренировку
          </BaseButton>
        </template>
      </PageHeader>

      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-btn"
          :class="{ active: activeTab === tab.id }"
          @click="switchTab(tab.id)"
        >
          {{ tab.label }}
        </button>
        <div class="filters">
          <select v-model="filters.groupId" class="filter-select" aria-label="Группа">
            <option value="">Все группы</option>
            <option v-for="g in activeGroups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <select v-model="filters.coachId" class="filter-select" aria-label="Тренер">
            <option value="">Все тренеры</option>
            <option v-for="c in coachOptions" :key="c.userId" :value="c.userId">{{ c.fullName }}</option>
          </select>
          <label class="period">с <input v-model="filters.from" class="filter-date" type="date" /></label>
          <label class="period">по <input v-model="filters.to" class="filter-date" type="date" /></label>
        </div>
      </div>

      <p v-if="referenceError" class="notice">Справочники не загрузились: {{ errorText(referenceError) }}</p>

      <StateBlock v-if="loading" kind="loading" />
      <StateBlock v-else-if="error" kind="error" :message="errorText(error)" />

      <div v-else class="table-card">
        <div class="table-header">
          <div class="col date">ДАТА / ВРЕМЯ</div>
          <div class="col group">ТРЕНИРОВКА / ГРУППА</div>
          <div class="col type">ТИП</div>
          <div class="col place">МЕСТО</div>
          <div class="col status">СТАТУС</div>
          <div class="col attendance">ПОСЕЩАЕМОСТЬ</div>
          <div class="col actions"></div>
        </div>

        <div
          v-for="train in trainings"
          :key="train.id"
          class="table-row"
          @click="goToReport(train.id)"
        >
          <div class="col date">
            <div>{{ formatDate(train.startsAt, { withYear: false }) }}</div>
            <div class="cell-sub">{{ timeRange(train) }}</div>
          </div>
          <div class="col group">
            <div>{{ train.title }}</div>
            <div class="cell-sub">{{ groupName(train.groupId) }}</div>
          </div>
          <div class="col type">{{ typeName(train.typeId) }}</div>
          <div class="col place">{{ venueName(train.venueId) }}</div>
          <div class="col status">
            <span
              class="status-badge"
              :class="`status-${tone(train.status)}`"
              :title="train.cancelReason ? `Причина отмены: ${train.cancelReason}` : ''"
            >
              {{ label('trainingStatus', train.status) }}
            </span>
          </div>
          <div class="col attendance">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: attendancePercent(train) + '%' }"></div>
            </div>
            <span>{{ attendanceText(train) }}</span>
          </div>
          <div class="col actions" @click.stop>
            <button class="action-btn" @click="goToReport(train.id)">Журнал</button>
            <button v-if="canEdit(train)" class="action-btn outline" @click="openEdit(train)">Изменить</button>
            <button v-else class="action-btn outline" @click="openPlan(train)">План</button>
          </div>
        </div>

        <StateBlock v-if="trainings.length === 0" kind="empty" message="Тренировок не найдено" />
        <p v-if="moreError" class="notice">{{ moreError }}</p>
        <button v-if="hasMore" class="load-more" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? 'Загрузка…' : 'Показать ещё' }}
        </button>
      </div>

      <!-- Создание и изменение -->
      <BaseModal
        v-model="formOpen"
        :title="editing ? 'Изменить тренировку' : 'Создать тренировку'"
        :submit-label="saving ? 'Сохранение…' : (editing ? 'Сохранить' : 'Создать')"
        :width="640"
        @submit="submitForm"
      >
        <BaseInput id="training-title" v-model="form.title" label="Название" placeholder="Водная подготовка" />
        <div class="row-2">
          <div class="form-field">
            <label class="form-label" for="training-group">Группа</label>
            <select id="training-group" v-model="form.groupId" class="form-select">
              <option value="" disabled>Выберите группу</option>
              <option v-for="g in formGroups" :key="g.id" :value="g.id">{{ g.name }}</option>
            </select>
          </div>
          <div class="form-field">
            <label class="form-label" for="training-type">Тип тренировки</label>
            <select id="training-type" v-model="form.typeId" class="form-select">
              <option value="" disabled>Выберите тип</option>
              <option v-for="t in formTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
            </select>
          </div>
        </div>
        <div class="row-2">
          <BaseInput id="training-starts" v-model="form.startsAt" type="datetime-local" label="Начало" />
          <BaseInput id="training-ends" v-model="form.endsAt" type="datetime-local" label="Конец" />
        </div>
        <div class="form-field">
          <label class="form-label" for="training-venue">Площадка</label>
          <select id="training-venue" v-model="form.venueId" class="form-select">
            <option value="" disabled>Выберите площадку</option>
            <option v-for="v in formVenues" :key="v.id" :value="v.id">{{ v.name }}</option>
          </select>
        </div>
        <div class="form-field">
          <span class="form-label">Тренеры</span>
          <div class="coach-list">
            <label v-for="c in formCoaches" :key="c.userId" class="coach-option">
              <input v-model="form.coachIds" type="checkbox" :value="c.userId" />
              {{ c.fullName }}
            </label>
            <span v-if="!formCoaches.length" class="form-hint">Список тренеров недоступен</span>
          </div>
        </div>
        <div class="form-field">
          <div class="plan-head">
            <span class="form-label">План тренировки</span>
            <span class="form-hint">{{ planMinutes }} из {{ durationMinutes > 0 ? durationMinutes : '—' }} мин</span>
          </div>
          <div v-for="(stage, i) in form.plan" :key="stage.key" class="plan-stage">
            <input v-model="stage.title" class="form-control" type="text" placeholder="Этап" aria-label="Название этапа" />
            <input v-model="stage.durationMinutes" class="form-control" type="number" min="1" step="1" placeholder="мин" aria-label="Длительность, минут" />
            <input v-model="stage.description" class="form-control" type="text" placeholder="Описание (необязательно)" aria-label="Описание этапа" />
            <button type="button" class="stage-remove" title="Удалить этап" @click="removeStage(i)">
              <BaseIcon name="close" :size="12" color="#6D7D79" />
            </button>
          </div>
          <button type="button" class="action-btn add-stage" @click="addStage">+ Этап</button>
        </div>
        <div class="form-field">
          <label class="form-label" for="training-comment">Комментарий</label>
          <textarea id="training-comment" v-model="form.comment" class="form-textarea" rows="2"></textarea>
        </div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
        <button v-if="editing" type="button" class="danger-link" @click="cancelFromEdit">Отменить тренировку…</button>
      </BaseModal>

      <!-- План (только чтение) -->
      <BaseModal v-model="planOpen" title="План тренировки" submit-label="Закрыть" @submit="planOpen = false">
        <template v-if="planTarget">
          <p class="modal-text">{{ planTarget.title }} · {{ formatDate(planTarget.startsAt) }}, {{ timeRange(planTarget) }}</p>
          <ol v-if="planTarget.plan && planTarget.plan.length" class="plan-list">
            <li v-for="(stage, i) in planTarget.plan" :key="i">
              <strong>{{ stage.title }}</strong> — {{ stage.durationMinutes }} мин
              <div v-if="stage.description" class="cell-sub">{{ stage.description }}</div>
            </li>
          </ol>
          <StateBlock v-else kind="empty" message="План тренировки не заполнен" />
          <p v-if="planTarget.comment" class="modal-text">Комментарий: {{ planTarget.comment }}</p>
          <p v-if="planTarget.cancelReason" class="modal-text">Причина отмены: {{ planTarget.cancelReason }}</p>
        </template>
      </BaseModal>

      <!-- Отмена -->
      <BaseModal
        v-model="cancelOpen"
        title="Отменить тренировку"
        :submit-label="cancelling ? 'Отмена…' : 'Отменить тренировку'"
        @submit="submitCancel"
      >
        <p v-if="cancelTarget" class="modal-text">
          {{ cancelTarget.title }} · {{ formatDate(cancelTarget.startsAt) }}, {{ timeRange(cancelTarget) }}
        </p>
        <div class="form-field">
          <label class="form-label" for="cancel-reason">Причина отмены</label>
          <textarea
            id="cancel-reason"
            v-model="cancelReason"
            class="form-textarea"
            rows="3"
            placeholder="Например: бассейн закрыт на обслуживание"
          ></textarea>
        </div>
        <p class="form-hint">Отменённую тренировку нельзя вернуть или изменить.</p>
        <p v-if="cancelError" class="form-error">{{ cancelError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { trainingsApi } from '../../api/trainings'
import { attendanceApi } from '../../api/attendance'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { organizationsApi } from '../../api/organizations'
import { getOrganizationId, hasPermission, hasRole, currentUser } from '../../utils/session'
import { parseDate, formatDate, formatTime, toIsoDate, toIsoDateTime, addDays, errorText } from '../../utils/format'
import { label, tone } from '../../utils/labels'

const PAGE_SIZE = 50

const router = useRouter()
const route = useRoute()

const tabs = [
  { id: 'all', label: 'Все', status: null },
  { id: 'planned', label: 'Запланированные', status: 'PLANNED' },
  { id: 'done', label: 'Проведённые', status: 'COMPLETED' },
  { id: 'cancelled', label: 'Отменённые', status: 'CANCELLED' }
]
const activeTab = ref('all')
const searchQuery = ref('')
// Период списка: GET /trainings требует from/to; по умолчанию — две недели назад и месяц вперёд
const filters = reactive({
  groupId: '',
  coachId: '',
  from: toIsoDate(addDays(new Date(), -14)),
  to: toIsoDate(addDays(new Date(), 30))
})

const trainings = ref([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const error = ref(null)
const moreError = ref('')
// trainingId → { present, total } по журналу посещаемости
const attendanceStats = ref({})

const groups = ref([])
const venues = ref([])
const trainingTypes = ref([])
const coaches = ref([])
const referenceError = ref(null)

const canWrite = computed(() => hasRole('TRAINER') && hasPermission('schedule.write'))
const canEdit = t => canWrite.value && t.status === 'PLANNED'
const hasMore = computed(() => page.value + 1 < totalPages.value)

// ─────────── справочники ───────────

// Все страницы списка Page<T> (size не больше 100)
async function fetchAll(request, maxPages = 10) {
  const items = []
  for (let p = 0; p < maxPages; p++) {
    const res = await request({ page: p, size: 100 })
    items.push(...(res?.items || []))
    if (p + 1 >= (res?.totalPages || 0)) break
  }
  return items
}

async function loadReference() {
  const org = getOrganizationId()
  const [g, v, t, c] = await Promise.allSettled([
    fetchAll(p => groupsApi.list(org, p)),
    fetchAll(p => dictionariesApi.list(org, 'venues', p)),
    fetchAll(p => dictionariesApi.list(org, 'training-types', p)),
    hasPermission('members.read')
      ? fetchAll(p => organizationsApi.members(org, { role: 'TRAINER', ...p }))
      : Promise.resolve([])
  ])
  groups.value = g.status === 'fulfilled' ? g.value : []
  venues.value = v.status === 'fulfilled' ? v.value : []
  trainingTypes.value = t.status === 'fulfilled' ? t.value : []
  coaches.value = c.status === 'fulfilled' ? c.value : []
  referenceError.value = [g, v, t].find(r => r.status === 'rejected')?.reason || null
}

const toMap = list => new Map(list.map(item => [item.id, item]))
const groupMap = computed(() => toMap(groups.value))
const venueMap = computed(() => toMap(venues.value))
const typeMap = computed(() => toMap(trainingTypes.value))
const groupName = id => groupMap.value.get(id)?.name || '—'
const venueName = id => venueMap.value.get(id)?.name || '—'
const typeName = id => typeMap.value.get(id)?.name || '—'

const byOrder = (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name, 'ru')
const activeOnly = list => list.filter(item => item.status === 'ACTIVE').sort(byOrder)
const activeGroups = computed(() => activeOnly(groups.value))

// В форме — активные записи плюс текущее значение изменяемой тренировки, даже если оно уже в архиве
function withCurrent(list, map, id) {
  if (!id || list.some(item => item.id === id)) return list
  const current = map.get(id)
  return current ? [...list, current] : list
}
const formGroups = computed(() => withCurrent(activeGroups.value, groupMap.value, form.groupId))
const formVenues = computed(() => withCurrent(activeOnly(venues.value), venueMap.value, form.venueId))
const formTypes = computed(() => withCurrent(activeOnly(trainingTypes.value), typeMap.value, form.typeId))

const coachOptions = computed(() => {
  const list = coaches.value
    .filter(c => c.status === 'ACTIVE')
    .sort((a, b) => (a.fullName || '').localeCompare(b.fullName || '', 'ru'))
  if (list.length) return list
  // Без права members.read список тренеров не получить — предлагаем себя
  const me = currentUser.value
  return me && hasRole('TRAINER') ? [{ userId: me.userId, fullName: me.fullName || 'Вы' }] : []
})
const formCoaches = computed(() => {
  const known = new Set(coachOptions.value.map(c => c.userId))
  const extra = form.coachIds
    .filter(id => !known.has(id))
    .map(id => ({ userId: id, fullName: coaches.value.find(c => c.userId === id)?.fullName || 'Тренер вне списка активных' }))
  return [...coachOptions.value, ...extra]
})

// ─────────── список ───────────

let loadSeq = 0

function listParams(pageNumber) {
  return {
    from: toIsoDateTime(parseDate(filters.from)),
    to: toIsoDateTime(addDays(filters.to, 1)), // «по» включительно
    q: searchQuery.value.trim() || undefined,
    status: tabs.find(t => t.id === activeTab.value)?.status || undefined,
    groupId: filters.groupId || undefined,
    coachId: filters.coachId || undefined,
    page: pageNumber,
    size: PAGE_SIZE
  }
}

async function load() {
  const seq = ++loadSeq
  error.value = null
  moreError.value = ''
  if (!filters.from || !filters.to || filters.from > filters.to) {
    trainings.value = []
    totalPages.value = 0
    error.value = { message: 'Укажите период: дата «с» должна быть не позже даты «по».' }
    return
  }
  loading.value = true
  try {
    const res = await trainingsApi.list(getOrganizationId(), listParams(0))
    if (seq !== loadSeq) return
    trainings.value = res.items || []
    page.value = res.page ?? 0
    totalPages.value = res.totalPages ?? 0
    loadAttendance(seq)
  } catch (e) {
    if (seq === loadSeq) {
      error.value = e
      trainings.value = []
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  const seq = loadSeq
  loadingMore.value = true
  moreError.value = ''
  try {
    const res = await trainingsApi.list(getOrganizationId(), listParams(page.value + 1))
    if (seq !== loadSeq) return
    trainings.value = [...trainings.value, ...(res.items || [])]
    page.value = res.page ?? page.value + 1
    totalPages.value = res.totalPages ?? totalPages.value
    loadAttendance(seq)
  } catch (e) {
    if (seq === loadSeq) moreError.value = errorText(e)
  } finally {
    loadingMore.value = false
  }
}

// Посещаемость прошедших занятий из журнала (GET /attendance, 6.9): «был» из числа участников
async function loadAttendance(seq) {
  const now = Date.now()
  const held = trainings.value.filter(t => t.status !== 'CANCELLED' && new Date(t.endsAt).getTime() <= now)
  if (!held.length || !hasPermission('attendance.read')) {
    attendanceStats.value = {}
    return
  }
  const days = held.map(t => toIsoDate(new Date(t.startsAt))).sort()
  try {
    const items = await fetchAll(p => attendanceApi.list(getOrganizationId(), {
      // даты журнала считаются в часовом поясе организации — берём день запаса с краёв
      from: toIsoDate(addDays(days[0], -1)),
      to: toIsoDate(addDays(days[days.length - 1], 1)),
      groupId: filters.groupId || undefined,
      ...p
    }))
    if (seq !== loadSeq) return
    const stats = {}
    for (const record of items) {
      const s = stats[record.trainingId] || (stats[record.trainingId] = { present: 0, total: 0 })
      s.total++
      if (record.status === 'PRESENT') s.present++
    }
    attendanceStats.value = stats
  } catch (_) {
    // Журнал недоступен (нет права или эндпоинт ещё не выкачен) — в колонке остаётся «—»
    if (seq === loadSeq) attendanceStats.value = {}
  }
}

function attendancePercent(t) {
  const s = attendanceStats.value[t.id]
  return s && s.total ? Math.round((s.present / s.total) * 100) : 0
}
function attendanceText(t) {
  const s = attendanceStats.value[t.id]
  return s && s.total ? `${s.present}/${s.total}` : '—'
}

function switchTab(id) {
  activeTab.value = id
  load()
}

watch(() => [filters.groupId, filters.coachId, filters.from, filters.to], () => load())

let searchTimer = null
watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(load, 300)
})
onBeforeUnmount(() => clearTimeout(searchTimer))

const goToReport = (id) => router.push(`/trainer/trainings/${id}/report`)
const timeRange = t => (t ? `${formatTime(t.startsAt)}–${formatTime(t.endsAt)}` : '')

// ─────────── создание и изменение ───────────

const formOpen = ref(false)
const editing = ref(null)
const saving = ref(false)
const formError = ref('')
const form = reactive(emptyForm())
let stageKey = 0

function emptyForm() {
  return { title: '', groupId: '', typeId: '', venueId: '', coachIds: [], startsAt: '', endsAt: '', plan: [], comment: '' }
}

// ISO-время → значение datetime-local по местному времени
function toLocalInput(iso) {
  const d = new Date(iso)
  if (!iso || isNaN(d)) return ''
  const p = n => String(n).padStart(2, '0')
  return `${toIsoDate(d)}T${p(d.getHours())}:${p(d.getMinutes())}`
}

function openCreate(date) {
  const day = /^\d{4}-\d{2}-\d{2}$/.test(date || '') ? date : ''
  const me = currentUser.value?.userId
  editing.value = null
  formError.value = ''
  Object.assign(form, emptyForm(), {
    startsAt: day ? `${day}T09:00` : '',
    endsAt: day ? `${day}T10:30` : '',
    coachIds: me && coachOptions.value.some(c => c.userId === me) ? [me] : []
  })
  formOpen.value = true
}

function openEdit(t) {
  editing.value = t
  formError.value = ''
  Object.assign(form, emptyForm(), {
    title: t.title || '',
    groupId: t.groupId || '',
    typeId: t.typeId || '',
    venueId: t.venueId || '',
    coachIds: [...(t.coachIds || [])],
    startsAt: toLocalInput(t.startsAt),
    endsAt: toLocalInput(t.endsAt),
    plan: (t.plan || []).map(s => ({
      key: ++stageKey,
      title: s.title || '',
      durationMinutes: String(s.durationMinutes ?? ''),
      description: s.description || ''
    })),
    comment: t.comment || ''
  })
  formOpen.value = true
}

const addStage = () => form.plan.push({ key: ++stageKey, title: '', durationMinutes: '', description: '' })
const removeStage = i => form.plan.splice(i, 1)

const durationMinutes = computed(() => {
  const start = new Date(form.startsAt)
  const end = new Date(form.endsAt)
  return !form.startsAt || !form.endsAt || isNaN(start) || isNaN(end) ? 0 : Math.round((end - start) / 60000)
})
const planMinutes = computed(() => form.plan.reduce((sum, s) => sum + (Number(s.durationMinutes) || 0), 0))

// Правила TrainingWrite (6.8, №028): обязательные поля, endsAt > startsAt, этапы > 0 и не длиннее занятия
function validateForm() {
  if (!form.title.trim()) return 'Укажите название тренировки.'
  if (!form.groupId) return 'Выберите группу.'
  if (!form.typeId) return 'Выберите тип тренировки.'
  if (!form.venueId) return 'Выберите площадку.'
  if (!form.coachIds.length) return 'Выберите хотя бы одного тренера.'
  if (!form.startsAt || !form.endsAt) return 'Укажите начало и конец тренировки.'
  if (durationMinutes.value <= 0) return 'Конец тренировки должен быть позже начала.'
  for (const stage of form.plan) {
    if (!stage.title.trim()) return 'У каждого этапа плана должно быть название.'
    const minutes = Number(stage.durationMinutes)
    if (!Number.isInteger(minutes) || minutes <= 0) {
      return `Длительность этапа «${stage.title.trim()}» — целое число минут больше нуля.`
    }
  }
  if (planMinutes.value > durationMinutes.value) return 'Этапы плана длиннее самой тренировки.'
  return ''
}

function buildWrite() {
  return {
    title: form.title.trim(),
    groupId: form.groupId,
    coachIds: [...form.coachIds],
    venueId: form.venueId,
    typeId: form.typeId,
    startsAt: toIsoDateTime(new Date(form.startsAt)),
    endsAt: toIsoDateTime(new Date(form.endsAt)),
    plan: form.plan.map(s => ({
      title: s.title.trim(),
      durationMinutes: Number(s.durationMinutes),
      description: s.description.trim() || null
    })),
    comment: form.comment.trim() || null
  }
}

// PATCH принимает только изменённые поля; массивы (coachIds, plan) заменяются целиком
function changedFields(original, write) {
  const same = (a, b) => JSON.stringify(a) === JSON.stringify(b)
  const time = value => new Date(value).getTime()
  const plan = list => (list || []).map(s => ({
    title: s.title,
    durationMinutes: Number(s.durationMinutes),
    description: s.description || null
  }))
  const patch = {}
  if (write.title !== original.title) patch.title = write.title
  if (write.groupId !== original.groupId) patch.groupId = write.groupId
  if (!same([...write.coachIds].sort(), [...(original.coachIds || [])].sort())) patch.coachIds = write.coachIds
  if (write.venueId !== original.venueId) patch.venueId = write.venueId
  if (write.typeId !== original.typeId) patch.typeId = write.typeId
  if (time(write.startsAt) !== time(original.startsAt)) patch.startsAt = write.startsAt
  if (time(write.endsAt) !== time(original.endsAt)) patch.endsAt = write.endsAt
  if (!same(plan(write.plan), plan(original.plan))) patch.plan = write.plan
  if ((write.comment || null) !== (original.comment || null)) patch.comment = write.comment
  return patch
}

async function submitForm() {
  if (saving.value) return
  formError.value = validateForm()
  if (formError.value) return
  const write = buildWrite()
  saving.value = true
  try {
    const org = getOrganizationId()
    if (editing.value) {
      const patch = changedFields(editing.value, write)
      if (Object.keys(patch).length) await trainingsApi.update(org, editing.value.id, patch)
    } else {
      await trainingsApi.create(org, write)
    }
    formOpen.value = false
    await load()
  } catch (e) {
    formError.value = errorText(e)
  } finally {
    saving.value = false
  }
}

// ─────────── план (просмотр) ───────────

const planOpen = ref(false)
const planTarget = ref(null)
function openPlan(t) {
  planTarget.value = t
  planOpen.value = true
}

// ─────────── отмена ───────────

const cancelOpen = ref(false)
const cancelTarget = ref(null)
const cancelReason = ref('')
const cancelError = ref('')
const cancelling = ref(false)

function openCancel(t) {
  cancelTarget.value = t
  cancelReason.value = ''
  cancelError.value = ''
  cancelOpen.value = true
}

// Отмена открывается из формы изменения запланированной тренировки
function cancelFromEdit() {
  const t = editing.value
  formOpen.value = false
  if (t) openCancel(t)
}

// Отмена — отдельный PATCH {status: CANCELLED, cancelReason}; причина обязательна
async function submitCancel() {
  if (cancelling.value || !cancelTarget.value) return
  const reason = cancelReason.value.trim()
  if (!reason) {
    cancelError.value = 'Укажите причину отмены.'
    return
  }
  cancelling.value = true
  cancelError.value = ''
  try {
    await trainingsApi.cancel(getOrganizationId(), cancelTarget.value.id, reason)
    cancelOpen.value = false
    await load()
  } catch (e) {
    cancelError.value = errorText(e)
  } finally {
    cancelling.value = false
  }
}

onMounted(async () => {
  loadReference()
  await load()
  // Переход из расписания: «Добавить событие» → форма с датой выбранного дня
  if (route.query.create === '1') {
    if (canWrite.value) openCreate(route.query.date)
    router.replace({ query: {} })
  }
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.tabs { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.tab-btn { padding: 8px 16px; border-radius: 8px; background: white; border: 1px solid #E3EAE8; font-size: 13px; font-weight: 600; color: #6D7D79; cursor: pointer; }
.tab-btn.active { background: #102522; color: white; border-color: #102522; }
.filters { margin-left: auto; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.filter-select, .filter-date { padding: 8px 12px; background: white; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 13px; font-weight: 600; color: #6D7D79; outline: none; font-family: inherit; }
.filter-select { cursor: pointer; }
.period { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #6D7D79; }
.notice { font-size: 13px; color: #D64545; }
.table-card { background: white; border: 1px solid #E3EAE8; border-radius: 20px; padding: 24px; display: flex; flex-direction: column; gap: 16px; overflow-x: auto; }
.table-header, .table-row { display: flex; align-items: center; gap: 16px; min-width: 1000px; }
.table-header { padding-bottom: 12px; border-bottom: 1px solid #E3EAE8; }
.table-row { padding: 14px 0; border-bottom: 1px solid #E3EAE8; cursor: pointer; transition: background .2s; }
.table-row:hover { background: #F9FBFA; }
.col { font-size: 14px; color: #152421; min-width: 0; }
.col.date { width: 160px; font-weight: 700; }
.col.group { width: 180px; font-weight: 600; }
.col.type { width: 140px; color: #6D7D79; }
.col.place { width: 160px; color: #6D7D79; }
.col.status { width: 140px; }
.col.attendance { width: 160px; display: flex; align-items: center; gap: 8px; }
/* одинаковая ширина колонки действий во всех строках — колонки таблицы не «гуляют» */
.col.actions { flex: 1 0 150px; display: flex; justify-content: flex-end; gap: 8px; }
.table-header .col { font-size: 12px; font-weight: 700; color: #6D7D79; text-transform: uppercase; }
.cell-sub { font-size: 12px; font-weight: 500; color: #6D7D79; margin-top: 2px; }
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-blue { background: #DDECFB; color: #35678E; }
.status-red { background: #FCE2E5; color: #D64545; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.status-gray { background: #F4F7F8; color: #888888; }
.progress-bar { width: 60px; height: 6px; background: #F4F7F8; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: #2E8B57; }
.action-btn { padding: 4px 8px; background: #F4F7F8; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; color: #152421; cursor: pointer; }
.action-btn.outline { background: white; border: 1px solid #E3EAE8; }
.danger-link { align-self: flex-start; padding: 0; background: none; border: none; font-size: 13px; font-weight: 600; color: #D64545; cursor: pointer; }
.load-more { align-self: center; padding: 8px 16px; background: white; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 13px; font-weight: 600; color: #152421; cursor: pointer; }
.load-more:disabled { opacity: .6; cursor: default; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

/* Форма тренировки */
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.form-select, .form-control, .form-textarea { width: 100%; padding: 8px 12px; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 14px; color: #152421; background: white; outline: none; font-family: inherit; }
.form-select, .form-control { min-height: 40px; }
.form-select { cursor: pointer; }
.form-textarea { resize: vertical; }
.form-select:focus, .form-control:focus, .form-textarea:focus { border-color: #B7F34B; }
.form-hint { font-size: 12px; color: #98A6A2; }
.form-error { font-size: 13px; color: #D64545; }
.coach-list { display: flex; flex-wrap: wrap; gap: 8px 16px; }
.coach-option { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #152421; cursor: pointer; }
.plan-head { display: flex; justify-content: space-between; align-items: center; }
.plan-stage { display: grid; grid-template-columns: 1fr 80px 1.4fr 28px; gap: 8px; align-items: center; }
.stage-remove { width: 28px; height: 28px; display: flex; justify-content: center; align-items: center; background: #F4F7F8; border: none; border-radius: 8px; cursor: pointer; }
.add-stage { align-self: flex-start; }
.modal-text { font-size: 14px; color: #152421; }
.plan-list { display: flex; flex-direction: column; gap: 8px; padding-left: 20px; font-size: 14px; color: #152421; }
</style>
