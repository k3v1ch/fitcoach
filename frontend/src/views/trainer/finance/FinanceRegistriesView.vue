<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Финансы" 
        subtitle="Управление финансовыми реестрами и платежами"
        v-model="searchQuery"
        search-placeholder="Поиск реестра..."
      >
        <template #actions>
          <BaseButton @click="showModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать реестр
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <div class="table-card">
        <div class="table-title">
          <h3>Реестры финансовых операций</h3>
          <span class="total">Всего записей: {{ filteredRegistries.length }}</span>
        </div>

        <div class="table-header">
          <div class="col num">№</div>
          <div class="col date">Дата</div>
          <div class="col type">Тип реестра</div>
          <div class="col period">Период выгрузки</div>
          <div class="col count">Записей</div>
          <div class="col status">Статус</div>
          <div class="col actions">Действия</div>
        </div>

        <div v-for="reg in filteredRegistries" :key="reg.id" class="table-row">
          <div class="col num">{{ reg.id }}</div>
          <div class="col date">{{ reg.date }}</div>
          <div class="col type">{{ reg.type }}</div>
          <div class="col period">{{ reg.period }}</div>
          <div class="col count">{{ reg.count }}</div>
          <div class="col status">
            <span class="status-badge" :class="reg.statusClass">{{ reg.status }}</span>
          </div>
          <div class="col actions">
            <button class="download-btn" @click="downloadRegistry(reg)">Скачать</button>
          </div>
        </div>

        <div v-if="filteredRegistries.length === 0" class="empty-state">
          Реестров не найдено
        </div>
      </div>

      <BaseModal v-model="showModal" title="Создать реестр" @submit="handleSubmit">
        <BaseInput v-model="form.type" label="Тип реестра" placeholder="Оплата абонементов" />
        <BaseInput v-model="form.period" label="Период выгрузки" placeholder="Сентябрь 2026" />
        <div class="row-2">
          <BaseInput v-model="form.dateFrom" label="Дата С" placeholder="01.09.2026" />
          <BaseInput v-model="form.dateTo" label="До" placeholder="30.09.2026" />
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
const form = reactive({ type: '', period: '', dateFrom: '', dateTo: '' })

const registries = ref([
  { id: 'Р-2026-06', date: '15.09.2026', type: 'Оплата абонементов', period: 'Август 2026', count: 48, status: 'Проведен', statusClass: 'status-green' },
  { id: 'Р-2026-05', date: '02.09.2026', type: 'Целевой сбор (Сочи)', period: 'Сентябрь 2026', count: 12, status: 'Проведен', statusClass: 'status-green' },
  { id: 'Р-2026-04', date: '28.08.2026', type: 'Расходы на инвентарь', period: 'Август 2026', count: 8, status: 'Проведен', statusClass: 'status-green' },
  { id: 'Р-2026-03', date: '15.08.2026', type: 'Оплата абонементов', period: 'Июль 2026', count: 44, status: 'На проверке', statusClass: 'status-yellow' },
  { id: 'Р-2026-02', date: '01.08.2026', type: 'Возвраты взносов', period: 'Июль 2026', count: 3, status: 'Отклонен', statusClass: 'status-red' },
  { id: 'Р-2026-01', date: '15.07.2026', type: 'Оплата абонементов', period: 'Июнь 2026', count: 40, status: 'Проведен', statusClass: 'status-green' }
])

const filteredRegistries = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return registries.value
  return registries.value.filter(r =>
    r.id.toLowerCase().includes(q) ||
    r.type.toLowerCase().includes(q) ||
    r.period.toLowerCase().includes(q) ||
    r.status.toLowerCase().includes(q)
  )
})

const downloadRegistry = (reg) => {
  // Готово к подключению к бэкенду: window.open(`/api/registries/${reg.id}/download`)
  alert(`Скачивание реестра ${reg.id} (демо)`)
}

const handleSubmit = () => {
  alert('Реестр создан (демо)')
  showModal.value = false
  Object.keys(form).forEach(k => form[k] = '')
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.table-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 16px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
  overflow-x: auto;
}
.table-title { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.table-title h3 { font-size: 16px; font-weight: 700; color: #152421; }
.table-title .total { font-size: 12px; color: #6D7D79; }
.table-header, .table-row { display: flex; align-items: center; gap: 12px; min-width: 1000px; }
.table-header {
  background: #F4F7F8; padding: 12px 16px;
  border-radius: 8px;
  font-size: 12px; font-weight: 700; color: #6D7D79; text-transform: uppercase;
}
.table-row { padding: 16px; border-bottom: 1px solid #E3EAE8; font-size: 13px; }
.col.num { width: 60px; font-weight: 600; color: #152421; }
.col.date { width: 120px; }
.col.type { width: 180px; font-weight: 500; }
.col.period { flex: 1; color: #6D7D79; }
.col.count { width: 100px; text-align: center; }
.col.status { width: 140px; display: flex; justify-content: center; }
.col.actions { width: 120px; display: flex; justify-content: flex-end; }
.status-badge { padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 600; }
.status-green { background: #EAF7EE; color: #2E8B57; }
.status-yellow { background: #FFF6ED; color: #D68C45; }
.status-red { background: #FDF2F2; color: #D64545; }
.download-btn { color: #2E8B57; background: none; border: none; font-size: 13px; font-weight: 600; cursor: pointer; }
.download-btn:hover { text-decoration: underline; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
</style>