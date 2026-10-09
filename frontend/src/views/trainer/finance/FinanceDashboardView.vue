<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Финансы"
        subtitle="Сводный обзор финансового состояния ваших групп"
        v-model="searchQuery"
        search-placeholder="Поиск операции..."
      />

      <FinanceTabs />

      <section class="stats-row">
        <StatCard label="Общий баланс" value="—" subtext="Расчёт баланса появится позже" />
        <StatCard label="Начислено за месяц" :value="stats.charged" :subtext="stats.chargedSubtext" />
        <StatCard label="Оплачено за месяц" :value="stats.paid" :trend="stats.collectability" :subtext="stats.paidSubtext" />
        <StatCard label="Задолженность" :value="stats.debt" :trend="stats.overdue" :trend-up="false" :subtext="stats.debtSubtext" />
      </section>

      <section class="content-grid">
        <div class="card chart-card">
          <h2>Динамика доходов за 6 месяцев</h2>
          <StateBlock v-if="chartLoading" kind="loading" />
          <StateBlock v-else-if="chartError" kind="error" :message="chartError" />
          <StateBlock v-else-if="!hasIncome" kind="empty" message="Поступлений за последние 6 месяцев не было" />
          <div v-else class="bar-chart">
            <div v-for="bar in chartData" :key="bar.key" class="bar-item">
              <span class="bar-value">{{ bar.value }}</span>
              <div class="bar" :style="{ height: bar.height + 'px' }"></div>
              <span class="bar-label">{{ bar.label }}</span>
            </div>
          </div>
        </div>

        <div class="card operations-card">
          <h2>Последние операции</h2>
          <StateBlock v-if="operationsLoading" kind="loading" />
          <StateBlock v-else-if="operationsError" kind="error" :message="operationsError" />
          <StateBlock v-else-if="operations.length === 0" kind="empty" message="Платежей и просроченных начислений пока нет" />
          <div v-else-if="filteredOperations.length === 0" class="empty-state">
            Операций по запросу «{{ searchQuery }}» не найдено
          </div>
          <div v-else class="operations-list">
            <div v-for="op in filteredOperations" :key="op.id" class="operation-item">
              <div class="op-info">
                <div class="op-name">{{ op.name }}</div>
                <div class="op-desc">{{ op.desc }}</div>
              </div>
              <div class="op-right">
                <div class="op-amount" :class="{ negative: op.amount.startsWith('-') }">
                  {{ op.amount }}
                </div>
                <span class="status-badge" :class="op.statusClass">{{ op.status }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import Sidebar from '../../../components/layout/Sidebar.vue'
import PageHeader from '../../../components/layout/PageHeader.vue'
import StatCard from '../../../components/layout/StatCard.vue'
import FinanceTabs from '../../../components/layout/FinanceTabs.vue'
import StateBlock from '../../../components/ui/StateBlock.vue'
import { financeApi } from '../../../api/finance'
import { chargesApi } from '../../../api/charges'
import { paymentsApi } from '../../../api/payments'
import { athletesApi } from '../../../api/athletes'
import { getOrganizationId } from '../../../utils/session'
import { errorText, formatDateShort, formatMoney, fullName, toIsoDate } from '../../../utils/format'
import { label, tone } from '../../../utils/labels'

const MONTHS_SHORT = ['Янв', 'Фев', 'Мар', 'Апр', 'Май', 'Июн', 'Июл', 'Авг', 'Сен', 'Окт', 'Ноя', 'Дек']
const CHART_MONTHS = 6
const BAR_MAX_HEIGHT = 140
const RECENT_PAYMENTS = 6
const RECENT_OVERDUE = 5

// ─────────── Поиск (по уже загруженной ленте операций) ───────────
const searchQuery = ref('')

// ─────────── Сводка: GET /finance/summary по месяцам ───────────
// receivedAmount — платежи с paidOn в периоде; outstandingAmount/overdueAmount — текущие долги (от периода не зависят)
const summary = ref(null)
const chartData = ref([])
const chartLoading = ref(true)
const chartError = ref('')
const hasIncome = computed(() => chartData.value.some(bar => bar.amount > 0))

async function loadSummaries() {
  const org = getOrganizationId()
  const months = Array.from({ length: CHART_MONTHS }, (_, i) => monthRange(i - CHART_MONTHS + 1))
  try {
    const results = await Promise.all(months.map(m => financeApi.summary(org, m.from, m.to)))
    const amounts = results.map(r => Number(r.receivedAmount) || 0)
    const max = Math.max(...amounts)
    chartData.value = months.map((m, i) => ({
      key: m.from,
      amount: amounts[i],
      value: formatMoney(amounts[i]),
      height: max > 0 && amounts[i] > 0 ? Math.max(4, Math.round(amounts[i] / max * BAR_MAX_HEIGHT)) : 0,
      label: MONTHS_SHORT[m.month]
    }))
    summary.value = results[results.length - 1]
  } catch (e) {
    chartError.value = errorText(e)
  } finally {
    chartLoading.value = false
  }
}

// ─────────── Начислено за месяц: действующие начисления со сроком оплаты в текущем месяце ───────────
const monthCharges = ref(null)
const monthChargesError = ref('')

async function loadMonthCharges() {
  const { from, to } = monthRange(0)
  try {
    const items = await fetchAll(params => chargesApi.list(getOrganizationId(), params),
      { status: 'ACTIVE', dueFrom: from, dueTo: to })
    monthCharges.value = {
      amount: sumMoney(items, 'amount'),
      paid: sumMoney(items, 'paidAmount'),
      athletes: new Set(items.map(c => c.athleteId)).size
    }
  } catch (e) {
    monthChargesError.value = errorText(e)
  }
}

// ─────────── Карточки ───────────
const stats = computed(() => {
  const empty = chartLoading.value ? '…' : '—'
  const result = {
    charged: monthCharges.value ? formatMoney(monthCharges.value.amount) : (monthChargesError.value ? '—' : '…'),
    chargedSubtext: monthChargesError.value ? 'Не удалось загрузить' : '',
    paid: empty, collectability: '', paidSubtext: chartError.value ? 'Не удалось загрузить' : '',
    debt: empty, overdue: '', debtSubtext: chartError.value ? 'Не удалось загрузить' : ''
  }
  // StatCard показывает trend только вместе с subtext; тексты короткие, чтобы строка не переносилась
  const m = monthCharges.value
  if (m) {
    result.chargedSubtext = `${m.athletes} ${plural(m.athletes, 'спортсмен', 'спортсмена', 'спортсменов')}`
    if (m.amount > 0) {
      result.collectability = `${Math.round(m.paid / m.amount * 100)}%`
      result.paidSubtext = 'от начисленного'
    }
  }
  const s = summary.value
  if (s) {
    result.paid = formatMoney(s.receivedAmount)
    result.debt = formatMoney(s.outstandingAmount)
    if (Number(s.overdueAmount) > 0) {
      result.overdue = formatMoney(s.overdueAmount)
      result.debtSubtext = 'просрочено'
    } else {
      result.debtSubtext = Number(s.outstandingAmount) > 0 ? 'Просроченных долгов нет' : 'Долгов нет'
    }
  }
  return result
})

// ─────────── Последние операции: платежи + просроченные начисления ───────────
const operations = ref([])
const operationsLoading = ref(true)
const operationsError = ref('')

async function loadOperations() {
  const org = getOrganizationId()
  try {
    const [paymentsPage, overduePage, athletes] = await Promise.all([
      paymentsApi.list(org, { size: RECENT_PAYMENTS }),
      chargesApi.list(org, { isOverdue: true, status: 'ACTIVE', size: RECENT_OVERDUE }),
      // Имена нужны только для подписей: без них лента всё равно показывается
      fetchAll(params => athletesApi.list(org, params)).catch(() => [])
    ])
    const payments = paymentsPage.items || []
    const overdue = overduePage.items || []

    const names = Object.fromEntries(athletes.map(a => [a.id, fullName(a)]))
    // В Payment нет названия начисления: берём его из GET /charges/{id}
    const titles = Object.fromEntries(overdue.map(c => [c.id, c.title]))
    const missing = [...new Set(payments.map(p => p.chargeId))].filter(id => !(id in titles))
    await Promise.all(missing.map(async id => {
      try {
        titles[id] = (await chargesApi.get(org, id)).title
      } catch (_) {
        titles[id] = ''
      }
    }))

    operations.value = [
      ...payments.map(p => ({
        id: `payment-${p.id}`,
        date: p.paidOn,
        name: names[p.athleteId] || '—',
        desc: `Оплата: ${titles[p.chargeId] || 'начисление'} · ${formatDateShort(p.paidOn)}`,
        amount: p.status === 'ACTIVE' ? `+${formatMoney(p.amount)}` : formatMoney(p.amount),
        status: label('paymentRecordStatus', p.status),
        statusClass: `status-${tone(p.status)}`
      })),
      ...overdue.map(c => ({
        id: `charge-${c.id}`,
        date: c.dueOn,
        name: names[c.athleteId] || '—',
        desc: `${c.title} · срок ${formatDateShort(c.dueOn)}`,
        amount: formatMoney(c.remainingAmount),
        status: 'Просрочено',
        statusClass: 'status-red'
      }))
    ].sort((a, b) => String(b.date || '').localeCompare(String(a.date || '')))
  } catch (e) {
    operationsError.value = errorText(e)
  } finally {
    operationsLoading.value = false
  }
}

// ─────────── Фильтр операций по поиску ───────────
const filteredOperations = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return operations.value
  return operations.value.filter(op =>
    op.name.toLowerCase().includes(q) ||
    op.desc.toLowerCase().includes(q) ||
    op.status.toLowerCase().includes(q)
  )
})

onMounted(() => {
  loadSummaries()
  loadMonthCharges()
  loadOperations()
})

// ─────────── Помощники ───────────
// Месяц со сдвигом от текущего: границы включительно в формате 'YYYY-MM-DD'
function monthRange(offset) {
  const now = new Date()
  const first = new Date(now.getFullYear(), now.getMonth() + offset, 1)
  const last = new Date(first.getFullYear(), first.getMonth() + 1, 0)
  return { from: toIsoDate(first), to: toIsoDate(last), month: first.getMonth() }
}

// Сумма денежных строк API ('1250.00') без ошибок округления
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

function plural(n, one, few, many) {
  const mod10 = n % 10
  const mod100 = n % 100
  if (mod10 === 1 && mod100 !== 11) return one
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few
  return many
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
.content-grid { display: grid; grid-template-columns: 1fr 500px; gap: 24px; }
@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }
.card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.card h2 { font-size: 18px; font-weight: 700; color: #152421; }
.bar-chart { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; height: 200px; }
.bar-item { display: flex; flex-direction: column; align-items: center; gap: 8px; flex: 1; }
.bar-value { font-size: 11px; font-weight: 600; color: #6D7D79; }
.bar { width: 32px; background: #102522; border-radius: 6px 6px 0 0; transition: height 0.3s; }
.bar-label { font-size: 12px; color: #98A6A2; }
.operations-list { display: flex; flex-direction: column; gap: 12px; }
.operation-item {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 12px; border-bottom: 1px solid #E3EAE8;
}
.operation-item:last-child { border-bottom: none; }
.op-name { font-size: 14px; font-weight: 600; color: #152421; }
.op-desc { font-size: 12px; color: #6D7D79; margin-top: 4px; }
.op-right { display: flex; align-items: center; gap: 12px; }
.op-amount { font-size: 14px; font-weight: 700; color: #152421; }
.op-amount.negative { color: #D64545; }
.status-badge { padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 700; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-red { background: #FEE2E2; color: #D64545; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.status-gray { background: #EEF1F0; color: #888888; }
.empty-state {
  padding: 30px 20px;
  text-align: center;
  color: #98A6A2;
  font-size: 13px;
  border: 1px dashed #E3EAE8;
  border-radius: 12px;
}
</style>
