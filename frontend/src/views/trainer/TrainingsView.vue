<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Тренировки" 
        subtitle="Управление планом тренировочного процесса"
        v-model="searchQuery"
        search-placeholder="Поиск тренировки..."
      >
        <template #actions>
          <BaseButton @click="showModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать тренировку
          </BaseButton>
        </template>
      </PageHeader>

      <div class="tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.id"
          class="tab-btn"
          :class="{ active: activeTab === tab.id }"
          @click="switchTab(tab.id)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="loading" class="empty-state">Загрузка...</div>

      <div v-else class="table-card">
        <div class="table-header">
          <div class="col date">ДАТА / ВРЕМЯ</div>
          <div class="col group">ГРУППА</div>
          <div class="col type">ТИП</div>
          <div class="col place">МЕСТО</div>
          <div class="col status">СТАТУС</div>
          <div class="col attendance">ПОСЕЩАЕМОСТЬ</div>
          <div class="col actions"></div>
        </div>

        <div 
          v-for="train in filteredTrainings" 
          :key="train.id" 
          class="table-row"
          @click="goToReport(train.id)"
        >
          <div class="col date">{{ formatDateTime(train.startsAt) }}</div>
          <div class="col group">{{ train.groupName || train.groupId }}</div>
          <div class="col type">{{ train.typeName || train.typeId }}</div>
          <div class="col place">{{ train.venueName || train.venueId }}</div>
          <div class="col status">
            <span class="status-badge" :class="statusClass(train.status)">
              {{ statusLabel(train.status) }}
            </span>
          </div>
          <div class="col attendance">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: (train.attendancePercent || 0) + '%' }"></div>
            </div>
            <span>{{ train.attendanceText || '—' }}</span>
          </div>
          <div class="col actions" @click.stop>
            <button class="action-btn" @click="goToReport(train.id)">Журнал</button>
            <button class="action-btn outline" @click="handlePlan(train.id)">План</button>
          </div>
        </div>

        <div v-if="filteredTrainings.length === 0 && !loading" class="empty-state">
          Тренировок не найдено
        </div>
      </div>

      <BaseModal 
        v-model="showModal" 
        title="Создать тренировку"
        @submit="handleSubmit"
      >
        <BaseInput v-model="form.groupId" label="ID группы (UUID)" placeholder="..." />
        <BaseInput v-model="form.title" label="Название" placeholder="Водная подготовка" />
        <div class="row-2">
          <BaseInput v-model="form.startsAt" label="Начало (YYYY-MM-DDTHH:MM)" placeholder="2026-10-16T10:00" />
          <BaseInput v-model="form.endsAt" label="Конец (YYYY-MM-DDTHH:MM)" placeholder="2026-10-16T11:30" />
        </div>
        <BaseInput v-model="form.venueId" label="ID площадки (UUID)" />
        <BaseInput v-model="form.typeId" label="ID типа тренировки (UUID)" />
        <BaseInput v-model="form.coachIds" label="ID тренеров (через запятую)" />
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import { trainingsApi } from '../../api/trainings'
import { getOrganizationId } from '../../utils/session'

const router = useRouter()
const showModal = ref(false)
const searchQuery = ref('')
const loading = ref(false)
const trainings = ref([])

const form = reactive({
  groupId: '',
  title: '',
  startsAt: '',
  endsAt: '',
  venueId: '',
  typeId: '',
  coachIds: ''
})

const tabs = [
  { id: 'all', label: 'Все', status: null },
  { id: 'planned', label: 'Запланированные', status: 'PLANNED' },
  { id: 'done', label: 'Проведённые', status: 'COMPLETED' },
  { id: 'cancelled', label: 'Отменённые', status: 'CANCELLED' }
]
const activeTab = ref('all')

const filteredTrainings = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return trainings.value
  return trainings.value.filter(t =>
    (t.title || '').toLowerCase().includes(q) ||
    (t.groupName || '').toLowerCase().includes(q) ||
    (t.typeName || '').toLowerCase().includes(q)
  )
})

// ─────────── load ───────────
async function load() {
  loading.value = true
  try {
    const from = new Date()
    from.setDate(from.getDate() - 30)
    const to = new Date()
    to.setDate(to.getDate() + 60)
    const status = tabs.find(t => t.id === activeTab.value)?.status
    const res = await trainingsApi.list(getOrganizationId(), {
      from: from.toISOString(),
      to: to.toISOString(),
      status: status || undefined,
      size: 100
    })
    trainings.value = res.items || res
  } catch (e) {
    console.warn('Не удалось загрузить тренировки, показываем демо:', e.message)
    trainings.value = demoData()
  } finally {
    loading.value = false
  }
}

function demoData() {
  return [
    { id: 1, startsAt: '2026-09-18T09:00', groupName: 'Группа А1 (Старшие)', typeName: 'Водная подготовка', venueName: 'Бассейн (Дорожка 3)', status: 'COMPLETED', attendancePercent: 92, attendanceText: '11/12' },
    { id: 2, startsAt: '2026-09-18T14:00', groupName: 'ОФП Начальная', typeName: 'Сухая тренировка', venueName: 'Зал сухого плавания', status: 'PLANNED', attendancePercent: 0, attendanceText: '0/20' },
    { id: 3, startsAt: '2026-09-18T17:00', groupName: 'Юниоры Бокс', typeName: 'Работа в парах', venueName: 'Зал единоборств', status: 'PLANNED', attendancePercent: 0, attendanceText: '0/8' },
    { id: 4, startsAt: '2026-09-16T15:30', groupName: 'Гимнастика Дети', typeName: 'Хореография', venueName: 'Зал хореографии', status: 'CANCELLED', attendancePercent: 0, attendanceText: '0/15' }
  ]
}

function switchTab(id) {
  activeTab.value = id
  load()
}

const goToReport = (id) => router.push(`/trainer/trainings/${id}/report`)
const handlePlan = (id) => alert(`План тренировки #${id} (демо)`)

async function handleSubmit() {
  try {
    await trainingsApi.create(getOrganizationId(), {
      title: form.title,
      groupId: form.groupId,
      coachIds: form.coachIds.split(',').map(s => s.trim()).filter(Boolean),
      venueId: form.venueId,
      typeId: form.typeId,
      startsAt: form.startsAt,
      endsAt: form.endsAt,
      plan: []
    })
    showModal.value = false
    Object.keys(form).forEach(k => form[k] = '')
    load()
  } catch (e) {
    alert(`Ошибка: ${e.message}`)
  }
}

function formatDateTime(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('ru-RU', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}
function statusLabel(s) {
  return { PLANNED: 'Запланирована', COMPLETED: 'Проведена', CANCELLED: 'Отменена' }[s] || s
}
function statusClass(s) {
  return { PLANNED: 'status-blue', COMPLETED: 'status-green', CANCELLED: 'status-red' }[s] || 'status-blue'
}

onMounted(load)
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.tabs { display: flex; gap: 12px; flex-wrap: wrap; }
.tab-btn { padding: 8px 16px; border-radius: 8px; background: white; border: 1px solid #E3EAE8; font-size: 13px; font-weight: 600; color: #6D7D79; cursor: pointer; }
.tab-btn.active { background: #102522; color: white; border-color: #102522; }
.table-card { background: white; border: 1px solid #E3EAE8; border-radius: 20px; padding: 24px; display: flex; flex-direction: column; gap: 16px; overflow-x: auto; }
.table-header, .table-row { display: flex; align-items: center; gap: 16px; min-width: 1000px; }
.table-header { padding-bottom: 12px; border-bottom: 1px solid #E3EAE8; }
.table-row { padding: 14px 0; border-bottom: 1px solid #E3EAE8; cursor: pointer; transition: background .2s; }
.table-row:hover { background: #F9FBFA; }
.col { font-size: 14px; color: #152421; }
.col.date { width: 160px; font-weight: 700; }
.col.group { width: 180px; font-weight: 600; }
.col.type { width: 140px; color: #6D7D79; }
.col.place { width: 160px; color: #6D7D79; }
.col.status { width: 140px; }
.col.attendance { width: 160px; display: flex; align-items: center; gap: 8px; }
.col.actions { flex: 1; display: flex; justify-content: flex-end; gap: 8px; }
.table-header .col { font-size: 12px; font-weight: 700; color: #6D7D79; text-transform: uppercase; }
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-blue { background: #DDECFB; color: #35678E; }
.status-red { background: #FCE2E5; color: #D64545; }
.progress-bar { width: 60px; height: 6px; background: #F4F7F8; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: #2E8B57; }
.action-btn { padding: 4px 8px; background: #F4F7F8; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; color: #152421; cursor: pointer; }
.action-btn.outline { background: white; border: 1px solid #E3EAE8; }
.empty-state { padding: 40px; text-align: center; color: #6D7D79; font-size: 14px; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
</style>