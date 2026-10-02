<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Сборы и мероприятия" 
        subtitle="Календарь спортивных выездов, аттестаций и сборов"
        v-model="searchQuery"
        search-placeholder="Поиск мероприятия..."
      >
        <template #actions>
          <BaseButton @click="showModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать мероприятие
          </BaseButton>
        </template>
      </PageHeader>

      <div class="tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab"
          class="tab-btn"
          :class="{ active: activeTab === tab }"
          @click="activeTab = tab"
        >
          {{ tab }}
        </button>
      </div>

      <div v-if="filteredEvents.length === 0" class="empty-state">
        Мероприятий не найдено
      </div>

      <div v-else class="events-grid">
        <div v-for="event in filteredEvents" :key="event.id" class="event-card">
          <div class="event-header">
            <span class="event-type" :class="event.typeClass">{{ event.type }}</span>
            <span class="event-date">{{ event.date }}</span>
          </div>
          <h3>{{ event.title }}</h3>
          <p class="event-participants">{{ event.participants }}</p>
          <div class="divider"></div>
          <div class="event-footer">
            <div>
              <span class="footer-label">ФИНАНСОВЫЙ СТАТУС</span>
              <span class="footer-value">{{ event.finance }}</span>
            </div>
            <button class="action-btn">Управление</button>
          </div>
        </div>
      </div>

      <BaseModal 
        v-model="showModal" 
        title="Создать мероприятие"
        @submit="handleSubmit"
      >
        <BaseInput v-model="form.title" label="Наименование" placeholder="Летние сборы" />
        <div class="row-2">
          <BaseInput v-model="form.dateFrom" label="Дата С" placeholder="16.10.2024" />
          <BaseInput v-model="form.dateTo" label="До" placeholder="20.10.2024" />
        </div>
        <BaseInput v-model="form.type" label="Тип" placeholder="Сборы" />
        <BaseInput v-model="form.participants" label="Количество участников" placeholder="20" />
        <BaseInput v-model="form.finance" label="Финансовый статус" placeholder="Бесплатно" />
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'

const showModal = ref(false)
const searchQuery = ref('')
const form = reactive({ title: '', dateFrom: '', dateTo: '', type: '', participants: '', finance: '' })

const tabs = ['Все события', 'Сборы', 'Соревнования', 'Медицинские осмотры']
const activeTab = ref('Все события')

const events = ref([
  { id: 1, type: 'Сборы', typeClass: 'type-green', date: '12 – 25 октября 2026', title: 'Осенние сборы Сочи 2026', participants: '15 спортсменов', finance: 'Оплачено полностью', category: 'Сборы' },
  { id: 2, type: 'Соревнования', typeClass: 'type-yellow', date: '28 сентября 2026', title: 'Кубок Надежд Юниоры', participants: '8 участников', finance: 'Требуется взнос', category: 'Соревнования' },
  { id: 3, type: 'Медицинский осмотр', typeClass: 'type-blue', date: '02 октября 2026', title: 'Плановый диспансерный осмотр', participants: 'Все группы', finance: 'Бесплатно', category: 'Медицинские осмотры' },
  { id: 4, type: 'Сборы', typeClass: 'type-red', date: '05 – 10 ноября 2026', title: 'Интенсив по сухому плаванию', participants: '10 спортсменов', finance: 'Ожидает оплаты', category: 'Сборы' }
])

const filteredEvents = computed(() => {
  let result = events.value
  if (activeTab.value !== 'Все события') {
    result = result.filter(e => e.category === activeTab.value)
  }
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    result = result.filter(e =>
      e.title.toLowerCase().includes(q) ||
      e.type.toLowerCase().includes(q) ||
      e.participants.toLowerCase().includes(q) ||
      e.finance.toLowerCase().includes(q)
    )
  }
  return result
})

const handleSubmit = () => {
  alert('Мероприятие создано (демо)')
  showModal.value = false
  Object.keys(form).forEach(k => form[k] = '')
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.tabs { display: flex; gap: 12px; flex-wrap: wrap; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }

.events-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 16px;
}

.event-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.event-header {
  display: flex; justify-content: space-between; align-items: center;
}
.event-type {
  padding: 4px 10px; border-radius: 99px;
  font-size: 11px; font-weight: 700;
}
.type-green { background: #E9F7D5; color: #2E8B57; }
.type-yellow { background: #FFF8E6; color: #F2B705; }
.type-blue { background: #DDECFB; color: #35678E; }
.type-red { background: #FCE2E5; color: #D64545; }
.event-date { font-size: 13px; color: #6D7D79; }

.event-card h3 { font-size: 18px; font-weight: 700; color: #152421; }
.event-participants { font-size: 14px; color: #6D7D79; }

.divider { height: 1px; background: #E3EAE8; }

.event-footer {
  display: flex; justify-content: space-between; align-items: center;
}
.footer-label {
  display: block; font-size: 11px; font-weight: 700;
  color: #98A6A2; text-transform: uppercase;
}
.footer-value { font-size: 14px; font-weight: 600; color: #152421; }
.action-btn {
  padding: 8px 16px; background: #F4F7F8;
  border: none; border-radius: 8px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
}
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.empty-state {
  padding: 60px; text-align: center;
  color: #98A6A2; font-size: 14px;
  background: white; border: 1px dashed #E3EAE8;
  border-radius: 16px;
}
</style>