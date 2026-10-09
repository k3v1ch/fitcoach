<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Финансы"
        subtitle="Журнал входящих платежей и оплат"
        v-model="searchQuery"
        search-placeholder="Поиск платежа..."
      >
        <template #actions>
          <BaseButton v-if="canWritePayments" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Принять оплату
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <div class="toolbar">
        <div class="filters">
          <select v-model="filters.period" class="filter-select">
            <option v-for="p in periodOptions" :key="p.value" :value="p.value">{{ p.label }}</option>
          </select>
          <select v-model="filters.method" class="filter-select">
            <option value="">Все способы</option>
            <option v-for="o in methodOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
          <select v-model="filters.status" class="filter-select">
            <option value="">Все статусы</option>
            <option v-for="o in statusOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
          <select v-model="filters.athleteId" class="filter-select">
            <option value="">Все спортсмены</option>
            <option v-for="a in sortedAthletes" :key="a.id" :value="a.id">{{ fullName(a) }}</option>
          </select>
        </div>
      </div>

      <div class="table-card">
        <div class="table-header">
          <div class="col num">№</div>
          <div class="col date">ДАТА</div>
          <div class="col name">СПОРТСМЕН</div>
          <div class="col amount">СУММА</div>
          <div class="col method">СПОСОБ</div>
          <div class="col basis">ОСНОВАНИЕ (НАЧИСЛЕНИЕ)</div>
          <div class="col status">СТАТУС</div>
        </div>

        <StateBlock v-if="loading" kind="loading" />
        <StateBlock v-else-if="loadError" kind="error" :message="loadError" />
        <template v-else>
          <div
            v-for="(pay, idx) in filteredPayments"
            :key="pay.id"
            class="table-row"
            :class="{ voided: pay.status === 'VOIDED' }"
          >
            <div class="col num">{{ idx + 1 }}</div>
            <div class="col date">{{ formatDateShort(pay.paidOn) }}</div>
            <div class="col name">{{ athleteName(pay.athleteId) }}</div>
            <div class="col amount">{{ formatMoney(pay.amount) }}</div>
            <div class="col method">{{ label('paymentMethod', pay.method) }}</div>
            <div class="col basis">
              {{ chargeTitle(pay.chargeId) }}
              <span v-if="pay.comment" class="basis-comment">· {{ pay.comment }}</span>
            </div>
            <div class="col status">
              <span
                class="status-badge"
                :class="`status-${tone(pay.status)}`"
                :title="pay.voidReason ? `Причина: ${pay.voidReason}` : ''"
              >{{ label('paymentRecordStatus', pay.status) }}</span>
              <button
                v-if="canWritePayments && pay.status === 'ACTIVE'"
                class="void-btn"
                @click="openVoid(pay)"
              >Аннулировать</button>
            </div>
          </div>
          <div v-if="filteredPayments.length === 0" class="empty-state">
            {{ emptyMessage }}
          </div>
          <div class="table-footer">
            <div class="footer-total">
              Итого поступило средств: <strong>{{ formatMoney(totals.all) }}</strong>
            </div>
            <div class="footer-detail">
              Через банк: {{ formatMoney(totals.bank) }} | Наличные: {{ formatMoney(totals.cash) }}<template v-if="totals.other"> | Другое: {{ formatMoney(totals.other) }}</template>
              <template v-if="truncated"> · показаны последние {{ payments.length }} из {{ totalElements }} — сузьте период</template>
            </div>
          </div>
        </template>
      </div>

      <!-- Приём оплаты по начислению -->
      <BaseModal
        v-model="showModal"
        title="Принять оплату"
        :submit-label="saving ? 'Сохранение…' : 'Сохранить'"
        @submit="handleSubmit"
      >
        <div class="field">
          <label class="field-label" for="payment-athlete">Спортсмен</label>
          <select id="payment-athlete" v-model="form.athleteId" class="field-control">
            <option value="" disabled>Выберите спортсмена</option>
            <option v-for="a in sortedAthletes" :key="a.id" :value="a.id">{{ fullName(a) }}</option>
          </select>
        </div>
        <div class="field">
          <label class="field-label" for="payment-charge">Основание (начисление)</label>
          <select
            id="payment-charge"
            v-model="form.chargeId"
            class="field-control"
            :disabled="!form.athleteId || openChargesLoading"
          >
            <option value="" disabled>{{ chargePlaceholder }}</option>
            <option v-for="c in openCharges" :key="c.id" :value="c.id">
              {{ c.title }} · остаток {{ formatMoney(c.remainingAmount) }} · до {{ formatDateShort(c.dueOn) }}
            </option>
          </select>
        </div>
        <div class="row-2">
          <BaseInput id="payment-amount" v-model="form.amount" label="Сумма, ₽" placeholder="3400" />
          <BaseInput id="payment-date" v-model="form.paidOn" type="date" label="Дата оплаты" />
        </div>
        <div class="field">
          <label class="field-label" for="payment-method">Способ оплаты</label>
          <select id="payment-method" v-model="form.method" class="field-control">
            <option v-for="o in methodOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <BaseInput id="payment-comment" v-model="form.comment" label="Комментарий" placeholder="Необязательно" />
        <p v-if="athletesError" class="form-error">Не удалось загрузить спортсменов: {{ athletesError }}</p>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>

      <!-- Аннулирование ошибочной записи -->
      <BaseModal
        v-model="showVoid"
        title="Аннулировать платёж"
        :submit-label="saving ? 'Сохранение…' : 'Аннулировать'"
        @submit="submitVoid"
      >
        <div v-if="voiding" class="info-block">
          <div class="info-row"><span>Спортсмен</span><strong>{{ athleteName(voiding.athleteId) }}</strong></div>
          <div class="info-row"><span>Сумма</span><strong>{{ formatMoney(voiding.amount) }}</strong></div>
          <div class="info-row"><span>Начисление</span><strong>{{ chargeTitle(voiding.chargeId) }}</strong></div>
        </div>
        <p class="form-hint">Деньги не возвращаются: исправляется только учётная запись, долг по начислению пересчитается.</p>
        <BaseInput id="void-reason" v-model="voidReason" label="Причина" placeholder="Например, платёж внесён дважды" />
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import Sidebar from '../../../components/layout/Sidebar.vue'
import PageHeader from '../../../components/layout/PageHeader.vue'
import FinanceTabs from '../../../components/layout/FinanceTabs.vue'
import BaseButton from '../../../components/ui/BaseButton.vue'
import BaseIcon from '../../../components/ui/BaseIcon.vue'
import BaseModal from '../../../components/ui/BaseModal.vue'
import BaseInput from '../../../components/ui/BaseInput.vue'
import StateBlock from '../../../components/ui/StateBlock.vue'
import { paymentsApi } from '../../../api/payments'
import { chargesApi } from '../../../api/charges'
import { athletesApi } from '../../../api/athletes'
import { getOrganizationId, hasPermission } from '../../../utils/session'
import { addDays, errorText, formatDateShort, formatMoney, fullName, toIsoDate, toMoney } from '../../../utils/format'
import { label, options, tone } from '../../../utils/labels'

const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь']
const MAX_PAGES = 10
const FIELD_LABELS = {
  chargeId: 'Начисление', amount: 'Сумма', paidOn: 'Дата оплаты', method: 'Способ оплаты', comment: 'Комментарий', reason: 'Причина'
}
const methodOptions = options('paymentMethod')
const statusOptions = options('paymentRecordStatus')

const canWritePayments = computed(() => hasPermission('payments.write'))

// ─────────── Период: последние 30 дней, три последних месяца или всё время ───────────
const periodOptions = (() => {
  const now = new Date()
  const list = [{ value: 'last30', label: 'Последние 30 дней' }]
  for (let i = 0; i < 3; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    list.push({ value: `month:${i}`, label: `${MONTHS[d.getMonth()]} ${d.getFullYear()}` })
  }
  list.push({ value: 'all', label: 'За всё время' })
  return list
})()

function periodBounds(value) {
  const now = new Date()
  if (value === 'last30') return { from: toIsoDate(addDays(now, -29)), to: toIsoDate(now) }
  if (value.startsWith('month:')) {
    const first = new Date(now.getFullYear(), now.getMonth() - Number(value.slice(6)), 1)
    return { from: toIsoDate(first), to: toIsoDate(new Date(first.getFullYear(), first.getMonth() + 1, 0)) }
  }
  return { from: undefined, to: undefined }
}

// ─────────── Спортсмены: подписи и выбор ───────────
const athletes = ref([])
const athletesError = ref('')
const athleteNames = computed(() => Object.fromEntries(athletes.value.map(a => [a.id, fullName(a)])))
const sortedAthletes = computed(() =>
  [...athletes.value].sort((a, b) => fullName(a).localeCompare(fullName(b), 'ru')))

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

// ─────────── Названия начислений: в Payment есть только chargeId ───────────
const chargeTitles = ref({})

function chargeTitle(id) {
  const title = chargeTitles.value[id]
  return title === undefined ? '…' : (title || '—')
}

async function loadChargeTitles(ids) {
  const org = getOrganizationId()
  const found = {}
  let missing = [...new Set(ids)].filter(id => !(id in chargeTitles.value))
  if (!missing.length) return
  // Много неизвестных — сначала свежие начисления пачкой, остальные по одному
  if (missing.length > 10) {
    try {
      const recent = await fetchAll(params => chargesApi.list(org, params), {}, 3)
      recent.forEach(c => { found[c.id] = c.title })
      missing = missing.filter(id => !(id in found))
    } catch (_) {
      // не страшно: догрузим по одному
    }
  }
  for (let i = 0; i < missing.length; i += 10) {
    const batch = missing.slice(i, i + 10)
    const results = await Promise.allSettled(batch.map(id => chargesApi.get(org, id)))
    results.forEach((r, j) => { found[batch[j]] = r.status === 'fulfilled' ? r.value.title : '' })
  }
  chargeTitles.value = { ...chargeTitles.value, ...found }
}

// ─────────── Журнал платежей: период, статус и спортсмен — параметры сервера ───────────
const searchQuery = ref('')
const filters = reactive({ period: 'last30', method: '', status: '', athleteId: '' })

const payments = ref([])
const totalElements = ref(0)
const loading = ref(true)
const loadError = ref('')
const truncated = computed(() => payments.value.length < totalElements.value)

let loadSeq = 0
async function load({ silent = false } = {}) {
  const seq = ++loadSeq
  if (!silent) loading.value = true
  loadError.value = ''
  try {
    const org = getOrganizationId()
    const { from, to } = periodBounds(filters.period)
    const params = { from, to, status: filters.status || undefined, athleteId: filters.athleteId || undefined }
    const first = await paymentsApi.list(org, { ...params, page: 0, size: 100 })
    const pages = Math.min(first.totalPages || 1, MAX_PAGES)
    const rest = await Promise.all(
      Array.from({ length: pages - 1 }, (_, i) => paymentsApi.list(org, { ...params, page: i + 1, size: 100 }))
    )
    if (seq !== loadSeq) return
    payments.value = rest.reduce((all, page) => all.concat(page.items || []), first.items || [])
    totalElements.value = first.totalElements || 0
    loadChargeTitles(payments.value.map(p => p.chargeId))
  } catch (e) {
    if (seq !== loadSeq) return
    payments.value = []
    totalElements.value = 0
    loadError.value = errorText(e)
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// Способа оплаты и текстового поиска нет среди параметров GET /payments — фильтруем загруженное
const filteredPayments = computed(() => {
  let result = payments.value
  if (filters.method) result = result.filter(p => p.method === filters.method)
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    result = result.filter(p =>
      athleteName(p.athleteId).toLowerCase().includes(q) ||
      chargeTitle(p.chargeId).toLowerCase().includes(q) ||
      label('paymentMethod', p.method).toLowerCase().includes(q) ||
      (p.comment || '').toLowerCase().includes(q)
    )
  }
  return result
})

const emptyMessage = computed(() => {
  if (searchQuery.value.trim() || filters.method || filters.status || filters.athleteId) {
    return 'Платежей по выбранным фильтрам не найдено'
  }
  return 'За выбранный период платежей нет'
})

// Итоги — только проведённые платежи; «через банк» — СБП и перевод
const totals = computed(() => {
  const active = filteredPayments.value.filter(p => p.status === 'ACTIVE')
  const byMethod = methods => sumMoney(active.filter(p => methods.includes(p.method)), 'amount')
  return {
    all: sumMoney(active, 'amount'),
    bank: byMethod(['SBP', 'TRANSFER']),
    cash: byMethod(['CASH']),
    other: byMethod(['OTHER'])
  }
})

watch(() => [filters.period, filters.status, filters.athleteId], () => load())

// ─────────── Приём оплаты ───────────
const showModal = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive({ athleteId: '', chargeId: '', amount: '', paidOn: '', method: 'SBP', comment: '' })

// Неоплаченные действующие начисления выбранного спортсмена
const openCharges = ref([])
const openChargesLoading = ref(false)
const openChargesError = ref('')
let chargesSeq = 0

async function loadOpenCharges(athleteId) {
  const seq = ++chargesSeq
  openCharges.value = []
  openChargesError.value = ''
  if (!athleteId) return
  openChargesLoading.value = true
  try {
    const items = await fetchAll(params => chargesApi.list(getOrganizationId(), params), { athleteId, status: 'ACTIVE' })
    if (seq !== chargesSeq) return
    openCharges.value = items
      .filter(c => c.paymentStatus !== 'PAID')
      .sort((a, b) => String(a.dueOn).localeCompare(String(b.dueOn)))
    if (openCharges.value.length === 1) form.chargeId = openCharges.value[0].id
  } catch (e) {
    if (seq === chargesSeq) openChargesError.value = errorText(e)
  } finally {
    if (seq === chargesSeq) openChargesLoading.value = false
  }
}

const chargePlaceholder = computed(() => {
  if (!form.athleteId) return 'Сначала выберите спортсмена'
  if (openChargesLoading.value) return 'Загрузка…'
  if (openChargesError.value) return `Ошибка: ${openChargesError.value}`
  return openCharges.value.length ? 'Выберите начисление' : 'Неоплаченных начислений нет'
})

watch(() => form.athleteId, athleteId => {
  form.chargeId = ''
  loadOpenCharges(athleteId)
})

// Сумма по умолчанию — остаток выбранного начисления
watch(() => form.chargeId, id => {
  const charge = openCharges.value.find(c => c.id === id)
  form.amount = charge ? plainAmount(charge.remainingAmount) : ''
})

function openCreate() {
  const previousAthleteId = form.athleteId
  Object.assign(form, {
    athleteId: filters.athleteId || '',
    chargeId: '',
    amount: '',
    paidOn: toIsoDate(new Date()),
    method: 'SBP',
    comment: ''
  })
  formError.value = ''
  showModal.value = true
  // Тот же спортсмен — наблюдатель не сработает, обновляем список начислений сами
  if (form.athleteId && form.athleteId === previousAthleteId) loadOpenCharges(form.athleteId)
}

async function handleSubmit() {
  if (saving.value) return
  const charge = openCharges.value.find(c => c.id === form.chargeId)
  const amount = toMoney(form.amount)
  if (!form.athleteId) return fail('Выберите спортсмена.')
  if (!charge) return fail('Выберите начисление, за которое получена оплата.')
  if (!amount || Number(amount) <= 0) return fail('Сумма должна быть больше нуля.')
  if (Number(amount) > Number(charge.remainingAmount)) {
    return fail(`Сумма больше остатка по начислению (${formatMoney(charge.remainingAmount)}).`)
  }
  if (!form.paidOn) return fail('Укажите дату оплаты.')
  if (form.paidOn > toIsoDate(new Date())) return fail('Дата оплаты не может быть в будущем.')

  saving.value = true
  formError.value = ''
  try {
    // PaymentWrite: chargeId, amount, paidOn, method, comment
    await paymentsApi.create(getOrganizationId(), {
      chargeId: charge.id,
      amount,
      paidOn: form.paidOn,
      method: form.method,
      comment: form.comment.trim() || null
    })
    chargeTitles.value = { ...chargeTitles.value, [charge.id]: charge.title }
    showModal.value = false
    await load({ silent: true })
  } catch (e) {
    formError.value = formErrorText(e)
  } finally {
    saving.value = false
  }
}

// ─────────── Аннулирование (причина обязательна) ───────────
const showVoid = ref(false)
const voiding = ref(null)
const voidReason = ref('')

function openVoid(payment) {
  voiding.value = payment
  voidReason.value = ''
  formError.value = ''
  showVoid.value = true
}

async function submitVoid() {
  if (saving.value) return
  const reason = voidReason.value.trim()
  if (!reason) return fail('Укажите причину аннулирования.')

  saving.value = true
  formError.value = ''
  try {
    await paymentsApi.void(getOrganizationId(), voiding.value.id, reason)
    showVoid.value = false
    await load({ silent: true })
  } catch (e) {
    formError.value = formErrorText(e)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  loadAthletes()
})

// ─────────── Помощники ───────────
function fail(message) {
  formError.value = message
  return false
}

// '3400.00' → '3400' для поля ввода
function plainAmount(value) {
  const n = Number(value)
  return isNaN(n) ? '' : String(n)
}

// Сумма денежных строк API без ошибок округления
function sumMoney(items, field) {
  return items.reduce((s, item) => s + Math.round(Number(item[field] || 0) * 100), 0) / 100
}

// Все страницы списка (size ≤ 100 по контракту), не больше maxPages запросов
async function fetchAll(request, params = {}, maxPages = MAX_PAGES) {
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
.toolbar { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 16px; }
.filters { display: flex; gap: 12px; flex-wrap: wrap; }
.filter-select {
  padding: 10px 16px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 13px; color: #6D7D79; cursor: pointer; outline: none;
}
.table-card { background: white; border: 1px solid #E3EAE8; border-radius: 16px; overflow-x: auto; }
.table-header, .table-row {
  display: flex; align-items: center; gap: 16px;
  padding: 12px 24px; min-width: 1000px;
}
.table-header {
  background: #F4F7F8; border-bottom: 1px solid #E3EAE8;
  font-size: 12px; font-weight: 700; color: #6D7D79; text-transform: uppercase;
}
.table-row { padding: 16px 24px; border-bottom: 1px solid #E3EAE8; font-size: 13px; }
.col { color: #152421; }
.col:not(.status) { flex-shrink: 0; } /* колонки не сжимаются из-за кнопки в строке */
.col.num { width: 40px; color: #6D7D79; }
.col.date { width: 100px; color: #6D7D79; }
.col.name { width: 220px; font-weight: 600; font-size: 14px; }
.col.amount { width: 120px; color: #2E8B57; font-weight: 700; font-size: 14px; }
.col.method { width: 140px; color: #6D7D79; }
.col.basis { width: 240px; color: #6D7D79; }
.col.status { flex: 1; min-width: 220px; display: flex; align-items: center; gap: 8px; white-space: nowrap; }
.table-row.voided .col.amount { color: #98A6A2; text-decoration: line-through; }
.basis-comment { color: #98A6A2; }
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 700; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.status-gray { background: #EEF1F0; color: #888888; }
.void-btn { padding: 4px 10px; background: #FCE2E5; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; color: #D64545; cursor: pointer; }
.table-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 24px; background: #F4F7F8;
  font-size: 13px; flex-wrap: wrap; gap: 12px;
}
.footer-total { color: #152421; }
.footer-total strong { font-weight: 800; color: #2E8B57; }
.footer-detail { color: #6D7D79; font-size: 12px; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.field-control { padding: 10px 12px; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 14px; background: white; cursor: pointer; outline: none; }
.field-control:disabled { background: #F4F7F8; color: #6D7D79; cursor: default; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.info-block { padding: 12px; background: #F4F7F8; border-radius: 8px; }
.info-row { display: flex; justify-content: space-between; gap: 12px; font-size: 13px; margin-bottom: 4px; }
.form-hint { font-size: 12px; color: #6D7D79; }
.form-error { font-size: 13px; color: #D64545; }
</style>
