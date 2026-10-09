<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Начисления"
        subtitle="Реестр выставленных начислений спортсменам"
        v-model="searchQuery"
        search-placeholder="Поиск начисления..."
      >
        <template #actions>
          <BaseButton v-if="canWriteCharges" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать начисление
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <div class="filters">
        <select v-model="filters.paymentStatus" class="filter-select">
          <option value="">Любой статус</option>
          <option v-for="o in paymentStatusOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <select v-model="filters.type" class="filter-select">
          <option value="">Все типы</option>
          <option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <select v-model="filters.athleteId" class="filter-select">
          <option value="">Все спортсмены</option>
          <option v-for="a in sortedAthletes" :key="a.id" :value="a.id">{{ fullName(a) }}</option>
        </select>
        <label class="checkbox">
          <input type="checkbox" v-model="filters.isOverdue" />
          Только просроченные
        </label>
        <label class="checkbox">
          <input type="checkbox" v-model="filters.showCancelled" />
          Показывать отменённые
        </label>
      </div>

      <div v-if="loading" class="table-card">
        <StateBlock kind="loading" />
      </div>
      <div v-else-if="loadError" class="table-card">
        <StateBlock kind="error" :message="loadError" />
      </div>

      <div v-else class="table-card">
        <div class="table-header">
          <div class="col num">№</div>
          <div class="col date">СРОК</div>
          <div class="col name">СПОРТСМЕН</div>
          <div class="col title">НАЗНАЧЕНИЕ</div>
          <div class="col amount">СУММА</div>
          <div class="col paid">ОПЛАЧЕНО</div>
          <div class="col status">СТАТУС</div>
          <div class="col actions">ДЕЙСТВИЕ</div>
        </div>

        <div
          v-for="(c, idx) in charges"
          :key="c.id"
          class="table-row"
        >
          <div class="col num">{{ idx + 1 }}</div>
          <div class="col date">{{ formatDateShort(c.dueOn) }}</div>
          <div class="col name">{{ athleteName(c.athleteId) }}</div>
          <div class="col title">
            {{ c.title }}
            <span v-if="isOverdue(c)" class="overdue-tag">Просрочено</span>
          </div>
          <div class="col amount">{{ formatMoney(c.amount) }}</div>
          <div class="col paid">{{ formatMoney(c.paidAmount) }}</div>
          <div class="col status">
            <span class="badge" :class="badgeTone(c)" :title="c.cancelReason || ''">
              {{ badgeLabel(c) }}
            </span>
          </div>
          <div class="col actions">
            <button v-if="canPay(c)" class="action-btn" @click="openPayment(c)">Оплата</button>
            <button v-if="canEdit(c)" class="action-btn outline" @click="openEdit(c)">Изменить</button>
            <button v-if="canCancel(c)" class="action-btn danger" @click="openCancel(c)">Отменить</button>
          </div>
        </div>

        <StateBlock v-if="charges.length === 0" kind="empty" :message="emptyMessage" />

        <div v-if="hasMore || loadMoreError" class="load-more">
          <span v-if="loadMoreError" class="form-error">{{ loadMoreError }}</span>
          <button v-if="hasMore" class="action-btn outline" :disabled="loadingMore" @click="loadMore">
            {{ loadingMore ? 'Загрузка…' : 'Показать ещё' }}
          </button>
        </div>

        <div class="table-footer">
          <div class="footer-total">
            Итого начислено: <strong>{{ formatMoney(totals.amount) }}</strong>
          </div>
          <div class="footer-detail">
            Оплачено: {{ formatMoney(totals.paid) }} · Долг: {{ formatMoney(totals.debt) }}
            <template v-if="hasMore"> · по показанным {{ charges.length }} из {{ totalElements }}</template>
          </div>
        </div>
      </div>

      <!-- Создание начисления -->
      <BaseModal
        v-model="showCreate"
        title="Создать начисление"
        :submit-label="saving ? 'Сохранение…' : 'Создать'"
        @submit="submitCreate"
      >
        <div class="field">
          <label class="field-label" for="charge-athlete">Спортсмен</label>
          <select id="charge-athlete" v-model="createForm.athleteId" class="role-select">
            <option value="" disabled>Выберите спортсмена</option>
            <option v-for="a in activeAthletes" :key="a.id" :value="a.id">{{ fullName(a) }}</option>
          </select>
        </div>
        <div class="field">
          <label class="field-label" for="charge-type">Тип</label>
          <select id="charge-type" v-model="createForm.type" class="role-select">
            <option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <div v-if="createForm.type === 'TRAINING'" class="field">
          <label class="field-label" for="charge-training">Тренировка</label>
          <select
            id="charge-training"
            v-model="createForm.trainingId"
            class="role-select"
            :disabled="!createForm.athleteId || trainingsLoading"
          >
            <option value="" disabled>{{ trainingPlaceholder }}</option>
            <option v-for="t in trainings" :key="t.id" :value="t.id">
              {{ formatDateTime(t.startsAt) }} · {{ t.title }}
            </option>
          </select>
        </div>
        <div v-if="createForm.type === 'EVENT'" class="field">
          <label class="field-label" for="charge-event">Мероприятие или сбор</label>
          <select id="charge-event" v-model="createForm.eventId" class="role-select">
            <option value="" disabled>{{ events.length ? 'Выберите мероприятие' : 'Нет опубликованных мероприятий' }}</option>
            <option v-for="e in events" :key="e.id" :value="e.id">{{ e.title }} · {{ label('eventType', e.type) }}</option>
          </select>
        </div>
        <div class="field">
          <label class="field-label" for="charge-section">Секция</label>
          <select
            id="charge-section"
            v-model="createForm.sectionId"
            class="role-select"
            :disabled="createForm.type !== 'SUBSCRIPTION'"
          >
            <option value="" disabled>
              {{ createForm.type === 'SUBSCRIPTION' ? 'Выберите секцию' : 'Определяется выбором выше' }}
            </option>
            <option v-for="s in sectionOptions" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <BaseInput id="charge-title" v-model="createForm.title" label="Название" placeholder="Абонемент за октябрь" />
        <div class="row-2">
          <BaseInput id="charge-amount" v-model="createForm.amount" label="Сумма, ₽" placeholder="12000" />
          <BaseInput id="charge-due" v-model="createForm.dueOn" type="date" label="Оплатить до" />
        </div>
        <div v-if="createForm.type === 'SUBSCRIPTION'" class="row-2">
          <BaseInput id="charge-period-from" v-model="createForm.periodFrom" type="date" label="Период с" />
          <BaseInput id="charge-period-to" v-model="createForm.periodTo" type="date" label="Период по" />
        </div>
        <BaseInput id="charge-comment" v-model="createForm.comment" label="Комментарий" placeholder="Необязательно" />
        <p v-if="athletesError" class="form-error">Не удалось загрузить спортсменов: {{ athletesError }}</p>
        <p v-if="refsError" class="form-error">Не удалось загрузить секции и мероприятия: {{ refsError }}</p>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>

      <!-- Платёж -->
      <BaseModal
        v-model="showPayment"
        title="Записать платёж"
        :submit-label="saving ? 'Сохранение…' : 'Сохранить'"
        @submit="submitPayment"
      >
        <div v-if="selectedCharge" class="info-block">
          <div class="info-row"><span>Начисление</span><strong>{{ selectedCharge.title }}</strong></div>
          <div class="info-row"><span>Спортсмен</span><strong>{{ athleteName(selectedCharge.athleteId) }}</strong></div>
          <div class="info-row"><span>Остаток</span><strong>{{ formatMoney(selectedCharge.remainingAmount) }}</strong></div>
        </div>
        <BaseInput id="payment-amount" v-model="paymentForm.amount" label="Сумма, ₽" />
        <BaseInput id="payment-date" v-model="paymentForm.paidOn" type="date" label="Дата оплаты" />
        <div class="field">
          <label class="field-label" for="payment-method">Способ</label>
          <select id="payment-method" v-model="paymentForm.method" class="role-select">
            <option v-for="o in methodOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <BaseInput id="payment-comment" v-model="paymentForm.comment" label="Комментарий" placeholder="Необязательно" />
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>

      <!-- Редактирование -->
      <BaseModal
        v-model="showEdit"
        title="Изменить начисление"
        :submit-label="saving ? 'Сохранение…' : 'Сохранить'"
        @submit="submitEdit"
      >
        <BaseInput id="edit-title" v-model="editForm.title" label="Название" />
        <BaseInput id="edit-amount" v-model="editForm.amount" label="Сумма, ₽" />
        <BaseInput id="edit-due" v-model="editForm.dueOn" type="date" label="Оплатить до" />
        <BaseInput id="edit-comment" v-model="editForm.comment" label="Комментарий" />
        <p v-if="selectedCharge && Number(selectedCharge.paidAmount) > 0" class="form-hint">
          Уже оплачено {{ formatMoney(selectedCharge.paidAmount) }} — сумма не может быть меньше.
        </p>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>

      <!-- Отмена -->
      <BaseModal
        v-model="showCancel"
        title="Отменить начисление"
        :submit-label="saving ? 'Сохранение…' : 'Отменить начисление'"
        @submit="submitCancel"
      >
        <div v-if="selectedCharge" class="info-block">
          <div class="info-row"><span>Начисление</span><strong>{{ selectedCharge.title }}</strong></div>
          <div class="info-row"><span>Спортсмен</span><strong>{{ athleteName(selectedCharge.athleteId) }}</strong></div>
          <div class="info-row"><span>Сумма</span><strong>{{ formatMoney(selectedCharge.amount) }}</strong></div>
        </div>
        <BaseInput id="cancel-reason" v-model="cancelReason" label="Причина отмены" placeholder="Например, выставлено по ошибке" />
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import Sidebar from '../../../components/layout/Sidebar.vue'
import PageHeader from '../../../components/layout/PageHeader.vue'
import FinanceTabs from '../../../components/layout/FinanceTabs.vue'
import BaseButton from '../../../components/ui/BaseButton.vue'
import BaseIcon from '../../../components/ui/BaseIcon.vue'
import BaseModal from '../../../components/ui/BaseModal.vue'
import BaseInput from '../../../components/ui/BaseInput.vue'
import StateBlock from '../../../components/ui/StateBlock.vue'
import { chargesApi } from '../../../api/charges'
import { paymentsApi } from '../../../api/payments'
import { athletesApi } from '../../../api/athletes'
import { sectionsApi } from '../../../api/sections'
import { groupsApi } from '../../../api/groups'
import { eventsApi } from '../../../api/events'
import { trainingsApi } from '../../../api/trainings'
import { getOrganizationId, hasPermission } from '../../../utils/session'
import {
  addDays, errorText, formatDateShort, formatDateTime, formatMoney, fullName, toIsoDate, toIsoDateTime, toMoney
} from '../../../utils/format'
import { label, options, tone } from '../../../utils/labels'

const PAGE_SIZE = 100
const FIELD_LABELS = {
  athleteId: 'Спортсмен', sectionId: 'Секция', type: 'Тип', title: 'Название', amount: 'Сумма', dueOn: 'Срок оплаты',
  periodFrom: 'Период с', periodTo: 'Период по', trainingId: 'Тренировка', eventId: 'Мероприятие', comment: 'Комментарий',
  chargeId: 'Начисление', paidOn: 'Дата оплаты', method: 'Способ', cancelReason: 'Причина отмены'
}
const paymentStatusOptions = options('paymentStatus')
const typeOptions = options('chargeType')
const methodOptions = options('paymentMethod')

const canWriteCharges = computed(() => hasPermission('charges.write'))
const canWritePayments = computed(() => hasPermission('payments.write'))

// ─────────── Спортсмены: подписи в таблице и выбор в формах ───────────
const athletes = ref([])
const athletesError = ref('')
const athleteNames = computed(() => Object.fromEntries(athletes.value.map(a => [a.id, fullName(a)])))
const sortedAthletes = computed(() =>
  [...athletes.value].sort((a, b) => fullName(a).localeCompare(fullName(b), 'ru')))
const activeAthletes = computed(() => sortedAthletes.value.filter(a => a.status === 'ACTIVE'))

function athleteName(id) {
  return athleteNames.value[id] || '—'
}

async function loadAthletes() {
  try {
    athletes.value = await fetchAll(params => athletesApi.list(getOrganizationId(), params))
    athletesError.value = ''
  } catch (e) {
    athletes.value = []
    athletesError.value = errorText(e)
  }
}

// ─────────── Список: поиск (q по названию) и фильтры — параметры сервера ───────────
const searchQuery = ref('')
const filters = reactive({
  paymentStatus: '',
  type: '',
  athleteId: '',
  isOverdue: false,
  showCancelled: false
})

const charges = ref([])
const loading = ref(true)
const loadError = ref('')
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loadingMore = ref(false)
const loadMoreError = ref('')
const hasMore = computed(() => page.value + 1 < totalPages.value)

function listParams() {
  return {
    q: searchQuery.value.trim() || undefined,
    paymentStatus: filters.paymentStatus || undefined,
    type: filters.type || undefined,
    athleteId: filters.athleteId || undefined,
    isOverdue: filters.isOverdue || undefined,
    status: filters.showCancelled ? undefined : 'ACTIVE'
  }
}

let loadSeq = 0
// silent: обновить данные после действия, не пряча таблицу за индикатором загрузки
async function load({ silent = false } = {}) {
  const seq = ++loadSeq
  if (!silent) loading.value = true
  loadError.value = ''
  loadMoreError.value = ''
  try {
    const res = await chargesApi.list(getOrganizationId(), { ...listParams(), page: 0, size: PAGE_SIZE })
    if (seq !== loadSeq) return
    charges.value = res.items || []
    page.value = 0
    totalPages.value = res.totalPages || 0
    totalElements.value = res.totalElements || 0
  } catch (e) {
    if (seq !== loadSeq) return
    charges.value = []
    loadError.value = errorText(e)
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value) return
  const seq = loadSeq
  loadingMore.value = true
  loadMoreError.value = ''
  try {
    const res = await chargesApi.list(getOrganizationId(), { ...listParams(), page: page.value + 1, size: PAGE_SIZE })
    if (seq !== loadSeq) return
    charges.value = charges.value.concat(res.items || [])
    page.value += 1
    totalPages.value = res.totalPages || 0
    totalElements.value = res.totalElements || 0
  } catch (e) {
    if (seq === loadSeq) loadMoreError.value = errorText(e)
  } finally {
    loadingMore.value = false
  }
}

const emptyMessage = computed(() => {
  const q = searchQuery.value.trim()
  if (q) return `Начислений с «${q}» в названии не найдено`
  const filtered = filters.paymentStatus || filters.type || filters.athleteId || filters.isOverdue
  if (filtered) return 'Начислений по выбранным фильтрам не найдено'
  return canWriteCharges.value ? 'Начислений пока нет — создайте первое' : 'Начислений пока нет'
})

// Итоги по показанным действующим начислениям
const totals = computed(() => {
  const active = charges.value.filter(c => c.status === 'ACTIVE')
  return {
    amount: sumMoney(active, 'amount'),
    paid: sumMoney(active, 'paidAmount'),
    debt: sumMoney(active, 'remainingAmount')
  }
})

// В Java-записи Charge флаг называется overdue (в документации — isOverdue)
function isOverdue(c) {
  return c.status === 'ACTIVE' && Boolean(c.overdue ?? c.isOverdue)
}
// Короткие подписи макета: колонка статуса узкая
const SHORT_PAYMENT_STATUS = { UNPAID: 'Не оплачено', PARTIALLY_PAID: 'Частично', PAID: 'Оплачено' }
function badgeLabel(c) {
  if (c.status === 'CANCELLED') return label('chargeStatus', c.status)
  return SHORT_PAYMENT_STATUS[c.paymentStatus] || label('paymentStatus', c.paymentStatus)
}
function badgeTone(c) {
  return c.status === 'CANCELLED' ? 'gray' : tone(c.paymentStatus)
}

function canPay(c) {
  return canWritePayments.value && c.status === 'ACTIVE' && c.paymentStatus !== 'PAID'
}
function canEdit(c) {
  return canWriteCharges.value && c.status === 'ACTIVE'
}
// Отменить можно только начисление без проведённых платежей (иначе сервер вернёт 409)
function canCancel(c) {
  return canWriteCharges.value && c.status === 'ACTIVE' && Number(c.paidAmount) === 0
}

// ─────────── Общее для форм ───────────
const saving = ref(false)
const formError = ref('')
const selectedCharge = ref(null)

function fail(message) {
  formError.value = message
  return false
}

// ─────────── Создание ───────────
const showCreate = ref(false)
const createForm = reactive({
  athleteId: '', sectionId: '', type: 'SUBSCRIPTION', title: '', amount: '', dueOn: '',
  periodFrom: '', periodTo: '', trainingId: '', eventId: '', comment: ''
})

// Справочники формы загружаются при первом открытии
const sections = ref([])
const groupSections = ref({})     // groupId → sectionId (секция тренировки берётся из её группы)
const events = ref([])            // опубликованные мероприятия и сборы
const refsLoaded = ref(false)
const refsError = ref('')

async function loadRefs() {
  const org = getOrganizationId()
  try {
    const [sectionItems, groupItems, eventItems] = await Promise.all([
      fetchAll(params => sectionsApi.list(org, params)),
      fetchAll(params => groupsApi.list(org, params)),
      fetchAll(params => eventsApi.list(org, params), { status: 'PUBLISHED' })
    ])
    sections.value = [...sectionItems].sort((a, b) => a.name.localeCompare(b.name, 'ru'))
    groupSections.value = Object.fromEntries(groupItems.map(g => [g.id, g.sectionId]))
    events.value = eventItems
    refsLoaded.value = true
    refsError.value = ''
  } catch (e) {
    refsError.value = errorText(e)
  }
}

// Архивную секцию нельзя выбрать заново, но выбранная остаётся видна
const sectionOptions = computed(() =>
  sections.value.filter(s => s.status === 'ACTIVE' || s.id === createForm.sectionId))

// Секции текущих групп выбранного спортсмена — подсказка для абонемента
const athleteSectionIds = ref([])
let athleteSeq = 0
async function loadAthleteSections(athleteId) {
  const seq = ++athleteSeq
  athleteSectionIds.value = []
  if (!athleteId) return
  try {
    const groups = await fetchAll(params => groupsApi.list(getOrganizationId(), params), { athleteId, status: 'ACTIVE' })
    if (seq !== athleteSeq) return
    athleteSectionIds.value = [...new Set(groups.map(g => g.sectionId))]
    if (createForm.type === 'SUBSCRIPTION' && !createForm.sectionId && athleteSectionIds.value.length) {
      createForm.sectionId = athleteSectionIds.value[0]
    }
  } catch (_) {
    // подсказка необязательна: секцию можно выбрать вручную
  }
}

// Тренировки спортсмена (его группы на дату занятия) за 90 дней назад и месяц вперёд
const trainings = ref([])
const trainingsLoading = ref(false)
const trainingsError = ref('')
let trainingsSeq = 0
async function loadTrainings() {
  const seq = ++trainingsSeq
  trainings.value = []
  trainingsError.value = ''
  if (!createForm.athleteId || createForm.type !== 'TRAINING') return
  trainingsLoading.value = true
  try {
    const now = new Date()
    const res = await trainingsApi.list(getOrganizationId(), {
      from: toIsoDateTime(addDays(now, -90)),
      to: toIsoDateTime(addDays(now, 31)),
      athleteId: createForm.athleteId,
      size: 100
    })
    if (seq !== trainingsSeq) return
    trainings.value = (res.items || [])
      .filter(t => t.status !== 'CANCELLED')
      .sort((a, b) => String(b.startsAt).localeCompare(String(a.startsAt)))
  } catch (e) {
    if (seq === trainingsSeq) trainingsError.value = errorText(e)
  } finally {
    if (seq === trainingsSeq) trainingsLoading.value = false
  }
}

const trainingPlaceholder = computed(() => {
  if (!createForm.athleteId) return 'Сначала выберите спортсмена'
  if (trainingsLoading.value) return 'Загрузка…'
  if (trainingsError.value) return `Ошибка: ${trainingsError.value}`
  return trainings.value.length ? 'Выберите тренировку' : 'Нет тренировок спортсмена за 90 дней'
})

watch(() => createForm.athleteId, athleteId => {
  createForm.trainingId = ''
  if (createForm.type === 'SUBSCRIPTION') createForm.sectionId = ''
  loadAthleteSections(athleteId)
  loadTrainings()
})

// Подсказки из выбранной тренировки или мероприятия: заменяются при новом выборе, пока пользователь их не правил
const suggested = { title: '', amount: '', dueOn: '' }
function suggest(field, value) {
  if (!value) return
  if (!String(createForm[field]).trim() || createForm[field] === suggested[field]) {
    createForm[field] = value
    suggested[field] = value
  }
}
function clearSuggestions() {
  for (const field of Object.keys(suggested)) {
    if (suggested[field] && createForm[field] === suggested[field]) createForm[field] = ''
    suggested[field] = ''
  }
}

watch(() => createForm.type, type => {
  createForm.trainingId = ''
  createForm.eventId = ''
  clearSuggestions()
  createForm.sectionId = type === 'SUBSCRIPTION' ? (athleteSectionIds.value[0] || '') : ''
  if (type === 'TRAINING') loadTrainings()
})

// Секция тренировки — секция её группы; название — по тренировке
watch(() => createForm.trainingId, async id => {
  if (createForm.type !== 'TRAINING') return
  createForm.sectionId = ''
  const training = trainings.value.find(t => t.id === id)
  if (!training) return
  suggest('title', `${training.title} · ${formatDateShort(training.startsAt)}`)
  let sectionId = groupSections.value[training.groupId]
  if (!sectionId) {
    try {
      sectionId = (await groupsApi.get(getOrganizationId(), training.groupId)).group?.sectionId
    } catch (_) {
      sectionId = ''
    }
  }
  if (createForm.type === 'TRAINING' && createForm.trainingId === id) createForm.sectionId = sectionId || ''
})

// Секция мероприятия неизменна; взнос с участника и срок сбора — подсказки для суммы и срока
watch(() => createForm.eventId, id => {
  if (createForm.type !== 'EVENT') return
  const event = events.value.find(e => e.id === id)
  createForm.sectionId = event ? event.sectionId : ''
  if (!event) return
  suggest('title', event.title)
  if (event.costPerAthlete) suggest('amount', plainAmount(event.costPerAthlete))
  suggest('dueOn', event.collectionDueOn)
})

function openCreate() {
  const now = new Date()
  const previousAthleteId = createForm.athleteId
  Object.assign(createForm, {
    athleteId: filters.athleteId || '',
    sectionId: '',
    type: 'SUBSCRIPTION',
    title: '',
    amount: '',
    dueOn: '',
    periodFrom: toIsoDate(new Date(now.getFullYear(), now.getMonth(), 1)),
    periodTo: toIsoDate(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
    trainingId: '',
    eventId: '',
    comment: ''
  })
  Object.assign(suggested, { title: '', amount: '', dueOn: '' })
  formError.value = ''
  showCreate.value = true
  if (!refsLoaded.value) loadRefs()
  // Новый спортсмен подхватывается наблюдателем; для прежнего заново подставляем секцию его группы
  if (createForm.athleteId && createForm.athleteId === previousAthleteId) loadAthleteSections(createForm.athleteId)
}

async function submitCreate() {
  if (saving.value) return
  const f = createForm
  const title = f.title.trim()
  const amount = toMoney(f.amount)
  if (!f.athleteId) return fail('Выберите спортсмена.')
  if (f.type === 'TRAINING' && !f.trainingId) return fail('Выберите тренировку.')
  if (f.type === 'EVENT' && !f.eventId) return fail('Выберите мероприятие или сбор.')
  if (!f.sectionId) return fail('Выберите секцию.')
  if (!title) return fail('Укажите название начисления.')
  if (!amount || Number(amount) <= 0) return fail('Сумма должна быть больше нуля.')
  if (!f.dueOn) return fail('Укажите срок оплаты.')
  if (f.type === 'SUBSCRIPTION') {
    if (!f.periodFrom || !f.periodTo) return fail('Укажите период абонемента.')
    if (f.periodFrom > f.periodTo) return fail('Начало периода позже его окончания.')
  }

  saving.value = true
  formError.value = ''
  try {
    // ChargeWrite: только поля записи, ссылки и период — по типу начисления
    await chargesApi.create(getOrganizationId(), {
      athleteId: f.athleteId,
      sectionId: f.sectionId,
      type: f.type,
      title,
      amount,
      dueOn: f.dueOn,
      periodFrom: f.type === 'SUBSCRIPTION' ? f.periodFrom : null,
      periodTo: f.type === 'SUBSCRIPTION' ? f.periodTo : null,
      trainingId: f.type === 'TRAINING' ? f.trainingId : null,
      eventId: f.type === 'EVENT' ? f.eventId : null,
      comment: f.comment.trim() || null
    })
    showCreate.value = false
    await load({ silent: true })
  } catch (e) {
    formError.value = formErrorText(e)
  } finally {
    saving.value = false
  }
}

// ─────────── Платёж ───────────
const showPayment = ref(false)
const paymentForm = reactive({ amount: '', paidOn: '', method: 'SBP', comment: '' })

function openPayment(charge) {
  selectedCharge.value = charge
  Object.assign(paymentForm, {
    amount: plainAmount(charge.remainingAmount),
    paidOn: toIsoDate(new Date()),
    method: 'SBP',
    comment: ''
  })
  formError.value = ''
  showPayment.value = true
}

async function submitPayment() {
  if (saving.value) return
  const charge = selectedCharge.value
  const amount = toMoney(paymentForm.amount)
  if (!amount || Number(amount) <= 0) return fail('Сумма платежа должна быть больше нуля.')
  if (Number(amount) > Number(charge.remainingAmount)) {
    return fail(`Сумма больше остатка по начислению (${formatMoney(charge.remainingAmount)}).`)
  }
  if (!paymentForm.paidOn) return fail('Укажите дату оплаты.')
  if (paymentForm.paidOn > toIsoDate(new Date())) return fail('Дата оплаты не может быть в будущем.')

  saving.value = true
  formError.value = ''
  try {
    await paymentsApi.create(getOrganizationId(), {
      chargeId: charge.id,
      amount,
      paidOn: paymentForm.paidOn,
      method: paymentForm.method,
      comment: paymentForm.comment.trim() || null
    })
    showPayment.value = false
    await load({ silent: true })
  } catch (e) {
    formError.value = formErrorText(e)
  } finally {
    saving.value = false
  }
}

// ─────────── Редактирование ───────────
const showEdit = ref(false)
const editForm = reactive({ title: '', amount: '', dueOn: '', comment: '' })

function openEdit(charge) {
  selectedCharge.value = charge
  Object.assign(editForm, {
    title: charge.title,
    amount: plainAmount(charge.amount),
    dueOn: charge.dueOn,
    comment: charge.comment || ''
  })
  formError.value = ''
  showEdit.value = true
}

async function submitEdit() {
  if (saving.value) return
  const charge = selectedCharge.value
  const title = editForm.title.trim()
  const amount = toMoney(editForm.amount)
  const comment = editForm.comment.trim() || null
  if (!title) return fail('Название не может быть пустым.')
  if (!amount || Number(amount) <= 0) return fail('Сумма должна быть больше нуля.')
  if (Number(amount) < Number(charge.paidAmount)) {
    return fail(`Сумма не может быть меньше уже оплаченной (${formatMoney(charge.paidAmount)}).`)
  }
  if (!editForm.dueOn) return fail('Укажите срок оплаты.')

  // PATCH: только изменённые поля
  const patch = {}
  if (title !== charge.title) patch.title = title
  if (Number(amount) !== Number(charge.amount)) patch.amount = amount
  if (editForm.dueOn !== charge.dueOn) patch.dueOn = editForm.dueOn
  if (comment !== (charge.comment || null)) patch.comment = comment
  if (!Object.keys(patch).length) {
    showEdit.value = false
    return
  }

  saving.value = true
  formError.value = ''
  try {
    await chargesApi.update(getOrganizationId(), charge.id, patch)
    showEdit.value = false
    await load({ silent: true })
  } catch (e) {
    formError.value = formErrorText(e)
  } finally {
    saving.value = false
  }
}

// ─────────── Отмена (причина обязательна) ───────────
const showCancel = ref(false)
const cancelReason = ref('')

function openCancel(charge) {
  selectedCharge.value = charge
  cancelReason.value = ''
  formError.value = ''
  showCancel.value = true
}

async function submitCancel() {
  if (saving.value) return
  const reason = cancelReason.value.trim()
  if (!reason) return fail('Укажите причину отмены.')

  saving.value = true
  formError.value = ''
  try {
    await chargesApi.cancel(getOrganizationId(), selectedCharge.value.id, reason)
    showCancel.value = false
    await load({ silent: true })
  } catch (e) {
    formError.value = formErrorText(e)
  } finally {
    saving.value = false
  }
}

// ─────────── Жизненный цикл ───────────
let searchTimer = null
watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(load, 300)
})
watch(
  () => [filters.paymentStatus, filters.type, filters.athleteId, filters.isOverdue, filters.showCancelled],
  () => load()
)
onBeforeUnmount(() => clearTimeout(searchTimer))

onMounted(() => {
  load()
  loadAthletes()
})

// ─────────── Помощники ───────────
// '12000.00' → '12000' для полей ввода
function plainAmount(value) {
  const n = Number(value)
  return isNaN(n) ? '' : String(n)
}

// Сумма денежных строк API без ошибок округления
function sumMoney(items, field) {
  return items.reduce((s, item) => s + Math.round(Number(item[field] || 0) * 100), 0) / 100
}

// Все страницы списка (size ≤ 100 по контракту), не больше maxPages запросов
async function fetchAll(request, params = {}, maxPages = 10) {
  const first = await request({ ...params, page: 0, size: 100 })
  const pages = Math.min(first.totalPages || 1, maxPages)
  const rest = await Promise.all(
    Array.from({ length: pages - 1 }, (_, i) => request({ ...params, page: i + 1, size: 100 }))
  )
  return rest.reduce((all, page) => all.concat(page.items || []), first.items || [])
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

.filters { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.filter-select { padding: 10px 16px; background: white; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 13px; color: #6D7D79; cursor: pointer; outline: none; }
.checkbox { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #6D7D79; }

.table-card { background: white; border: 1px solid #E3EAE8; border-radius: 16px; overflow-x: auto; }
.table-header, .table-row { display: flex; align-items: center; gap: 16px; padding: 12px 24px; min-width: 1200px; }
.table-header { background: #F4F7F8; border-bottom: 1px solid #E3EAE8; font-size: 12px; font-weight: 700; color: #6D7D79; text-transform: uppercase; }
.table-row { padding: 16px 24px; border-bottom: 1px solid #E3EAE8; font-size: 13px; }
.col { color: #152421; }
.col:not(.actions) { flex-shrink: 0; } /* колонки не сжимаются от числа кнопок в строке */
.col.num { width: 40px; color: #6D7D79; }
.col.date { width: 100px; color: #6D7D79; }
.col.name { width: 180px; font-weight: 600; }
.col.title { width: 220px; font-weight: 500; position: relative; }
.col.amount { width: 110px; font-weight: 700; }
.col.paid { width: 110px; color: #2E8B57; font-weight: 600; }
.col.status { width: 130px; }
.col.actions { flex: 1; display: flex; justify-content: flex-end; gap: 6px; }
.overdue-tag { display: inline-block; margin-left: 6px; padding: 2px 6px; background: #FCE2E5; color: #D64545; border-radius: 4px; font-size: 10px; font-weight: 700; }
.badge { padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 700; }
.badge.green { background: #E9F7D5; color: #2E8B57; }
.badge.yellow { background: #FFF8E6; color: #F2B705; }
.badge.red { background: #FEE2E2; color: #D64545; }
.badge.gray { background: #EEF1F0; color: #888888; }
.action-btn { padding: 6px 10px; background: #F4F7F8; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; color: #152421; cursor: pointer; }
.action-btn.outline { background: white; border: 1px solid #E3EAE8; }
.action-btn.danger { background: #FCE2E5; color: #D64545; }
.action-btn:disabled { opacity: 0.6; cursor: default; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; }
.load-more { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 24px; border-bottom: 1px solid #E3EAE8; }
.table-footer { display: flex; justify-content: space-between; padding: 16px 24px; background: #F4F7F8; font-size: 13px; flex-wrap: wrap; gap: 12px; }
.footer-total strong { font-weight: 800; }
.footer-detail { color: #6D7D79; font-size: 12px; }
.info-block { padding: 12px; background: #F4F7F8; border-radius: 8px; }
.info-row { display: flex; justify-content: space-between; gap: 12px; font-size: 13px; margin-bottom: 4px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.role-select { padding: 10px 12px; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 14px; background: white; cursor: pointer; outline: none; }
.role-select:disabled { background: #F4F7F8; color: #6D7D79; cursor: default; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-hint { font-size: 12px; color: #6D7D79; }
.form-error { font-size: 13px; color: #D64545; }
</style>
