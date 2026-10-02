<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Секции" 
        subtitle="4 активных секции под управлением клуба"
        v-model="searchQuery"
        search-placeholder="Поиск секции..."
      >
        <template #actions>
          <BaseButton @click="showModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить секцию
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

      <div v-if="filteredSections.length === 0" class="empty-state">
        Секций не найдено
      </div>

      <div v-else class="sections-grid">
        <div v-for="section in filteredSections" :key="section.id" class="section-card">
          <div class="section-header">
            <h3>{{ section.title }}</h3>
            <span class="status-badge" :class="section.statusClass">{{ section.status }}</span>
          </div>
          <p class="section-desc">{{ section.description }}</p>
          <div class="section-stats">
            <div class="stat-item">
              <span class="stat-label">Групп</span>
              <span class="stat-value">{{ section.groups }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Тренеров</span>
              <span class="stat-value">{{ section.coaches }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Спортсменов</span>
              <span class="stat-value">{{ section.athletes }}</span>
            </div>
          </div>
        </div>
      </div>

      <BaseModal 
        v-model="showModal" 
        title="Добавить секцию"
        @submit="handleSubmit"
      >
        <BaseInput v-model="form.title" label="Название секции" placeholder="Спортивное плавание" />
        <BaseInput v-model="form.description" label="Описание" placeholder="Тренировки в бассейне 25м" />
        <div class="row-2">
          <BaseInput v-model="form.category" label="Категория" placeholder="Плавание" />
          <BaseInput v-model="form.groups" label="Количество групп" placeholder="3" />
        </div>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'

const showModal = ref(false)
const searchQuery = ref('')
const form = reactive({ title: '', description: '', category: '', groups: '' })

const tabs = ['Все', 'Плавание', 'Бокс', 'Гимнастика', 'ОФП']
const activeTab = ref('Все')

const sections = ref([
  { id: 1, title: 'Спортивное плавание', description: 'Тренировки в чаше бассейна 25м. Техника, выносливость, подготовка к разрядам и стартам.', groups: '3 группы', coaches: '2 тренера', athletes: '28 человек', status: 'Активна', statusClass: 'status-green', category: 'Плавание' },
  { id: 2, title: 'Бокс классический', description: 'Единоборства и общая выносливость. Отработка ударов, тактическая работа в спаррингах.', groups: '1 группа', coaches: '1 тренер', athletes: '8 человек', status: 'Активна', statusClass: 'status-green', category: 'Бокс' },
  { id: 3, title: 'Художественная гимнастика', description: 'Растяжка, координация, ритмика и хореография для детей.', groups: '1 группа', coaches: '1 тренер', athletes: '15 человек', status: 'Набор открыт', statusClass: 'status-blue', category: 'Гимнастика' },
  { id: 4, title: 'Общая физ-подготовка (ОФП)', description: 'Базовая физическая подготовка для детей и взрослых.', groups: '1 группа', coaches: '1 тренер', athletes: '20 человек', status: 'Активна', statusClass: 'status-green', category: 'ОФП' }
])

const filteredSections = computed(() => {
  let result = sections.value
  if (activeTab.value !== 'Все') {
    result = result.filter(s => s.category === activeTab.value)
  }
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    result = result.filter(s =>
      s.title.toLowerCase().includes(q) ||
      s.description.toLowerCase().includes(q) ||
      s.status.toLowerCase().includes(q)
    )
  }
  return result
})

const handleSubmit = () => {
  alert(`Секция ${form.title} добавлена (демо)`)
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

.sections-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 16px; }
.section-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 16px; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
}
.section-header { display: flex; justify-content: space-between; align-items: center; }
.section-header h3 { font-size: 18px; font-weight: 700; color: #152421; }
.status-badge { padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-blue { background: #DDECFB; color: #35678E; }
.section-desc { font-size: 14px; color: #6D7D79; line-height: 1.5; }
.section-stats { display: flex; gap: 24px; }
.stat-item { display: flex; flex-direction: column; gap: 4px; }
.stat-label { font-size: 11px; font-weight: 700; color: #6D7D79; text-transform: uppercase; }
.stat-value { font-size: 16px; font-weight: 700; color: #152421; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.empty-state {
  padding: 60px; text-align: center;
  color: #98A6A2; font-size: 14px;
  background: white; border: 1px dashed #E3EAE8;
  border-radius: 16px;
}
</style>