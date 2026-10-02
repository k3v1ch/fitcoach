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
          <div class="child-selector">
            <div class="child-avatar">АК</div>
            <span>Мой ребёнок</span>
            <BaseIcon name="chevron-down" :size="14" />
          </div>
        </template>
      </PageHeader>

      <section class="metrics-grid">
        <MetricCard label="Всего оплачено YTD" :value="stats.total" note="Успешных транзакций: {{ stats.count }}" icon="credit-card" color="#E9F7D5" icon-color="#2E8B57" />
        <MetricCard label="Ожидает оплаты" :value="stats.pending" :note="stats.pendingNote" icon="bell" color="#FFF1D6" icon-color="#F2B705" />
        <MetricCard label="Задолженность" :value="stats.debt" note="Все абонементы закрыты" icon="check" color="#E9F7D5" icon-color="#2E8B57" />
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
          <button class="btn-outline" @click="reload">Обновить</button>
        </div>

        <div v-if="loading" class="empty-state">Загрузка...</div>
        <div v-else-if="filteredCharges.length === 0" class="empty-state">Начислений нет</div>
        <div v-else class="table">
          <div class="table-head">
            <div class="col-date">ДАТА</div>
            <div class="col-purpose">НАЗНАЧЕНИЕ</div>
            <div class="col-amount">СУММА</div>
            <div class="col-status">СТАТУС</div>
            <div class="col-action">ДЕЙСТВИЕ</div>
          </div>

          <div v-for="charge in filteredCharges" :key="charge.id" class="table-row">
            <div class="col-date">{{ formatDate(charge.dueOn) }}</div>
            <div class="col-purpose">
              <div class="purpose-title">{{ charge.title }}</div>
              <div class="purpose-meta">
                Начислено: {{ formatMoney(charge.amount) }} ₽ 
                · Оплачено: {{ formatMoney(charge.paidAmount) }} ₽
                · Остаток: {{ formatMoney(charge.remainingAmount) }} ₽
              </div>
            </div>
            <div class="col-amount">{{ formatMoney(charge.amount) }} ₽</div>
            <div class="col-status">
              <span class="badge" :class="statusClass(charge.paymentStatus)">
                {{ statusLabel(charge.paymentStatus) }}
              </span>
            </div>
            <div class="col-action">
              <button 
                v-if="charge.paymentStatus !== 'PAID'" 
                class="btn-pay"
                @click="openPayment(charge)"
              >
                Оплатить
              </button>
              <button 
                v-else 
                class="btn-receipt"
                @click="downloadReceipt(charge)"
              >
                Чек PDF
              </button>
            </div>
          </div>
        </div>
      </div>

      <BaseModal 
        v-model="showModal" 
        title="Оплата начисления"
        submit-label="Записать платёж"
        @submit="submitPayment"
      >
        <div v-if="selectedCharge" class="payment-modal">
          <div class="info-row">
            <span class="info-label">Начисление</span>
            <span class="info-value">{{ selectedCharge.title }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Остаток</span>
            <span class="info-value">{{ formatMoney(selectedCharge.remainingAmount) }} ₽</span>
          </div>
          <BaseInput v-model="paymentForm.amount" label="Сумма, ₽" placeholder="1000" />
          <BaseInput v-model="paymentForm.paidOn" label="Дата (YYYY-MM-DD)" placeholder="2026-09-20" />
          <div class="field">
            <label class="field-label">Способ</label>
            <select v-model="paymentForm.method" class="role-select">
              <option value="SBP">СБП</option>
              <option value="TRANSFER">Перевод</option>
              <option value="CASH">Наличные</option>
              <option value="OTHER">Другое</option>
            </select>
          </div>
        </div>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import MetricCard from '../../components/layout/MetricCard.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import { chargesApi } from '../../api/charges'
import { paymentsApi } from '../../api/payments'
import { getOrganizationId } from '../../utils/session'

const tabs = [
  { id: 'all', label: 'Все' },
  { id: 'unpaid', label: 'К оплате' },
  { id: 'paid', label: 'Оплаченные' }
]
const activeTab = ref('all')
const charges = ref([])
const loading = ref(false)

const showModal = ref(false)
const selectedCharge = ref(null)
const paymentForm = reactive({ amount: '', paidOn: '', method: 'SBP' })

const stats = computed(() => {
  const paid = charges.value.filter(c => c.paymentStatus === 'PAID')
  const pending = charges.value.filter(c => c.paymentStatus !== 'PAID')
  const sum = arr => arr.reduce((s, c) => s + Number(c.amount || 0), 0)
  const pendingSum = pending.reduce((s, c) => s + Number(c.remainingAmount || 0), 0)
  return {
    total: sum(paid).toLocaleString('ru-RU'),
    count: paid.length,
    pending: pendingSum.toLocaleString('ru-RU'),
    pendingNote: pending.length ? `${pending.length} начислений` : 'Все закрыто',
    debt: '0'
  }
})

const counts = computed(() => ({
  all: charges.value.length,
  unpaid: charges.value.filter(c => c.paymentStatus !== 'PAID').length,
  paid: charges.value.filter(c => c.paymentStatus === 'PAID').length
}))

const filteredCharges = computed(() => {
  if (activeTab.value === 'unpaid') return charges.value.filter(c => c.paymentStatus !== 'PAID')
  if (activeTab.value === 'paid') return charges.value.filter(c => c.paymentStatus === 'PAID')
  return charges.value
})

async function reload() {
  loading.value = true
  try {
    const res = await chargesApi.list(getOrganizationId(), { size: 100 })
    charges.value = res.items || res
  } catch (e) {
    console.warn('Не удалось загрузить начисления, используем демо:', e.message)
    charges.value = demoTimes()
  } finally {
    loading.value = false
  }
}

function demoTimes() {
  return [
    { id: '1', title: 'Абонемент за Сентябрь', dueOn: '2026-09-25', amount: 12000, paidAmount: 12000, remainingAmount: 0, paymentStatus: 'PAID' },
    { id: '2', title: 'Сбор в Сочи', dueOn: '2026-09-25', amount: 45000, paidAmount: 0, remainingAmount: 45000, paymentStatus: 'UNPAID' },
    { id: '3', title: 'Индивидуальные занятия', dueOn: '2026-10-01', amount: 8000, paidAmount: 3000, remainingAmount: 5000, paymentStatus: 'PARTIALLY_PAID' }
  ]
}

function openPayment(charge) {
  selectedCharge.value = charge
  paymentForm.amount = String(charge.remainingAmount)
  paymentForm.paidOn = new Date().toISOString().slice(0, 10)
  paymentForm.method = 'SBP'
  showModal.value = true
}

async function submitPayment() {
  try {
    await paymentsApi.create(getOrganizationId(), {
      chargeId: selectedCharge.value.id,
      amount: paymentForm.amount,
      paidOn: paymentForm.paidOn,
      method: paymentForm.method
    })
    showModal.value = false
    await reload()
  } catch (e) {
    alert(`Ошибка: ${e.message}`)
  }
}

function downloadReceipt(charge) {
  alert(`Скачивание чека: ${charge.title}`)
}

function formatMoney(n) {
  return Number(n || 0).toLocaleString('ru-RU')
}
function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  return dt.toLocaleDateString('ru-RU')
}
function statusLabel(s) {
  return { PAID: 'Оплачено', UNPAID: 'Не оплачено', PARTIALLY_PAID: 'Частично' }[s] || s
}
function statusClass(s) {
  return { PAID: 'green', UNPAID: 'red', PARTIALLY_PAID: 'yellow' }[s] || 'yellow'
}

onMounted(reload)
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.child-selector { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: white; border: 1px solid #E3EAE8; border-radius: 12px; font-size: 13px; font-weight: 600; color: #152421; cursor: pointer; }
.child-avatar { width: 24px; height: 24px; background: #DDECFB; color: #35678E; border-radius: 99px; display: flex; justify-content: center; align-items: center; font-size: 10px; font-weight: 700; }

.metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }
.card { background: white; border-radius: 20px; padding: 24px; box-shadow: 0 8px 24px rgba(23,52,46,0.05); display: flex; flex-direction: column; gap: 20px; }
.table-header-actions { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.left-filters { display: flex; gap: 8px; flex-wrap: wrap; }
.tab-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid #E3EAE8; background: transparent; font-size: 13px; font-weight: 600; color: #6D7D79; cursor: pointer; }
.tab-btn.active { background: #102522; color: white; border-color: #102522; }
.btn-outline { padding: 10px 16px; border-radius: 12px; border: 1px solid #E3EAE8; background: transparent; font-size: 13px; font-weight: 600; color: #152421; cursor: pointer; }

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
.btn-pay { padding: 8px 12px; background: #C4F000; border-radius: 8px; font-size: 12px; font-weight: 700; color: #102522; border: none; cursor: pointer; }
.btn-receipt { padding: 8px 12px; border-radius: 8px; border: 1px solid #E3EAE8; background: transparent; font-size: 12px; font-weight: 600; color: #6D7D79; cursor: pointer; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; }

.payment-modal { display: flex; flex-direction: column; gap: 12px; }
.info-row { display: flex; justify-content: space-between; font-size: 13px; }
.info-label { color: #6D7D79; }
.info-value { color: #152421; font-weight: 600; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.role-select { padding: 10px 12px; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 14px; background: white; cursor: pointer; outline: none; }
</style>