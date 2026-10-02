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
          <BaseButton @click="showModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать сбор
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <section class="fees-grid">
        <div v-for="fee in filteredFees" :key="fee.id" class="fee-card">
          <div class="fee-header">
            <h3>{{ fee.title }}</h3>
            <span class="status-badge" :class="fee.statusClass">{{ fee.deadline }}</span>
          </div>
          <div class="fee-stats">
            <div class="fee-line">
              <span>Цель: {{ fee.target }} ₽</span>
              <span class="collected">Собрано: {{ fee.collected }} ₽</span>
            </div>
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: fee.percent + '%' }"></div>
            </div>
            <div class="fee-line small">
              <span>{{ fee.percent }}% собрано</span>
              <span v-if="fee.remaining > 0" class="remaining">Осталось: {{ fee.remaining }} ₽</span>
            </div>
          </div>
        </div>
        <div v-if="filteredFees.length === 0" class="empty-state">
          Сборов не найдено
        </div>
      </section>

      <div class="detail-card">
        <h3>Взносы по сбору «Сбор Сочи 2026»</h3>
        <div class="table">
          <div class="table-header">
            <div class="col name">СПОРТСМЕН</div>
            <div class="col amount">НАЧИСЛЕНО</div>
            <div class="col amount">ОПЛАЧЕНО</div>
            <div class="col amount">ДОЛГ</div>
            <div class="col status">СТАТУС</div>
          </div>
          <div v-for="a in athletes" :key="a.id" class="table-row">
            <div class="col name">{{ a.name }}</div>
            <div class="col amount">{{ a.charged }} ₽</div>
            <div class="col amount paid">{{ a.paid }} ₽</div>
            <div class="col amount debt">{{ a.debt }} ₽</div>
            <div class="col status">
              <span class="status-badge" :class="a.statusClass">{{ a.status }}</span>
            </div>
          </div>
        </div>
      </div>

      <BaseModal v-model="showModal" title="Создать целевой сбор" @submit="handleSubmit">
        <BaseInput v-model="form.title" label="Название сбора" placeholder="Сбор Сочи 2026" />
        <div class="row-2">
          <BaseInput v-model="form.target" label="Цель, ₽" placeholder="150000" />
          <BaseInput v-model="form.deadline" label="Дедлайн" placeholder="15.10.2026" />
        </div>
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
const form = reactive({ title: '', target: '', deadline: '' })

const fees = ref([
  { id: 1, title: 'Сбор Сочи 2026', target: '150 000', collected: '120 000', percent: 80, remaining: 30000, deadline: '15.10.2026', statusClass: 'status-blue' },
  { id: 2, title: 'Экипировка (Форма)', target: '80 000', collected: '64 000', percent: 80, remaining: 16000, deadline: '30.09.2026', statusClass: 'status-blue' },
  { id: 3, title: 'Аренда Доп-Зала', target: '45 000', collected: '45 000', percent: 100, remaining: 0, deadline: 'Выполнен', statusClass: 'status-green' }
])

const athletes = ref([
  { id: 1, name: 'Михаил Литвинов', charged: 15000, paid: 15000, debt: 0, status: 'Оплачено', statusClass: 'status-green' },
  { id: 2, name: 'Дмитрий Рогов', charged: 15000, paid: 15000, debt: 0, status: 'Оплачено', statusClass: 'status-green' },
  { id: 3, name: 'Егор Кузнецов', charged: 15000, paid: 10000, debt: 5000, status: 'Частично', statusClass: 'status-yellow' },
  { id: 4, name: 'Алина Орлова', charged: 15000, paid: 0, debt: 15000, status: 'Не оплачено', statusClass: 'status-red' }
])

const filteredFees = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return fees.value
  return fees.value.filter(f => f.title.toLowerCase().includes(q))
})

const handleSubmit = () => {
  alert('Целевой сбор создан (демо)')
  showModal.value = false
  Object.keys(form).forEach(k => form[k] = '')
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
}
.fee-header { display: flex; justify-content: space-between; align-items: flex-start; }
.fee-header h3 { font-size: 16px; font-weight: 700; color: #152421; }
.status-badge { padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 700; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-blue { background: #DDECFB; color: #35678E; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.status-red { background: #FEE2E2; color: #D64545; }
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
.col.name { width: 300px; font-weight: 600; color: #152421; }
.col.amount { width: 150px; color: #6D7D79; }
.col.amount.paid { color: #2E8B57; font-weight: 600; }
.col.amount.debt { color: #D64545; font-weight: 600; }
.col.status { flex: 1; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; grid-column: 1 / -1; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
</style>