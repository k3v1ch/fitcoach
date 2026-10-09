<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        title="Оплаты и Финансы"
        subtitle="Баланс по абонементам, счетам за сборы и история платежей"
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
        </template>
      </PageHeader>

      <div v-if="pageState" class="card">
        <StateBlock :kind="pageState.kind" :message="pageState.message" />
      </div>

      <template v-else>
        <section class="metrics-grid">
          <MetricCard :label="`Оплачено в ${year} году`" :value="stats.paid" :note="stats.paidNote" icon="credit-card" color="#E9F7D5" icon-color="#2E8B57" />
          <MetricCard label="Ожидает оплаты" :value="stats.pending" :note="stats.pendingNote" icon="bell" color="#FFF1D6" icon-color="#F2B705" />
          <MetricCard
            label="Задолженность"
            :value="stats.debt"
            :note="stats.debtNote"
            :icon="stats.overdue ? 'alert' : 'check'"
            :color="stats.overdue ? '#FCE2E5' : '#E9F7D5'"
            :icon-color="stats.overdue ? '#D64545' : '#2E8B57'"
          />
        </section>

        <div class="card">
          <div class="table-header-actions">
            <div class="left-filters">
              <button
                v-for="tab in tabs" :key="tab.id"
                class="tab-btn"
                :class="{ active: activeTab === tab.id }"
                @click="activeTab = tab.id"
              >
                {{ tab.label }} ({{ counts[tab.id] }})
              </button>
            </div>
            <button class="btn-outline" :disabled="loading" @click="refresh">{{ loading ? 'Обновление…' : 'Обновить' }}</button>
          </div>

          <p v-if="refreshError" class="form-error" role="alert">Не удалось обновить: {{ refreshError }}</p>

          <StateBlock v-if="!rows.length" kind="empty" :message="emptyText" />
          <div v-else class="table">
            <div class="table-head">
              <div class="col-date">{{ activeTab === 'payments' ? 'ДАТА ОПЛАТЫ' : 'СРОК' }}</div>
              <div class="col-purpose">НАЗНАЧЕНИЕ</div>
              <div class="col-amount">СУММА</div>
              <div class="col-status">СТАТУС</div>
              <div class="col-action">ДЕЙСТВИЕ</div>
            </div>

            <div v-for="row in rows" :key="row.id" class="table-row">
              <div class="col-date">{{ row.date }}</div>
              <div class="col-purpose">
                <div class="purpose-title">{{ row.title }}</div>
                <div class="purpose-meta">{{ row.meta }}</div>
              </div>
              <div class="col-amount">{{ row.amount }}</div>
              <div class="col-status">
                <span class="badge" :class="row.tone">{{ row.status }}</span>
              </div>
              <div class="col-action">
                <button v-if="row.action === 'pay'" class="btn-pay" @click="openHowToPay(row.charge)">
                  Как оплатить
                </button>
                <button v-else-if="row.action === 'history'" class="btn-receipt" @click="openHistory(row.charge)">
                  Платежи
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- Онлайн-оплаты нет: родитель передаёт деньги тренеру, платёж записывает тренер (payments.write) -->
      <BaseModal
        v-model="showModal"
        title="Как оплатить начисление"
        submit-label="Понятно"
        @submit="showModal = false"
      >
        <div v-if="selectedCharge" class="payment-modal">
          <div class="info-row">
            <span class="info-label">Начисление</span>
            <span class="info-value">{{ selectedCharge.title }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Остаток</span>
            <span class="info-value">{{ formatMoney(selectedCharge.remainingAmount) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Срок оплаты</span>
            <span class="info-value" :class="{ 'info-value--red': selectedCharge.overdue }">
              {{ formatDate(selectedCharge.dueOn) }}{{ selectedCharge.overdue ? ' — просрочено' : '' }}
            </span>
          </div>
          <p class="pay-hint">
            Оплатить в кабинете пока нельзя. Передайте деньги тренеру или переведите по реквизитам организации —
            после получения тренер отметит платёж, и статус начисления обновится.
          </p>
          <div class="org-box">
            <div class="info-row">
              <span class="info-label">Организация</span>
              <span class="info-value">{{ organization?.name || currentOrganization?.organizationName || '—' }}</span>
            </div>
            <div v-if="organization?.address" class="info-row">
              <span class="info-label">Адрес</span>
              <span class="info-value">{{ organization.address }}</span>
            </div>
            <p v-if="orgLoading" class="pay-hint">Загружаем сведения организации…</p>
            <p v-else-if="orgError" class="form-error">Не удалось загрузить сведения организации: {{ orgError }}</p>
            <p v-else-if="organization?.description" class="org-description">{{ organization.description }}</p>
            <p v-else class="pay-hint">Реквизиты для перевода уточните у тренера.</p>
          </div>
        </div>
      </BaseModal>

      <BaseModal
        v-model="showHistory"
        title="Платежи по начислению"
        submit-label="Закрыть"
        @submit="showHistory = false"
      >
        <div v-if="historyCharge" class="payment-modal">
          <div class="info-row">
            <span class="info-label">Начисление</span>
            <span class="info-value">{{ historyCharge.title }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Сумма</span>
            <span class="info-value">{{ formatMoney(historyCharge.amount) }}</span>
          </div>
          <div v-for="p in historyPayments" :key="p.id" class="history-row">
            <div class="history-copy">
              <span class="info-value">{{ formatDate(p.paidOn) }} · {{ label('paymentMethod', p.method) }}</span>
              <span v-if="p.comment || p.voidReason" class="history-note">
                {{ p.status === 'VOIDED' ? `Аннулирован: ${p.voidReason || 'без причины'}` : p.comment }}
              </span>
            </div>
            <span class="history-amount" :class="{ 'history-amount--void': p.status === 'VOIDED' }">{{ formatMoney(p.amount) }}</span>
          </div>
        </div>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import MetricCard from '../../components/layout/MetricCard.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { chargesApi } from '../../api/charges'
import { paymentsApi } from '../../api/payments'
import { organizationsApi } from '../../api/organizations'
import {
  currentOrganization, myAthletes, selectedAthleteId, selectedAthlete, loadMyAthletes, selectAthlete,
  getOrganizationId, hasPermission
} from '../../utils/session'
import { formatDate, formatDateShort, formatMoney, fullName, initials, errorText } from '../../utils/format'
import { label, tone } from '../../utils/labels'

const NO_CHILD = 'Ребёнок ещё не привязан — обратитесь к тренеру'
const year = new Date().getFullYear()

// История платежей видна при payments.read (у родителя оно есть по умолчанию)
const canSeePayments = computed(() => hasPermission('payments.read'))

const tabs = computed(() => [
  { id: 'all', label: 'Все' },
  { id: 'unpaid', label: 'К оплате' },
  { id: 'paid', label: 'Оплаченные' },
  ...(canSeePayments.value ? [{ id: 'payments', label: 'История платежей' }] : [])
])
const activeTab = ref('all')

function plural(n, one, few, many) {
  const m10 = n % 10
  const m100 = n % 100
  if (m10 === 1 && m100 !== 11) return one
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return few
  return many
}

// Сумма денежных полей в копейках — без ошибок округления
const sumMoney = (list, key) => list.reduce((s, x) => s + Math.round(Number(x[key] || 0) * 100), 0) / 100

// ─────────── загрузка ───────────

const childrenReady = ref(false)
const childrenError = ref(null)

const charges = ref([])
const payments = ref([])
const loading = ref(false)
const loaded = ref(false)     // данные выбранного ребёнка уже показаны: «Обновить» не скрывает таблицу
const error = ref(null)
const refreshError = ref('')

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

let loadSeq = 0
async function load({ keep = false } = {}) {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  error.value = null
  refreshError.value = ''
  if (!keep) {
    charges.value = []
    payments.value = []
    loaded.value = false
  }
  if (!athleteId) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const org = getOrganizationId()
    // Родитель видит только начисления и платежи своих детей; athleteId выбирает ребёнка
    const [chargeList, paymentList] = await Promise.all([
      fetchAll(p => chargesApi.list(org, { athleteId, ...p })),
      canSeePayments.value ? fetchAll(p => paymentsApi.list(org, { athleteId, ...p })) : Promise.resolve([])
    ])
    if (seq !== loadSeq) return
    charges.value = chargeList
    payments.value = paymentList
    loaded.value = true
  } catch (e) {
    if (seq !== loadSeq) return
    if (keep && loaded.value) refreshError.value = errorText(e)
    else error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

const refresh = () => load({ keep: true })

// Сведения организации для подсказки об оплате: название, адрес, описание (реквизиты указывает тренер)
const organization = ref(null)
const orgLoading = ref(false)
const orgError = ref('')

async function loadOrganization() {
  orgLoading.value = true
  orgError.value = ''
  try {
    organization.value = await organizationsApi.get(getOrganizationId())
  } catch (e) {
    orgError.value = errorText(e)
  } finally {
    orgLoading.value = false
  }
}

onMounted(async () => {
  loadOrganization()
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

const pageState = computed(() => {
  if (!childrenReady.value) return { kind: 'loading', message: '' }
  if (childrenError.value) return { kind: 'error', message: errorText(childrenError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CHILD }
  if (loading.value && !loaded.value) return { kind: 'loading', message: '' }
  if (error.value) return { kind: 'error', message: errorText(error.value) }
  return null
})

// ─────────── метрики ───────────

const isActive = c => c.status === 'ACTIVE'
const isUnpaid = c => isActive(c) && Number(c.remainingAmount) > 0
const isPaid = c => isActive(c) && c.paymentStatus === 'PAID'

const stats = computed(() => {
  const pending = charges.value.filter(c => isUnpaid(c) && !c.overdue)
  const overdue = charges.value.filter(c => isUnpaid(c) && c.overdue)
  const paidThisYear = payments.value.filter(p => p.status === 'ACTIVE' && String(p.paidOn).startsWith(`${year}-`))
  const nearest = pending.map(c => c.dueOn).sort()[0]
  return {
    paid: canSeePayments.value ? formatMoney(sumMoney(paidThisYear, 'amount')) : '—',
    paidNote: canSeePayments.value
      ? `${paidThisYear.length} ${plural(paidThisYear.length, 'платёж', 'платежа', 'платежей')} за год`
      : 'История платежей недоступна',
    pending: formatMoney(sumMoney(pending, 'remainingAmount')),
    pendingNote: pending.length
      ? `${pending.length} ${plural(pending.length, 'начисление', 'начисления', 'начислений')} · ближайший срок ${formatDate(nearest, { withYear: false })}`
      : 'Нет начислений к оплате',
    debt: formatMoney(sumMoney(overdue, 'remainingAmount')),
    debtNote: overdue.length
      ? `Срок прошёл: ${overdue.length} ${plural(overdue.length, 'начисление', 'начисления', 'начислений')}`
      : 'Просроченных начислений нет',
    overdue: overdue.length > 0
  }
})

// ─────────── таблица ───────────

const counts = computed(() => ({
  all: charges.value.length,
  unpaid: charges.value.filter(isUnpaid).length,
  paid: charges.value.filter(isPaid).length,
  payments: payments.value.length
}))

const paymentsByCharge = computed(() => {
  const map = new Map()
  for (const p of payments.value) {
    if (!map.has(p.chargeId)) map.set(p.chargeId, [])
    map.get(p.chargeId).push(p)
  }
  return map
})

const chargeById = computed(() => new Map(charges.value.map(c => [c.id, c])))

function periodText(c) {
  if (!c.periodFrom || !c.periodTo) return ''
  return `за ${formatDateShort(c.periodFrom)} – ${formatDateShort(c.periodTo)}`
}

function chargeRow(c) {
  const cancelled = c.status === 'CANCELLED'
  const meta = cancelled
    ? `Отменено${c.cancelReason ? `: ${c.cancelReason}` : ''}`
    : [
        label('chargeType', c.type),
        periodText(c),
        `Оплачено ${formatMoney(c.paidAmount)}`,
        `Остаток ${formatMoney(c.remainingAmount)}`,
        c.comment
      ].filter(Boolean).join(' · ')
  let action = null
  if (!cancelled && Number(c.remainingAmount) > 0) action = 'pay'
  else if (paymentsByCharge.value.get(c.id)?.length) action = 'history'
  return {
    id: c.id,
    charge: c,
    date: formatDateShort(c.dueOn),
    title: c.title,
    meta,
    amount: formatMoney(c.amount),
    status: cancelled ? 'Отменено' : c.overdue ? 'Просрочено' : label('paymentStatus', c.paymentStatus),
    tone: cancelled ? 'gray' : c.overdue ? 'red' : tone(c.paymentStatus),
    action
  }
}

function paymentRow(p) {
  const voided = p.status === 'VOIDED'
  return {
    id: p.id,
    charge: chargeById.value.get(p.chargeId) || null,
    date: formatDateShort(p.paidOn),
    title: chargeById.value.get(p.chargeId)?.title || 'Начисление',
    meta: voided
      ? `Аннулирован${p.voidReason ? `: ${p.voidReason}` : ''}`
      : [label('paymentMethod', p.method), p.comment].filter(Boolean).join(' · '),
    amount: formatMoney(p.amount),
    status: label('paymentRecordStatus', p.status),
    tone: voided ? 'gray' : 'green',
    action: null
  }
}

const byDueAsc = (a, b) => String(a.dueOn).localeCompare(String(b.dueOn))
const byDueDesc = (a, b) => byDueAsc(b, a)

const rows = computed(() => {
  if (activeTab.value === 'payments') {
    return [...payments.value]
      .sort((a, b) => String(b.paidOn).localeCompare(String(a.paidOn)) || String(b.createdAt).localeCompare(String(a.createdAt)))
      .map(paymentRow)
  }
  // К оплате — от ближайшего срока, остальное — от новых к старым
  if (activeTab.value === 'unpaid') return charges.value.filter(isUnpaid).sort(byDueAsc).map(chargeRow)
  const list = activeTab.value === 'paid' ? charges.value.filter(isPaid) : [...charges.value]
  return list.sort(byDueDesc).map(chargeRow)
})

const emptyText = computed(() => ({
  all: 'Начислений пока нет',
  unpaid: 'Начислений к оплате нет',
  paid: 'Оплаченных начислений пока нет',
  payments: 'Платежей пока нет'
}[activeTab.value]))

// ─────────── подсказка об оплате и платежи начисления ───────────

const showModal = ref(false)
const selectedCharge = ref(null)

function openHowToPay(charge) {
  selectedCharge.value = charge
  if (!organization.value && !orgLoading.value) loadOrganization()
  showModal.value = true
}

const showHistory = ref(false)
const historyCharge = ref(null)
const historyPayments = computed(() => (historyCharge.value
  ? [...(paymentsByCharge.value.get(historyCharge.value.id) || [])].sort((a, b) => String(b.paidOn).localeCompare(String(a.paidOn)))
  : []))

function openHistory(charge) {
  historyCharge.value = charge
  showHistory.value = true
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.child-selector { position: relative; display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: white; border: 1px solid #E3EAE8; border-radius: 12px; font-size: 13px; font-weight: 600; color: #152421; cursor: pointer; }
.child-avatar { width: 24px; height: 24px; background: #DDECFB; color: #35678E; border-radius: 99px; display: flex; justify-content: center; align-items: center; font-size: 10px; font-weight: 700; }
/* Прозрачный нативный список поверх плашки ребёнка — переключение без смены вида */
.child-select-overlay { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; }

.metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }
.card { background: white; border-radius: 20px; padding: 24px; box-shadow: 0 8px 24px rgba(23,52,46,0.05); display: flex; flex-direction: column; gap: 20px; }
.table-header-actions { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.left-filters { display: flex; gap: 8px; flex-wrap: wrap; }
.tab-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid #E3EAE8; background: transparent; font-size: 13px; font-weight: 600; color: #6D7D79; cursor: pointer; }
.tab-btn.active { background: #102522; color: white; border-color: #102522; }
.btn-outline { padding: 10px 16px; border-radius: 12px; border: 1px solid #E3EAE8; background: transparent; font-size: 13px; font-weight: 600; color: #152421; cursor: pointer; }
.btn-outline:disabled { opacity: 0.6; cursor: default; }

.table { display: flex; flex-direction: column; }
.table-head { display: flex; padding: 12px; background: #F4F7F8; border-radius: 8px; font-size: 12px; font-weight: 700; color: #6D7D79; }
.table-row { display: flex; padding: 16px 12px; border-bottom: 1px solid #E3EAE8; align-items: center; }
.col-date { width: 120px; font-size: 13px; font-weight: 600; color: #152421; }
.col-purpose { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.purpose-title { font-size: 14px; font-weight: 600; color: #152421; }
.purpose-meta { font-size: 11px; color: #98A6A2; }
.col-amount { width: 150px; font-size: 14px; font-weight: 700; color: #152421; }
.col-status { width: 150px; }
.col-action { width: 120px; text-align: right; }
.badge { padding: 3px 10px; border-radius: 6px; font-size: 11px; font-weight: 700; display: inline-block; }
.badge.yellow { background: #FFF1D6; color: #8B6914; }
.badge.green { background: #E9F7D5; color: #2E8B57; }
.badge.red { background: #FCE2E5; color: #D64545; }
.badge.blue { background: #DDECFB; color: #3B82F6; }
.badge.gray { background: #F4F7F8; color: #888888; }
.btn-pay { padding: 8px 12px; background: #C4F000; border-radius: 8px; font-size: 12px; font-weight: 700; color: #102522; border: none; cursor: pointer; }
.btn-receipt { padding: 8px 12px; border-radius: 8px; border: 1px solid #E3EAE8; background: transparent; font-size: 12px; font-weight: 600; color: #6D7D79; cursor: pointer; }

.payment-modal { display: flex; flex-direction: column; gap: 12px; }
.info-row { display: flex; justify-content: space-between; gap: 16px; font-size: 13px; }
.info-label { color: #6D7D79; flex-shrink: 0; }
.info-value { color: #152421; font-weight: 600; text-align: right; }
.info-value--red { color: #D64545; }
.pay-hint { font-size: 13px; color: #6D7D79; line-height: 1.5; }
.org-box { display: flex; flex-direction: column; gap: 10px; padding: 14px; background: #F4F7F8; border-radius: 12px; }
.org-description { font-size: 13px; color: #152421; line-height: 1.5; white-space: pre-line; }
.form-error { font-size: 13px; color: #D64545; }
.history-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 10px 0; border-top: 1px solid #E3EAE8; }
.history-copy { display: flex; flex-direction: column; gap: 2px; }
.history-copy .info-value { text-align: left; }
.history-note { font-size: 12px; color: #98A6A2; }
.history-amount { font-size: 14px; font-weight: 700; color: #152421; white-space: nowrap; }
.history-amount--void { color: #98A6A2; text-decoration: line-through; }
</style>
