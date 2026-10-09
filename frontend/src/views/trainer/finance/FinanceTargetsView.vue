<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Финансы"
        subtitle="Целевые сборы на поездки, мероприятия и экипировку"
        v-model="searchQuery"
        search-placeholder="Поиск сбора..."
      >
        <template #actions>
          <BaseButton v-if="canWriteEvents" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать сбор
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <section class="fees-grid">
        <div v-if="loading" class="empty-state">
          <StateBlock kind="loading" />
        </div>
        <div v-else-if="loadError" class="empty-state">
          <StateBlock kind="error" :message="loadError" />
        </div>
        <div v-else-if="fees.length === 0" class="empty-state">
          <StateBlock kind="empty" :message="emptyMessage" />
        </div>
        <template v-else>
          <div
            v-for="fee in fees"
            :key="fee.id"
            class="fee-card"
            :class="{ active: fee.id === selectedId }"
            @click="selectedId = fee.id"
          >
            <div class="fee-header">
              <h3>{{ fee.title }}</h3>
              <span class="status-badge" :class="fee.badgeClass">{{ fee.badge }}</span>
            </div>
            <div class="fee-stats">
              <div class="fee-line">
                <span>Цель: {{ formatMoney(fee.target) }}</span>
                <span class="collected">Собрано: {{ fee.collected === null ? '—' : formatMoney(fee.collected) }}</span>
              </div>
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: Math.min(fee.percent, 100) + '%' }"></div>
              </div>
              <div class="fee-line small">
                <span>{{ fee.collected === null ? 'Сумма взносов недоступна' : `${fee.percent}% собрано` }}</span>
                <span v-if="fee.remaining > 0" class="remaining">Осталось: {{ formatMoney(fee.remaining) }}</span>
              </div>
            </div>
          </div>
        </template>
      </section>

      <div v-if="selectedFee" class="detail-card">
        <h3>Взносы по сбору «{{ selectedFee.title }}»</h3>
        <StateBlock v-if="selectedFee.chargesError" kind="error" :message="selectedFee.chargesError" />
        <StateBlock
          v-else-if="contributions.length === 0"
          kind="empty"
          message="Взносов по этому сбору ещё нет: выставьте их во вкладке «Начисления» с типом «Мероприятие или сбор»"
        />
        <div v-else class="table">
          <div class="table-header">
            <div class="col name">СПОРТСМЕН</div>
            <div class="col amount">НАЧИСЛЕНО</div>
            <div class="col amount">ОПЛАЧЕНО</div>
            <div class="col amount">ДОЛГ</div>
            <div class="col status">СТАТУС</div>
          </div>
          <div v-for="a in contributions" :key="a.athleteId" class="table-row">
            <div class="col name">{{ a.name }}</div>
            <div class="col amount">{{ formatMoney(a.charged) }}</div>
            <div class="col amount paid">{{ formatMoney(a.paid) }}</div>
            <div class="col amount debt">{{ formatMoney(a.debt) }}</div>
            <div class="col status">
              <span class="status-badge" :class="`status-${tone(a.status)}`">{{ SHORT_PAYMENT_STATUS[a.status] }}</span>
            </div>
          </div>
        </div>
      </div>

      <BaseModal
        v-model="showModal"
        title="Создать целевой сбор"
        :submit-label="saving ? 'Сохранение…' : 'Создать'"
        @submit="handleSubmit"
      >
        <BaseInput id="fee-title" v-model="form.title" label="Название сбора" placeholder="Сбор Сочи 2026" />
        <div class="field">
          <label class="field-label" for="fee-section">Секция</label>
          <select id="fee-section" v-model="form.sectionId" class="field-control">
            <option value="" disabled>Выберите секцию</option>
            <option v-for="s in sections" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="row-2">
          <BaseInput id="fee-target" v-model="form.target" label="Цель, ₽" placeholder="150000" />
          <BaseInput id="fee-deadline" v-model="form.deadline" type="date" label="Дедлайн" />
        </div>
        <p class="form-hint">
          Сбор создаётся черновиком. Опубликуйте его и добавьте участников в разделе «Мероприятия»,
          затем выставьте взносы во вкладке «Начисления».
        </p>
        <p v-if="sectionsError" class="form-error">Не удалось загрузить секции: {{ sectionsError }}</p>
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
import { eventsApi } from '../../../api/events'
import { chargesApi } from '../../../api/charges'
import { athletesApi } from '../../../api/athletes'
import { sectionsApi } from '../../../api/sections'
import { getOrganizationId, hasPermission } from '../../../utils/session'
import { errorText, formatDateShort, formatMoney, fullName, toIsoDate, toMoney } from '../../../utils/format'
import { label, tone } from '../../../utils/labels'

const FIELD_LABELS = {
  title: 'Название', type: 'Тип', sectionId: 'Секция', targetAmount: 'Цель', collectionDueOn: 'Дедлайн',
  requiredDocumentTypes: 'Документы'
}
// Подписи статусов взноса — как в макете (колонка узкая)
const SHORT_PAYMENT_STATUS = { UNPAID: 'Не оплачено', PARTIALLY_PAID: 'Частично', PAID: 'Оплачено' }

const canWriteEvents = computed(() => hasPermission('events.write'))

// ─────────── Сборы: мероприятия типа FUNDRAISER, поиск q — параметр сервера ───────────
const searchQuery = ref('')
const events = ref([])
const eventCharges = ref({})   // eventId → действующие начисления сбора (null — не загрузились)
const chargesErrors = ref({})  // eventId → текст ошибки
const loading = ref(true)
const loadError = ref('')
const selectedId = ref(null)

let loadSeq = 0
async function load({ silent = false } = {}) {
  const seq = ++loadSeq
  if (!silent) loading.value = true
  loadError.value = ''
  try {
    const org = getOrganizationId()
    const items = await fetchAll(params => eventsApi.list(org, params), {
      type: 'FUNDRAISER',
      q: searchQuery.value.trim() || undefined
    })
    // Собранное считаем по взносам: GET /events/{id} отдаёт только Event, без финансовых итогов
    const results = await Promise.allSettled(items.map(e =>
      fetchAll(params => chargesApi.list(org, params), { eventId: e.id, status: 'ACTIVE' })))
    if (seq !== loadSeq) return
    events.value = items
    eventCharges.value = Object.fromEntries(items.map((e, i) =>
      [e.id, results[i].status === 'fulfilled' ? results[i].value : null]))
    chargesErrors.value = Object.fromEntries(items.map((e, i) =>
      [e.id, results[i].status === 'rejected' ? errorText(results[i].reason) : '']))
    if (!items.some(e => e.id === selectedId.value)) selectedId.value = items[0]?.id || null
  } catch (e) {
    if (seq !== loadSeq) return
    events.value = []
    loadError.value = errorText(e)
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

const fees = computed(() => events.value.map(e => {
  const charges = eventCharges.value[e.id]
  const target = Number(e.targetAmount) || 0
  const collected = charges ? sumMoney(charges, 'paidAmount') : null
  const percent = collected !== null && target > 0 ? Math.round(collected / target * 100) : 0
  return {
    id: e.id,
    title: e.title,
    target,
    collected,
    percent,
    remaining: collected !== null ? Math.max(target - collected, 0) : 0,
    chargesError: chargesErrors.value[e.id] || '',
    ...badge(e, collected, target)
  }
}))

// Опубликованный сбор: «Выполнен» при достижении цели, иначе срок (красный — если прошёл); остальные — статус
function badge(event, collected, target) {
  if (event.status !== 'PUBLISHED') {
    return { badge: label('eventStatus', event.status), badgeClass: `status-${tone(event.status)}` }
  }
  if (collected !== null && target > 0 && collected >= target) return { badge: 'Выполнен', badgeClass: 'status-green' }
  if (!event.collectionDueOn) return { badge: label('eventStatus', event.status), badgeClass: 'status-blue' }
  const expired = event.collectionDueOn < toIsoDate(new Date())
  return { badge: `до ${formatDateShort(event.collectionDueOn)}`, badgeClass: expired ? 'status-red' : 'status-blue' }
}

const selectedFee = computed(() => fees.value.find(f => f.id === selectedId.value) || null)

const emptyMessage = computed(() => {
  const q = searchQuery.value.trim()
  if (q) return `Сборов по запросу «${q}» не найдено`
  return canWriteEvents.value ? 'Целевых сборов пока нет — создайте первый' : 'Целевых сборов пока нет'
})

// ─────────── Взносы выбранного сбора: по спортсменам ───────────
const athletes = ref([])
const athleteNames = computed(() => Object.fromEntries(athletes.value.map(a => [a.id, fullName(a)])))

async function loadAthletes() {
  try {
    athletes.value = await fetchAll(params => athletesApi.list(getOrganizationId(), params))
  } catch (_) {
    athletes.value = [] // без имён таблица всё равно показывает суммы
  }
}

const contributions = computed(() => {
  const byAthlete = new Map()
  for (const c of eventCharges.value[selectedId.value] || []) {
    const row = byAthlete.get(c.athleteId) || { athleteId: c.athleteId, charged: 0, paid: 0, debt: 0 }
    row.charged += kopecks(c.amount)
    row.paid += kopecks(c.paidAmount)
    row.debt += kopecks(c.remainingAmount)
    byAthlete.set(c.athleteId, row)
  }
  return [...byAthlete.values()]
    .map(r => ({
      athleteId: r.athleteId,
      name: athleteNames.value[r.athleteId] || '—',
      charged: r.charged / 100,
      paid: r.paid / 100,
      debt: r.debt / 100,
      status: r.paid === 0 ? 'UNPAID' : r.debt === 0 ? 'PAID' : 'PARTIALLY_PAID'
    }))
    .sort((a, b) => a.name.localeCompare(b.name, 'ru'))
})

// ─────────── Создание сбора (EventWrite с type = FUNDRAISER) ───────────
const showModal = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive({ title: '', sectionId: '', target: '', deadline: '' })
const sections = ref([])
const sectionsError = ref('')

async function loadSections() {
  try {
    const items = await fetchAll(params => sectionsApi.list(getOrganizationId(), params), { status: 'ACTIVE' })
    sections.value = items.sort((a, b) => a.name.localeCompare(b.name, 'ru'))
    sectionsError.value = ''
    if (!form.sectionId && sections.value.length === 1) form.sectionId = sections.value[0].id
  } catch (e) {
    sectionsError.value = errorText(e)
  }
}

function openCreate() {
  Object.assign(form, { title: '', sectionId: sections.value.length === 1 ? sections.value[0].id : '', target: '', deadline: '' })
  formError.value = ''
  showModal.value = true
  if (!sections.value.length) loadSections()
}

async function handleSubmit() {
  if (saving.value) return
  const title = form.title.trim()
  const targetAmount = toMoney(form.target)
  if (!title) { formError.value = 'Укажите название сбора.'; return }
  if (!form.sectionId) { formError.value = 'Выберите секцию.'; return }
  if (!targetAmount || Number(targetAmount) <= 0) { formError.value = 'Цель сбора должна быть больше нуля.'; return }
  if (!form.deadline) { formError.value = 'Укажите дедлайн сбора.'; return }

  saving.value = true
  formError.value = ''
  try {
    // Для FUNDRAISER: цель и срок обязательны, startsAt/endsAt/location не передаются
    const created = await eventsApi.create(getOrganizationId(), {
      title,
      type: 'FUNDRAISER',
      sectionId: form.sectionId,
      targetAmount,
      collectionDueOn: form.deadline,
      requiredDocumentTypes: []
    })
    showModal.value = false
    if (created?.id) selectedId.value = created.id
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
onBeforeUnmount(() => clearTimeout(searchTimer))

onMounted(() => {
  load()
  loadAthletes()
})

// ─────────── Помощники ───────────
function kopecks(value) {
  return Math.round(Number(value || 0) * 100)
}

function sumMoney(items, field) {
  return items.reduce((s, item) => s + kopecks(item[field]), 0) / 100
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
.fees-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; }
.fee-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 16px; padding: 20px;
  display: flex; flex-direction: column; gap: 16px;
  cursor: pointer;
}
.fee-card.active { border-color: #102522; }
.fee-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.fee-header h3 { font-size: 16px; font-weight: 700; color: #152421; }
.status-badge { padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 700; white-space: nowrap; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-blue { background: #DDECFB; color: #35678E; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.status-red { background: #FEE2E2; color: #D64545; }
.status-gray { background: #EEF1F0; color: #888888; }
.fee-stats { display: flex; flex-direction: column; gap: 8px; }
.fee-line { display: flex; justify-content: space-between; font-size: 12px; }
.fee-line.small { font-size: 11px; color: #98A6A2; }
.collected { color: #2E8B57; font-weight: 700; }
.remaining { color: #D64545; }
.progress-track { height: 8px; background: #F4F7F8; border-radius: 4px; overflow: hidden; }
.progress-fill { height: 100%; background: #102522; border-radius: 4px; }
.detail-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
  overflow-x: auto;
}
.detail-card h3 { font-size: 16px; font-weight: 700; color: #152421; }
.table { min-width: 900px; }
.table-header, .table-row { display: flex; align-items: center; gap: 16px; padding: 12px 16px; }
.table-header {
  background: #F4F7F8; border-radius: 8px;
  font-size: 12px; font-weight: 700; color: #6D7D79; text-transform: uppercase;
}
.table-row { border-bottom: 1px solid #E3EAE8; font-size: 13px; }
.col.name, .col.amount { flex-shrink: 0; }
.col.name { width: 300px; font-weight: 600; color: #152421; }
.col.amount { width: 150px; color: #6D7D79; }
.col.amount.paid { color: #2E8B57; font-weight: 600; }
.col.amount.debt { color: #D64545; font-weight: 600; }
.col.status { flex: 1; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; grid-column: 1 / -1; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.field-control { padding: 10px 12px; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 14px; background: white; cursor: pointer; outline: none; }
.form-hint { font-size: 12px; color: #6D7D79; line-height: 1.5; }
.form-error { font-size: 13px; color: #D64545; }
</style>
