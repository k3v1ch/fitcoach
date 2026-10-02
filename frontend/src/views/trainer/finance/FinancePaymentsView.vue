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
          <BaseButton @click="showModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Принять оплату
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <div class="toolbar">
        <div class="filters">
          <select v-model="filters.period" class="filter-select">
            <option>Последние 30 дней</option>
            <option>Сентябрь 2026</option>
            <option>Август 2026</option>
          </select>
          <select v-model="filters.method" class="filter-select">
            <option value="">Все способы</option>
            <option>Карта</option>
            <option>Перевод</option>
            <option>Наличные</option>
          </select>
          <select v-model="filters.status" class="filter-select">
            <option value="">Все статусы</option>
            <option>Зачислено</option>
            <option>Обработка</option>
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
        <div v-for="pay in filteredPayments" :key="pay.id" class="table-row">
          <div class="col num">{{ pay.id }}</div>
          <div class="col date">{{ pay.date }}</div>
          <div class="col name">{{ pay.name }}</div>
          <div class="col amount">{{ pay.amount }}</div>
          <div class="col method">{{ pay.method }}</div>
          <div class="col basis">{{ pay.basis }}</div>
          <div class="col status">
            <span class="status-badge" :class="pay.statusClass">{{ pay.status }}</span>
          </div>
        </div>
        <div v-if="filteredPayments.length === 0" class="empty-state">
          Платежей по выбранным фильтрам не найдено
        </div>
        <div class="table-footer">
          <div class="footer-total">
            Итого поступило средств: <strong>{{ totalAmount }} ₽</strong>
          </div>
          <div class="footer-detail">Через банк: 15 700 ₽ | Наличные: 6 000 ₽</div>
        </div>
      </div>

      <BaseModal v-model="showModal" title="Принять оплату" @submit="handleSubmit">
        <BaseInput v-model="form.athlete" label="Спортсмен" placeholder="Иван Иванов" />
        <BaseInput v-model="form.amount" label="Сумма, ₽" placeholder="3 400" />
        <BaseInput v-model="form.method" label="Способ оплаты" placeholder="Карта" />
        <BaseInput v-model="form.basis" label="Основание" placeholder="Месячный абонемент" />
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import Sidebar from '../../../components/layout/Sidebar.vue'
import PageHeader from '../../../components/layout/PageHeader.vue'
import FinanceTabs from '../../../components/layout/FinanceTabs.vue'
import BaseButton from '../../../components/ui/BaseButton.vue'
import BaseIcon from '../../../components/ui/BaseIcon.vue'
import BaseModal from '../../../components/ui/BaseModal.vue'
import BaseInput from '../../../components/ui/BaseInput.vue'

const showModal = ref(false)
const searchQuery = ref('')
const form = reactive({ athlete: '', amount: '', method: '', basis: '' })
const filters = reactive({ period: 'Последние 30 дней', method: '', status: '' })

const payments = ref([
  { id: 1, date: '18.09.2026', name: 'Михаил Литвинов', amount: '3 400 ₽', method: 'Карта', basis: 'Месячный абонемент (Сентябрь)', status: 'Зачислено', statusClass: 'status-green' },
  { id: 2, date: '18.09.2026', name: 'Алина Орлова', amount: '1 200 ₽', method: 'Перевод', basis: 'Индивидуальная тренировка', status: 'Зачислено', statusClass: 'status-green' },
  { id: 3, date: '16.09.2026', name: 'Марина Соколова', amount: '5 000 ₽', method: 'Наличные', basis: 'Сбор на сборы Сочи', status: 'Зачислено', statusClass: 'status-green' },
  { id: 4, date: '15.09.2026', name: 'Дмитрий Рогов', amount: '2 500 ₽', method: 'Перевод', basis: 'Аренда бассейна', status: 'Обработка', statusClass: 'status-yellow' },
  { id: 5, date: '15.09.2026', name: 'Софья Петрова', amount: '2 800 ₽', method: 'Карта', basis: 'Месячный абонемент', status: 'Зачислено', statusClass: 'status-green' },
  { id: 6, date: '12.09.2026', name: 'Кирилл Захаров', amount: '2 000 ₽', method: 'Наличные', basis: 'Абонемент ОФП', status: 'Зачислено', statusClass: 'status-green' },
  { id: 7, date: '10.09.2026', name: 'Егор Булатов', amount: '4 000 ₽', method: 'Карта', basis: 'Индивидуальная тренировка x4', status: 'Зачислено', statusClass: 'status-green' },
  { id: 8, date: '08.09.2026', name: 'Виктория Белова', amount: '800 ₽', method: 'Карта', basis: 'Разовое посещение', status: 'Зачислено', statusClass: 'status-green' }
])

const filteredPayments = computed(() => {
  let result = payments.value
  if (filters.method) result = result.filter(p => p.method === filters.method)
  if (filters.status) result = result.filter(p => p.status === filters.status)
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    result = result.filter(p =>
      p.name.toLowerCase().includes(q) ||
      p.basis.toLowerCase().includes(q) ||
      p.method.toLowerCase().includes(q)
    )
  }
  return result
})

const parseAmount = (s) => Number(String(s).replace(/[^\d]/g, ''))
const totalAmount = computed(() =>
  filteredPayments.value.reduce((s, p) => s + parseAmount(p.amount), 0).toLocaleString('ru-RU')
)

const handleSubmit = () => {
  alert(`Оплата ${form.amount} ₽ от ${form.athlete} принята (демо)`)
  showModal.value = false
  Object.keys(form).forEach(k => form[k] = '')
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
.col.num { width: 40px; color: #6D7D79; }
.col.date { width: 100px; color: #6D7D79; }
.col.name { width: 220px; font-weight: 600; font-size: 14px; }
.col.amount { width: 120px; color: #2E8B57; font-weight: 700; font-size: 14px; }
.col.method { width: 140px; color: #6D7D79; }
.col.basis { width: 240px; color: #6D7D79; }
.col.status { flex: 1; }
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 700; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.table-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 24px; background: #F4F7F8;
  font-size: 13px; flex-wrap: wrap; gap: 12px;
}
.footer-total { color: #152421; }
.footer-total strong { font-weight: 800; color: #2E8B57; }
.footer-detail { color: #6D7D79; font-size: 12px; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; }
</style>