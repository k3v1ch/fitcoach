<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Сборы и мероприятия"
        subtitle="Календарь спортивных выездов, аттестаций и сборов"
        v-model="searchQuery"
        search-placeholder="Поиск мероприятия..."
      >
        <template #actions>
          <BaseButton v-if="canWrite" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать мероприятие
          </BaseButton>
        </template>
      </PageHeader>

      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-btn"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          {{ tab.name }}
        </button>
        <select v-model="statusFilter" class="filter-select" aria-label="Статус мероприятия">
          <option value="">Все статусы</option>
          <option v-for="o in statusOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
      </div>

      <div v-if="loading" class="empty-state">
        <StateBlock kind="loading" />
      </div>
      <div v-else-if="loadError" class="empty-state">
        <StateBlock kind="error" :message="loadError" />
      </div>
      <div v-else-if="events.length === 0" class="empty-state">
        <StateBlock kind="empty" :message="emptyMessage" />
      </div>

      <div v-else class="events-grid">
        <div v-for="event in events" :key="event.id" class="event-card">
          <div class="event-header">
            <div class="event-badges">
              <span class="event-type" :class="TYPE_CLASS[event.type] || 'type-blue'">{{ label('eventType', event.type) }}</span>
              <span class="event-type" :class="`status-${tone(event.status)}`">{{ label('eventStatus', event.status) }}</span>
            </div>
            <span class="event-date">{{ dateText(event) }}</span>
          </div>
          <h3>{{ event.title }}</h3>
          <p class="event-participants">{{ participantsText(event) }}</p>
          <div class="divider"></div>
          <div class="event-footer">
            <div>
              <span class="footer-label">ФИНАНСОВЫЙ СТАТУС</span>
              <span class="footer-value">{{ financeText(event) }}</span>
            </div>
            <button class="action-btn" @click="openManage(event)">{{ canWrite ? 'Управление' : 'Подробнее' }}</button>
          </div>
        </div>
      </div>

      <!-- Создание и изменение черновика -->
      <BaseModal
        v-model="showForm"
        :title="editingId ? 'Изменить мероприятие' : 'Создать мероприятие'"
        :submit-label="saving ? 'Сохранение…' : (editingId ? 'Сохранить' : 'Создать')"
        :width="560"
        @submit="handleSubmit"
      >
        <BaseInput id="event-title" v-model="form.title" label="Наименование" placeholder="Летние сборы" />
        <div class="row-2">
          <div class="field">
            <label class="field-label" for="event-type">Тип</label>
            <select id="event-type" v-model="form.type" class="field-control" :disabled="!!editingId">
              <option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </div>
          <div class="field">
            <label class="field-label" for="event-section">Секция</label>
            <select id="event-section" v-model="form.sectionId" class="field-control" :disabled="!!editingId">
              <option value="" disabled>Выберите секцию</option>
              <option v-for="s in formSections" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
        </div>
        <template v-if="form.type === 'FUNDRAISER'">
          <div class="row-2">
            <BaseInput id="event-target" v-model="form.targetAmount" label="Цель сбора, ₽" placeholder="50 000" />
            <BaseInput id="event-due" v-model="form.collectionDueOn" type="date" label="Собрать до" />
          </div>
        </template>
        <template v-else>
          <div class="row-2">
            <BaseInput id="event-from" v-model="form.startsAt" type="datetime-local" label="Дата С" />
            <BaseInput id="event-to" v-model="form.endsAt" type="datetime-local" label="До" />
          </div>
          <BaseInput id="event-location" v-model="form.location" label="Место" placeholder="Сочи, спорткомплекс «Юг»" />
        </template>
        <div class="row-2">
          <BaseInput id="event-cost" v-model="form.costPerAthlete" label="Взнос с участника, ₽" placeholder="Необязательно" />
          <BaseInput id="event-deadline" v-model="form.responseDeadline" type="datetime-local" label="Ответить до" />
        </div>
        <div class="field">
          <span class="field-label">Обязательные документы</span>
          <div class="check-row">
            <label v-for="o in documentOptions" :key="o.value" class="check-item">
              <input v-model="form.requiredDocumentTypes" type="checkbox" :value="o.value" />
              {{ o.label }}
            </label>
          </div>
        </div>
        <div class="field">
          <label class="field-label" for="event-description">Описание</label>
          <textarea id="event-description" v-model="form.description" class="field-control" rows="3" placeholder="Необязательно"></textarea>
        </div>
        <p v-if="sectionsError && !editingId" class="form-error">Не удалось загрузить секции: {{ sectionsError }}</p>
        <p v-else-if="!activeSections.length && !editingId" class="form-hint">
          Сначала создайте секцию на странице
          <router-link to="/trainer/sections">«Секции»</router-link>.
        </p>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>

      <!-- Управление: сведения, статус, участники -->
      <BaseModal
        v-model="showManage"
        :title="managed ? managed.title : ''"
        submit-label="Закрыть"
        :width="640"
        @submit="showManage = false"
      >
        <template v-if="managed">
          <div class="manage-meta">
            <div class="meta-row">
              <span class="meta-label">Тип и статус</span>
              <span class="meta-value">
                {{ label('eventType', managed.type) }}
                <span class="event-type" :class="`status-${tone(managed.status)}`">{{ label('eventStatus', managed.status) }}</span>
              </span>
            </div>
            <div class="meta-row">
              <span class="meta-label">{{ managed.type === 'FUNDRAISER' ? 'Срок сбора' : 'Когда' }}</span>
              <span class="meta-value">{{ dateText(managed) }}</span>
            </div>
            <div v-if="managed.location" class="meta-row">
              <span class="meta-label">Место</span>
              <span class="meta-value">{{ managed.location }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">Секция</span>
              <span class="meta-value">{{ sectionName(managed.sectionId) }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">Финансы</span>
              <span class="meta-value">{{ financeText(managed) }}</span>
            </div>
            <div v-if="managed.responseDeadline" class="meta-row">
              <span class="meta-label">Ответить до</span>
              <span class="meta-value">{{ formatDateTime(managed.responseDeadline) }}</span>
            </div>
            <div v-if="(managed.requiredDocumentTypes || []).length" class="meta-row">
              <span class="meta-label">Документы</span>
              <span class="meta-value">{{ documentsText(managed) }}</span>
            </div>
            <p v-if="managed.description" class="manage-description">{{ managed.description }}</p>
          </div>

          <div v-if="canWrite && isOpen(managed)" class="manage-actions">
            <button v-if="managed.status === 'DRAFT'" class="action-btn" :disabled="manageBusy" @click="openEdit(managed)">Редактировать</button>
            <button v-if="managed.status === 'DRAFT'" class="action-btn action-btn--primary" :disabled="manageBusy" @click="changeStatus('PUBLISHED')">Опубликовать</button>
            <button v-if="managed.status === 'PUBLISHED'" class="action-btn action-btn--primary" :disabled="manageBusy" @click="changeStatus('COMPLETED')">Завершить</button>
            <button class="action-btn action-btn--danger" :disabled="manageBusy" @click="changeStatus('CANCELLED')">Отменить мероприятие</button>
          </div>
          <p v-if="canWrite && managed.status === 'DRAFT'" class="form-hint">
            Черновик видите только вы. После публикации добавленные участники и их родители получат приглашение и смогут ответить.
          </p>
          <p v-if="manageError" class="form-error">{{ manageError }}</p>

          <h4 class="manage-subtitle">Участники{{ participantsLoading ? '' : ` (${participants.length})` }}</h4>
          <StateBlock v-if="participantsLoading" kind="loading" />
          <StateBlock v-else-if="participantsError" kind="error" :message="participantsError" />
          <StateBlock v-else-if="!participants.length" kind="empty" message="Участников пока нет" />
          <ul v-else class="participant-list">
            <li v-for="p in participants" :key="p.athleteId" class="participant-item">
              <div class="participant-copy">
                <router-link :to="`/trainer/athletes/${p.athleteId}`" class="participant-name">{{ p.athleteName }}</router-link>
                <span v-if="p.comment" class="participant-comment">{{ p.comment }}</span>
              </div>
              <span class="event-type" :class="`status-${tone(p.response)}`">{{ label('participantResponse', p.response) }}</span>
              <button v-if="canEditParticipants" class="link-btn" :disabled="manageBusy" @click="removeParticipant(p)">Исключить</button>
            </li>
          </ul>

          <div v-if="canEditParticipants" class="add-participants">
            <span class="field-label">Добавить участников</span>
            <div class="row-2">
              <select v-model="addGroupId" class="field-control" aria-label="Выбрать спортсменов группы">
                <option value="">Отметить всю группу…</option>
                <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
              </select>
              <input v-model="athleteFilter" class="field-control" type="text" placeholder="Поиск спортсмена" />
            </div>
            <StateBlock v-if="athletesError" kind="error" :message="athletesError" />
            <StateBlock v-else-if="!athletesLoaded" kind="loading" />
            <p v-else-if="!candidateAthletes.length" class="form-hint">
              {{ athleteFilter.trim() ? 'Никого не найдено.' : 'Все активные спортсмены уже участвуют.' }}
            </p>
            <div v-else class="check-list">
              <label v-for="a in candidateAthletes" :key="a.id" class="check-item">
                <input v-model="selectedAthletes" type="checkbox" :value="a.id" />
                {{ fullName(a) }}
              </label>
            </div>
            <div class="add-footer">
              <BaseButton :loading="adding" :disabled="!selectedAthletes.length" @click="addSelected">
                Добавить выбранных{{ selectedAthletes.length ? ` (${selectedAthletes.length})` : '' }}
              </BaseButton>
            </div>
            <p v-if="addError" class="form-error">{{ addError }}</p>
          </div>
        </template>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { api } from '../../api/index'
import { eventsApi } from '../../api/events'
import { sectionsApi } from '../../api/sections'
import { athletesApi } from '../../api/athletes'
import { groupsApi } from '../../api/groups'
import { getOrganizationId, hasPermission, hasRole } from '../../utils/session'
import {
  errorText, formatDate, formatTime, formatDateTime, formatMoney, toMoney, toIsoDate, toIsoDateTime, parseDate, fullName
} from '../../utils/format'
import { label, options, tone } from '../../utils/labels'

const ALL = 'ALL'
const TYPES = ['CAMP', 'COMPETITION', 'MEDICAL_EXAM', 'FUNDRAISER']
const TYPE_CLASS = { CAMP: 'type-green', COMPETITION: 'type-yellow', MEDICAL_EXAM: 'type-blue', FUNDRAISER: 'type-red' }
const OPEN_STATUSES = ['DRAFT', 'PUBLISHED']
const FIELD_LABELS = {
  title: 'Наименование', type: 'Тип', sectionId: 'Секция', startsAt: 'Дата начала', endsAt: 'Дата окончания',
  location: 'Место', costPerAthlete: 'Взнос', targetAmount: 'Цель сбора', collectionDueOn: 'Срок сбора',
  responseDeadline: 'Срок ответа', requiredDocumentTypes: 'Документы', description: 'Описание', status: 'Статус'
}

const tabs = [
  { id: ALL, name: 'Все события' },
  { id: 'CAMP', name: 'Сборы' },
  { id: 'COMPETITION', name: 'Соревнования' },
  { id: 'MEDICAL_EXAM', name: 'Медицинские осмотры' },
  { id: 'FUNDRAISER', name: 'Целевые сборы' }
]
const statusOptions = options('eventStatus')
const typeOptions = options('eventType')
const documentOptions = options('documentType')

const route = useRoute()
const router = useRouter()
// Запись — только тренер с events.write (сервер проверяет и роль, и право)
const canWrite = computed(() => hasRole('TRAINER') && hasPermission('events.write'))

// ─────────── Список: поиск (q), вкладка (type), статус — параметры сервера ───────────
const searchQuery = ref('')
const activeTab = ref(ALL)
const statusFilter = ref('')
const events = ref([])
const loading = ref(true)
const loadError = ref('')

let loadSeq = 0
async function loadEvents() {
  const seq = ++loadSeq
  loading.value = true
  loadError.value = ''
  try {
    const items = await fetchAll(params => eventsApi.list(getOrganizationId(), params), {
      q: searchQuery.value.trim(),
      type: activeTab.value === ALL ? null : activeTab.value,
      status: statusFilter.value || null
    })
    if (seq !== loadSeq) return
    events.value = items
    loadCounts(items)
  } catch (e) {
    if (seq === loadSeq) {
      events.value = []
      loadError.value = errorText(e)
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

const emptyMessage = computed(() => {
  const q = searchQuery.value.trim()
  if (q) return `Ничего не найдено по запросу «${q}»`
  if (activeTab.value !== ALL || statusFilter.value) return 'Мероприятий с такими условиями нет'
  return canWrite.value ? 'Мероприятий пока нет — создайте первое' : 'Мероприятий пока нет'
})

// Число участников: в Event его нет — берём из списка участников каждого мероприятия (по 6 запросов)
const participantCounts = reactive({})

async function loadCounts(list) {
  const ids = list.map(e => e.id).filter(id => !(id in participantCounts))
  for (let i = 0; i < ids.length; i += 6) {
    await Promise.all(ids.slice(i, i + 6).map(async id => {
      try {
        const items = await eventsApi.participants(getOrganizationId(), id)
        participantCounts[id] = Array.isArray(items) ? items.length : 0
      } catch (_) {
        participantCounts[id] = null
      }
    }))
  }
}

function participantsText(event) {
  const n = participantCounts[event.id]
  let who
  if (n === undefined) who = 'Участники загружаются…'
  else if (n === null) who = 'Участники: нет данных'
  else if (n === 0) who = 'Участников пока нет'
  else who = `${n} ${plural(n, 'участник', 'участника', 'участников')}`
  return event.location ? `${who} · ${event.location}` : who
}

// «12 – 25 октября 2026 г.», «5 октября 2026 г., 10:00–14:00»; денежный сбор — «Сбор до …»
function dateText(event) {
  if (event.type === 'FUNDRAISER') {
    return event.collectionDueOn ? `Сбор до ${formatDate(event.collectionDueOn)}` : '—'
  }
  const s = parseDate(event.startsAt)
  const e = parseDate(event.endsAt)
  if (!s || isNaN(s)) return '—'
  if (!e || isNaN(e)) return formatDate(s)
  if (toIsoDate(s) === toIsoDate(e)) return `${formatDate(s)}, ${formatTime(s)}–${formatTime(e)}`
  if (s.getFullYear() === e.getFullYear()) {
    if (s.getMonth() === e.getMonth()) return `${s.getDate()} – ${formatDate(e)}`
    return `${formatDate(s, { withYear: false })} – ${formatDate(e)}`
  }
  return `${formatDate(s)} – ${formatDate(e)}`
}

// Финансовые итоги мероприятия API пока не отдаёт — показываем условия: цель сбора или взнос
function financeText(event) {
  const cost = Number(event.costPerAthlete) > 0 ? formatMoney(event.costPerAthlete) : null
  if (event.type === 'FUNDRAISER') {
    const target = event.targetAmount != null ? `Цель ${formatMoney(event.targetAmount)}` : 'Цель не указана'
    return cost ? `${target} · по ${cost}` : target
  }
  return cost ? `Взнос ${cost} с участника` : 'Без взноса'
}

function documentsText(event) {
  return (event.requiredDocumentTypes || []).map(t => label('documentType', t)).join(', ')
}

const isOpen = (event) => OPEN_STATUSES.includes(event?.status)

// ─────────── Секции: выбор в форме и подпись в карточке ───────────
const sections = ref([])
const sectionsError = ref('')

async function loadSections() {
  try {
    sections.value = await fetchAll(params => sectionsApi.list(getOrganizationId(), params))
    sectionsError.value = ''
  } catch (e) {
    sections.value = []
    sectionsError.value = errorText(e)
  }
}

const activeSections = computed(() => sections.value.filter(s => s.status === 'ACTIVE'))
// При изменении черновика секция неизменяема — показываем её, даже если она уже в архиве
const formSections = computed(() => {
  if (!editingId.value) return activeSections.value
  const current = sections.value.find(s => s.id === form.sectionId)
  return current && current.status !== 'ACTIVE' ? [...activeSections.value, current] : activeSections.value
})
const sectionName = (id) => sections.value.find(s => s.id === id)?.name || '—'

// ─────────── Создание и изменение черновика ───────────
const showForm = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formError = ref('')
const form = reactive(emptyForm())

function emptyForm() {
  return {
    title: '', type: 'CAMP', sectionId: '', startsAt: '', endsAt: '', location: '', costPerAthlete: '',
    targetAmount: '', collectionDueOn: '', responseDeadline: '', requiredDocumentTypes: [], description: ''
  }
}

function openCreate() {
  Object.assign(form, emptyForm(), {
    type: activeTab.value !== ALL ? activeTab.value : 'CAMP',
    sectionId: activeSections.value.length === 1 ? activeSections.value[0].id : ''
  })
  editingId.value = null
  formError.value = ''
  showForm.value = true
}

function openEdit(event) {
  Object.assign(form, {
    title: event.title || '',
    type: event.type,
    sectionId: event.sectionId || '',
    startsAt: toLocalInput(event.startsAt),
    endsAt: toLocalInput(event.endsAt),
    location: event.location || '',
    costPerAthlete: event.costPerAthlete != null ? String(event.costPerAthlete) : '',
    targetAmount: event.targetAmount != null ? String(event.targetAmount) : '',
    collectionDueOn: event.collectionDueOn || '',
    responseDeadline: toLocalInput(event.responseDeadline),
    requiredDocumentTypes: [...(event.requiredDocumentTypes || [])],
    description: event.description || ''
  })
  editingId.value = event.id
  formError.value = ''
  showManage.value = false
  showForm.value = true
}

// Правила API (№041): обычному мероприятию нужны время и место, денежному сбору — цель и срок
function validateForm() {
  if (!form.title.trim()) return 'Укажите название мероприятия.'
  if (!editingId.value) {
    if (!TYPES.includes(form.type)) return 'Выберите тип мероприятия.'
    if (!form.sectionId) return 'Выберите секцию.'
  }
  const fundraiser = form.type === 'FUNDRAISER'
  if (fundraiser) {
    const target = form.targetAmount.trim() ? toMoney(form.targetAmount) : null
    if (target === null || Number(target) <= 0) return 'Укажите цель сбора — сумму больше нуля.'
    if (!form.collectionDueOn) return 'Укажите срок сбора.'
  } else {
    const starts = localInputDate(form.startsAt)
    const ends = localInputDate(form.endsAt)
    if (!starts) return 'Укажите дату и время начала.'
    if (!ends) return 'Укажите дату и время окончания.'
    if (starts >= ends) return 'Окончание должно быть позже начала.'
    if (!form.location.trim()) return 'Укажите место проведения.'
  }
  if (form.costPerAthlete.trim()) {
    const cost = toMoney(form.costPerAthlete)
    if (cost === null || Number(cost) <= 0) return 'Взнос должен быть больше нуля — или оставьте поле пустым.'
  }
  if (form.responseDeadline) {
    const deadline = localInputDate(form.responseDeadline)
    if (!deadline) return 'Проверьте срок ответа.'
    const limit = fundraiser ? nextDay(form.collectionDueOn) : localInputDate(form.startsAt)
    if (limit && deadline > limit) {
      return fundraiser ? 'Срок ответа не может быть позже срока сбора.' : 'Срок ответа не может быть позже начала мероприятия.'
    }
  }
  return ''
}

// Тело запроса — только поля EventWrite / EventPatch (лишние поля сервер отклоняет)
function buildBody() {
  const fundraiser = form.type === 'FUNDRAISER'
  const fields = {
    title: form.title.trim(),
    description: form.description.trim() || null,
    startsAt: fundraiser ? null : toIsoDateTime(localInputDate(form.startsAt)),
    endsAt: fundraiser ? null : toIsoDateTime(localInputDate(form.endsAt)),
    location: fundraiser ? null : form.location.trim(),
    costPerAthlete: form.costPerAthlete.trim() ? toMoney(form.costPerAthlete) : null,
    targetAmount: fundraiser ? toMoney(form.targetAmount) : null,
    collectionDueOn: fundraiser ? form.collectionDueOn : null,
    responseDeadline: form.responseDeadline ? toIsoDateTime(localInputDate(form.responseDeadline)) : null,
    requiredDocumentTypes: [...form.requiredDocumentTypes]
  }
  // type и sectionId после создания неизменяемы — в PATCH их не передаём
  return editingId.value ? fields : { ...fields, type: form.type, sectionId: form.sectionId }
}

async function handleSubmit() {
  if (saving.value) return
  const problem = validateForm()
  if (problem) {
    formError.value = problem
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const body = buildBody()
    if (editingId.value) await eventsApi.update(getOrganizationId(), editingId.value, body)
    else await eventsApi.create(getOrganizationId(), body)
    showForm.value = false
    await loadEvents()
  } catch (e) {
    formError.value = formErrorText(e)
  } finally {
    saving.value = false
  }
}

// ─────────── Управление: статус и участники ───────────
const showManage = ref(false)
const managed = ref(null)
const manageBusy = ref(false)
const manageError = ref('')
const participants = ref([])
const participantsLoading = ref(false)
const participantsError = ref('')

const canEditParticipants = computed(() => canWrite.value && isOpen(managed.value))

function openManage(event) {
  managed.value = event
  manageError.value = ''
  addError.value = ''
  selectedAthletes.value = []
  athleteFilter.value = ''
  participants.value = []
  showManage.value = true
  loadParticipants()
  if (canWrite.value && isOpen(event)) {
    loadAthletes()
    loadGroups()
  }
}

let participantsSeq = 0
async function loadParticipants() {
  const id = managed.value?.id
  if (!id) return
  const seq = ++participantsSeq
  participantsLoading.value = true
  participantsError.value = ''
  try {
    const items = await eventsApi.participants(getOrganizationId(), id)
    if (seq !== participantsSeq) return
    participants.value = items || []
    participantCounts[id] = participants.value.length
  } catch (e) {
    if (seq === participantsSeq) {
      participants.value = []
      participantsError.value = errorText(e)
    }
  } finally {
    if (seq === participantsSeq) participantsLoading.value = false
  }
}

// Переходы (№043): DRAFT → PUBLISHED | CANCELLED; PUBLISHED → COMPLETED | CANCELLED
async function changeStatus(status) {
  const event = managed.value
  if (!event || manageBusy.value) return
  const questions = {
    COMPLETED: `Завершить мероприятие «${event.title}»? После завершения его нельзя будет изменить.`,
    CANCELLED: `Отменить мероприятие «${event.title}»?` +
      (event.status === 'PUBLISHED' ? ' Участники получат уведомление об отмене.' : '') +
      ' Начисления и платежи не изменятся.'
  }
  if (questions[status] && !confirm(questions[status])) return
  manageBusy.value = true
  manageError.value = ''
  try {
    managed.value = await eventsApi.update(getOrganizationId(), event.id, { status })
    await loadEvents()
  } catch (e) {
    manageError.value = errorText(e)
  } finally {
    manageBusy.value = false
  }
}

async function removeParticipant(participant) {
  if (manageBusy.value) return
  if (!confirm(`Исключить ${participant.athleteName} из мероприятия? Начисления спортсмена не изменятся.`)) return
  manageBusy.value = true
  manageError.value = ''
  try {
    await eventsApi.removeParticipant(getOrganizationId(), managed.value.id, participant.athleteId)
    await loadParticipants()
  } catch (e) {
    manageError.value = errorText(e)
  } finally {
    manageBusy.value = false
  }
}

// Кандидаты в участники: активные спортсмены организации, которых ещё нет в списке
const athletes = ref([])
const athletesLoaded = ref(false)
const athletesError = ref('')
const groups = ref([])
const athleteFilter = ref('')
const selectedAthletes = ref([])
const addGroupId = ref('')
const adding = ref(false)
const addError = ref('')

async function loadAthletes() {
  if (athletesLoaded.value) return
  try {
    athletes.value = await fetchAll(params => athletesApi.list(getOrganizationId(), params), { status: 'ACTIVE' })
    athletesLoaded.value = true
    athletesError.value = ''
  } catch (e) {
    athletesError.value = errorText(e)
  }
}

async function loadGroups() {
  if (groups.value.length) return
  try {
    groups.value = await fetchAll(params => groupsApi.list(getOrganizationId(), params), { status: 'ACTIVE' })
  } catch (_) {
    groups.value = [] // без groups.read выбор по группе просто недоступен
  }
}

const candidateAthletes = computed(() => {
  const taken = new Set(participants.value.map(p => p.athleteId))
  const q = athleteFilter.value.trim().toLowerCase()
  return athletes.value.filter(a => !taken.has(a.id) && (!q || fullName(a).toLowerCase().includes(q)))
})

// «Отметить всю группу»: текущий состав группы добавляется к выбранным
watch(addGroupId, async (groupId) => {
  if (!groupId) return
  addError.value = ''
  try {
    const detail = await groupsApi.get(getOrganizationId(), groupId)
    const candidates = new Set(candidateAthletes.value.map(a => a.id))
    const ids = (detail?.athletes || []).filter(a => !a.leftOn && candidates.has(a.athleteId)).map(a => a.athleteId)
    selectedAthletes.value = [...new Set([...selectedAthletes.value, ...ids])]
    if (!ids.length) addError.value = 'Все спортсмены этой группы уже участвуют.'
  } catch (e) {
    addError.value = errorText(e)
  } finally {
    addGroupId.value = ''
  }
})

async function addSelected() {
  if (adding.value || !selectedAthletes.value.length || !managed.value) return
  adding.value = true
  addError.value = ''
  try {
    await putParticipants(managed.value.id, [...selectedAthletes.value])
    selectedAthletes.value = []
    await loadParticipants()
  } catch (e) {
    addError.value = errorText(e)
  } finally {
    adding.value = false
  }
}

// PUT …/participants: сервер принимает тело-массив UUID (List<UUID>), а обёртка
// eventsApi.addParticipants шлёт { athleteIds } и получает 400 — поэтому запрос здесь
function putParticipants(eventId, athleteIds) {
  return api.put(`/organizations/${getOrganizationId()}/events/${eventId}/participants`, athleteIds)
}

// ─────────── Жизненный цикл ───────────
let searchTimer = null
watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadEvents, 300)
})
watch([activeTab, statusFilter], loadEvents)
onBeforeUnmount(() => clearTimeout(searchTimer))

onMounted(async () => {
  loadEvents()
  await loadSections()
  // Переход с главной: «Создать событие» → сразу открыть форму
  if (route.query.create === '1') {
    if (canWrite.value) openCreate()
    router.replace({ query: {} })
  }
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

// 'YYYY-MM-DDTHH:mm' из <input type="datetime-local"> → Date по местному времени
function localInputDate(value) {
  if (!value) return null
  const d = new Date(value)
  return isNaN(d) ? null : d
}

// RFC 3339 из API → значение для <input type="datetime-local">
function toLocalInput(value) {
  const d = parseDate(value)
  if (!d || isNaN(d)) return ''
  const p = n => String(n).padStart(2, '0')
  return `${toIsoDate(d)}T${p(d.getHours())}:${p(d.getMinutes())}`
}

// Конец дня 'YYYY-MM-DD' — полночь следующего дня по местному времени
function nextDay(date) {
  const d = parseDate(date)
  return d && !isNaN(d) ? new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1) : null
}

function plural(n, one, few, many) {
  const mod10 = n % 10
  const mod100 = n % 100
  if (mod10 === 1 && mod100 !== 11) return one
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few
  return many
}

// Текст ошибки формы: имена полей из fieldErrors — по-русски
function formErrorText(e) {
  const fieldErrors = (e?.fieldErrors || []).map(f => ({ ...f, field: FIELD_LABELS[f.field] || f.field }))
  return errorText({ status: e?.status, message: e?.message, fieldErrors })
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.tabs { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }
.filter-select {
  margin-left: auto;
  padding: 8px 12px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  font-family: inherit; outline: none; cursor: pointer;
}

.events-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 16px;
}

.event-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.event-header {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
}
.event-badges { display: flex; gap: 6px; flex-wrap: wrap; }
.event-type {
  padding: 4px 10px; border-radius: 99px;
  font-size: 11px; font-weight: 700;
  white-space: nowrap;
}
.type-green { background: #E9F7D5; color: #2E8B57; }
.type-yellow { background: #FFF8E6; color: #F2B705; }
.type-blue { background: #DDECFB; color: #35678E; }
.type-red { background: #FCE2E5; color: #D64545; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.status-red { background: #FCE2E5; color: #D64545; }
.status-blue { background: #DDECFB; color: #3B82F6; }
.status-gray { background: #EEF1F0; color: #888888; }
.event-date { font-size: 13px; color: #6D7D79; text-align: right; }

.event-card h3 { font-size: 18px; font-weight: 700; color: #152421; }
.event-participants { font-size: 14px; color: #6D7D79; }

.divider { height: 1px; background: #E3EAE8; }

.event-footer {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
}
.footer-label {
  display: block; font-size: 11px; font-weight: 700;
  color: #98A6A2; text-transform: uppercase;
}
.footer-value { font-size: 14px; font-weight: 600; color: #152421; }
.action-btn {
  padding: 8px 16px; background: #F4F7F8;
  border: none; border-radius: 8px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
}
.action-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.action-btn--primary { background: #102522; color: #C4F000; }
.action-btn--danger { background: #FCE2E5; color: #D64545; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.empty-state {
  padding: 60px; text-align: center;
  color: #98A6A2; font-size: 14px;
  background: white; border: 1px dashed #E3EAE8;
  border-radius: 16px;
}

/* Поля формы в стиле BaseInput */
.field { display: flex; flex-direction: column; gap: 6px; width: 100%; }
.field-label { font-size: 12px; color: var(--color-gray-text); font-weight: 500; }
.field-control {
  width: 100%; min-height: 40px; padding: 8px 12px;
  border: 1px solid var(--color-gray-border); border-radius: 8px;
  font-size: 14px; font-family: inherit; color: var(--color-dark);
  background: var(--color-white); outline: none;
}
.field-control:focus { border-color: var(--color-primary); }
.field-control:disabled { background: #F4F7F8; color: #6D7D79; }
textarea.field-control { resize: vertical; }
.check-row { display: flex; gap: 16px; flex-wrap: wrap; }
.check-list {
  display: flex; flex-direction: column; gap: 8px;
  max-height: 200px; overflow-y: auto;
  padding: 10px 12px; border: 1px solid var(--color-gray-border); border-radius: 8px;
}
.check-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #152421; cursor: pointer; }
.form-hint { font-size: 13px; color: #6D7D79; line-height: 1.4; }
.form-hint a { color: #35678E; }
.form-error { font-size: 13px; color: #D64545; }

/* Управление мероприятием */
.manage-meta { display: flex; flex-direction: column; gap: 8px; }
.meta-row { display: flex; gap: 12px; font-size: 14px; align-items: baseline; }
.meta-label { width: 120px; flex-shrink: 0; font-size: 12px; font-weight: 700; color: #98A6A2; text-transform: uppercase; }
.meta-value { color: #152421; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.manage-description { font-size: 13px; color: #6D7D79; line-height: 1.5; white-space: pre-line; }
.manage-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.manage-subtitle { font-size: 15px; font-weight: 700; color: #152421; }
.participant-list { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.participant-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; background: #F4F7F8; border-radius: 10px;
}
.participant-copy { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.participant-name { font-size: 14px; font-weight: 600; color: #152421; text-decoration: none; }
.participant-name:hover { text-decoration: underline; }
.participant-comment { font-size: 12px; color: #6D7D79; }
.link-btn {
  background: none; border: none; padding: 0;
  font-size: 12px; font-weight: 600; color: #D64545; cursor: pointer;
}
.link-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.add-participants { display: flex; flex-direction: column; gap: 10px; padding-top: 12px; border-top: 1px solid #E3EAE8; }
.add-footer { display: flex; justify-content: flex-end; }

@media (max-width: 768px) {
  .events-grid { grid-template-columns: 1fr; }
  .filter-select { margin-left: 0; }
}
</style>
