<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Группы и секции" 
        subtitle="Всего 6 активных групп под вашим руководством"
        v-model="searchQuery"
        search-placeholder="Поиск группы..."
      >
        <template #actions>
          <BaseButton @click="showModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать группу
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

      

      <div class="table-container">
        <div class="table-header">
          <div class="col name">НАЗВАНИЕ ГРУППЫ</div>
          <div class="col direction">НАПРАВЛЕНИЕ</div>
          <div class="col coach">ТРЕНЕР</div>
          <div class="col count">СПОРТСМЕНОВ</div>
          <div class="col schedule">РАСПИСАНИЕ</div>
          <div class="col status">СТАТУС</div>
        </div>
        <div 
          v-for="group in filteredGroups" 
          :key="group.id" 
          class="table-row"
          @click="goToGroup(group.id)"
        >
          <div class="col name">{{ group.name }}</div>
          <div class="col direction">{{ group.direction }}</div>
          <div class="col coach">{{ group.coach }}</div>
          <div class="col count">{{ group.count }}</div>
          <div class="col schedule">{{ group.schedule }}</div>
          <div class="col status">
            <span class="status-badge" :class="group.statusClass">{{ group.status }}</span>
          </div>
        </div>
      </div>

      <div v-if="filteredGroups.length === 0" class="empty-state">
        Ничего не найдено по запросу «{{ searchQuery }}»
      </div>

      <BaseModal 
        v-model="showModal" 
        title="Создать группу"
        @submit="handleSubmit"
      >
        <BaseInput v-model="form.name" label="Название группы" placeholder="Группа А2" />
        <BaseInput v-model="form.direction" label="Направление" placeholder="Плавание" />
        <BaseInput v-model="form.coach" label="Тренер" placeholder="Алексей Крылов" />
        <BaseInput v-model="form.schedule" label="Расписание" placeholder="Пн, Ср, Пт · 09:00" />
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { reactive } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'

const showModal = ref(false)
const form = reactive({ name: '', direction: '', coach: '', schedule: '' })

const handleSubmit = () => {
  alert('Группа создана (демо)')
  showModal.value = false
  Object.keys(form).forEach(k => form[k] = '')
}

const router = useRouter()
const searchQuery = ref('')
const tabs = ['Все группы', 'Плавание', 'Бокс', 'Гимнастика']
const activeTab = ref('Все группы')

const groups = [
  { id: 1, name: 'Группа А1 (Старшие)', direction: 'Плавание', coach: 'Алексей Крылов', count: '12 человек', schedule: 'Пн, Ср, Пт · 09:00', status: 'Активна', statusClass: 'status-green', category: 'Плавание' },
  { id: 2, name: 'Юниоры Бокс', direction: 'Бокс', coach: 'Алексей Крылов', count: '8 человек', schedule: 'Вт, Чт · 17:00, Сб · 12:00', status: 'Активна', statusClass: 'status-green', category: 'Бокс' },
  { id: 3, name: 'Гимнастика Дети', direction: 'Гимнастика', coach: 'Марина Соколова', count: '15 человек', schedule: 'Пн, Чт · 15:30', status: 'Активна', statusClass: 'status-green', category: 'Гимнастика' },
  { id: 4, name: 'Спец-подготовка', direction: 'Плавание', coach: 'Алексей Крылов', count: '5 человек', schedule: 'Ср, Пт · 11:00', status: 'Набор закрыт', statusClass: 'status-blue', category: 'Плавание' },
  { id: 5, name: 'ОФП Начальная', direction: 'ОФП', coach: 'Егор Булатов', count: '20 человек', schedule: 'Вт, Чт · 14:00', status: 'Ожидает старта', statusClass: 'status-yellow', category: 'ОФП' }
]

const filteredGroups = computed(() => {
  let result = groups
  // Фильтр по вкладке
  if (activeTab.value !== 'Все группы') {
    result = result.filter(g => g.category === activeTab.value)
  }
  // Фильтр по поиску
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    result = result.filter(g =>
      g.name.toLowerCase().includes(q) ||
      g.direction.toLowerCase().includes(q) ||
      g.coach.toLowerCase().includes(q)
    )
  }
  return result
})

const goToGroup = (id) => router.push(`/trainer/groups/${id}`)

const handleCreateGroup = () => alert('Создание группы (демо)')
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

.table-container { display: flex; flex-direction: column; gap: 16px; }
.table-header {
  display: flex; align-items: center; gap: 24px;
  padding: 0 24px; height: 40px;
}
.table-row {
  display: flex; align-items: center; gap: 24px;
  padding: 24px; background: white;
  border: 1px solid #E3EAE8; border-radius: 16px;
  cursor: pointer; transition: background 0.2s;
}
.table-row:hover { background: #F9FBFA; }

.col { font-size: 14px; color: #152421; }
.col.name { width: 220px; font-weight: 700; font-size: 15px; }
.col.direction { width: 160px; color: #6D7D79; }
.col.coach { width: 180px; }
.col.count { width: 120px; font-weight: 600; }
.col.schedule { width: 260px; color: #6D7D79; }
.col.status { flex: 1; }

.empty-state {
  padding: 60px;
  text-align: center;
  color: #98A6A2;
  font-size: 14px;
  background: white;
  border: 1px dashed #E3EAE8;
  border-radius: 16px;
}

.table-header .col {
  font-size: 12px; font-weight: 700; color: #6D7D79;
  text-transform: uppercase;
}

.status-badge {
  display: inline-block; padding: 4px 12px;
  border-radius: 999px; font-size: 12px; font-weight: 700;
  text-align: center;
}
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-blue { background: #DDECFB; color: #35678E; }
.status-yellow { background: #FFF8E6; color: #F2B705; }

@media (max-width: 1200px) {
  .table-header, .table-row { gap: 12px; padding: 16px; }
  .col.schedule, .col.direction { display: none; }
}
</style>