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
        <StatCard label="Общий баланс" :value="stats.balance" subtext="Доступно к выводу" />
        <StatCard label="Начислено за месяц" :value="stats.charged" :subtext="stats.athletesCount" />
        <StatCard label="Оплачено за месяц" :value="stats.paid" :trend="stats.collectability" />
        <StatCard label="Задолженность" :value="stats.debt" trend="- Требует оплаты" :trend-up="false" />
      </section>

      <section class="content-grid">
        <div class="card chart-card">
          <h2>Динамика доходов за 6 месяцев</h2>
          <div class="bar-chart">
            <div v-for="bar in chartData" :key="bar.label" class="bar-item">
              <span class="bar-value">{{ bar.value }}</span>
              <div class="bar" :style="{ height: bar.height + 'px' }"></div>
              <span class="bar-label">{{ bar.label }}</span>
            </div>
          </div>
        </div>

        <div class="card operations-card">
          <h2>Последние операции</h2>
          <div v-if="filteredOperations.length === 0" class="empty-state">
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

// ─────────── Поиск ───────────
const searchQuery = ref('')

// ─────────── Данные ───────────
// Готово к интеграции: заменить на fetch('/api/finance/dashboard')
const stats = ref({
  balance: '0 ₽',
  charged: '0 ₽',
  paid: '0 ₽',
  debt: '0 ₽',
  athletesCount: '0 спортсменов',
  collectability: ''
})

const chartData = ref([])
const operations = ref([])

// Моковые данные (убрать после подключения к API)
onMounted(() => {
  // Здесь будет: const res = await fetch(...); ...
  stats.value = {
    balance: '245 600 ₽',
    charged: '89 200 ₽',
    paid: '76 800 ₽',
    debt: '12 400 ₽',
    athletesCount: '48 спортсменов',
    collectability: '+ 86% собираемость'
  }
  chartData.value = [
    { value: '45000 ₽', height: 72, label: 'Апр' },
    { value: '68000 ₽', height: 109, label: 'Май' },
    { value: '52000 ₽', height: 83, label: 'Июн' },
    { value: '74000 ₽', height: 118, label: 'Июл' },
    { value: '89000 ₽', height: 142, label: 'Авг' },
    { value: '76800 ₽', height: 123, label: 'Сен' }
  ]
  operations.value = [
    { id: 1, name: 'Михаил Литвинов', desc: 'Оплата абонемента · 18.09.2026', amount: '+3 400 ₽', status: 'Успешно', statusClass: 'status-green' },
    { id: 2, name: 'Алина Орлова', desc: 'Разовое занятие · 17.09.2026', amount: '+1 200 ₽', status: 'Успешно', statusClass: 'status-green' },
    { id: 3, name: 'Егор Булатов (Зал ОФП)', desc: 'Аренда оборудования · 15.09.2026', amount: '-4 500 ₽', status: 'Расход', statusClass: 'status-red' },
    { id: 4, name: 'Дмитрий Рогов', desc: 'Сбор на сборы Сочи · 14.09.2026', amount: '+5 000 ₽', status: 'Успешно', statusClass: 'status-green' },
    { id: 5, name: 'Елизавета Котова', desc: 'Индивидуальная тренировка · 12.09.2026', amount: '+2 000 ₽', status: 'Ожидает', statusClass: 'status-yellow' }
  ]
})

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
.empty-state {
  padding: 30px 20px;
  text-align: center;
  color: #98A6A2;
  font-size: 13px;
  border: 1px dashed #E3EAE8;
  border-radius: 12px;
}
</style>