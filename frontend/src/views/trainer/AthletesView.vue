<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Спортсмены" 
        :subtitle="`${totalCount} спортсменов под вашим контролем`"
        v-model="searchQuery"
        search-placeholder="Поиск по ФИО..."
      >
        <template #actions>
          <BaseButton @click="showModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить атлета
          </BaseButton>
        </template>
      </PageHeader>

      <!-- Фильтры -->
      <div class="filter-panel">
        <div class="filter-group">
          <label>Группа</label>
          <select v-model="filters.group">
            <option value="">Все</option>
            <option v-for="g in uniqueGroups" :key="g" :value="g">{{ g }}</option>
          </select>
        </div>
        <div class="filter-group">
          <label>Секция</label>
          <select v-model="filters.section">
            <option value="">Все</option>
            <option v-for="s in uniqueSections" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
        <div class="filter-group">
          <label>Статус</label>
          <select v-model="filters.status">
            <option value="">Все</option>
            <option value="ACTIVE">Активен</option>
            <option value="ARCHIVED">В архиве</option>
          </select>
        </div>
        <div class="filter-group sort-group">
          <label>Сортировка</label>
          <button class="sort-btn" @click="toggleSort">
            ФИО
            <BaseIcon name="chevron-down" :size="12" :style="{ transform: sortAsc ? 'rotate(180deg)' : 'none' }" />
          </button>
        </div>
      </div>

      <!-- Таблица -->
      <div class="table-card">
        <div v-if="loading" class="empty-state">Загрузка...</div>
        <div v-else-if="paginatedAthletes.length === 0" class="empty-state">
          Спортсменов не найдено
        </div>
        <template v-else>
          <div class="table-header">
            <div class="col name">ФИО АТЛЕТА / ВОЗРАСТ</div>
            <div class="col group">ГРУППА</div>
            <div class="col section">СЕКЦИЯ</div>
            <div class="col coach">ТРЕНЕР</div>
            <div class="col date">ДАТА ЗАЧИСЛЕНИЯ</div>
            <div class="col status">СТАТУС</div>
          </div>

          <div v-for="athlete in paginatedAthletes" :key="athlete.id" class="table-row">
            <div class="col name">
              <div class="avatar-small">{{ initialsOf(athlete) }}</div>
              <div class="name-info">
                <router-link :to="`/trainer/athletes/${athlete.id}`" class="name-link">
                  {{ fullName(athlete) }}
                </router-link>
                <span class="age">{{ ageOf(athlete) }}</span>
              </div>
            </div>
            <div class="col group">{{ groupName(athlete) }}</div>
            <div class="col section">{{ sectionName(athlete) }}</div>
            <div class="col coach">{{ coachName(athlete) }}</div>
            <div class="col date">{{ formatDate(athlete.enrolledOn) }}</div>
            <div class="col status">
              <span class="status-badge" :class="statusClass(athlete.status)">
                {{ statusLabel(athlete.status) }}
              </span>
            </div>
          </div>
        </template>

        <!-- Пагинация -->
        <div v-if="totalPages > 1" class="pagination">
          <span>Показано {{ paginatedAthletes.length }} из {{ filteredAthletes.length }}</span>
          <div class="pages">
            <button class="page-btn" :disabled="currentPage === 1" @click="currentPage--">Назад</button>
            <button 
              v-for="page in totalPages" 
              :key="page"
              class="page-btn"
              :class="{ active: currentPage === page }"
              @click="currentPage = page"
            >
              {{ page }}
            </button>
            <button class="page-btn" :disabled="currentPage === totalPages" @click="currentPage++">Вперед</button>
          </div>
        </div>
      </div>

      <BaseModal 
        v-model="showModal" 
        title="Добавить нового атлета"
        submit-label="Сохранить"
        @submit="handleSubmit"
      >
        <BaseInput v-model="form.lastName" label="Фамилия" placeholder="Иванов" />
        <BaseInput v-model="form.firstName" label="Имя" placeholder="Иван" />
        <BaseInput v-model="form.middleName" label="Отчество (необязательно)" placeholder="Иванович" />
        <BaseInput v-model="form.birthDate" label="Дата рождения (YYYY-MM-DD)" placeholder="2010-05-12" />
        <BaseInput v-model="form.enrolledOn" label="Дата зачисления (YYYY-MM-DD)" placeholder="2024-09-01" />
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import { athletesApi } from '../../api/athletes'
import { getOrganizationId } from '../../utils/session'

// ─────────── Состояние ───────────
const searchQuery = ref('')
const filters = reactive({ group: '', section: '', status: '' })
const sortAsc = ref(true)
const currentPage = ref(1)
const perPage = 5
const loading = ref(false)
const items = ref([])

// ─────────── Загрузка с API ───────────
async function load() {
  loading.value = true
  try {
    const res = await athletesApi.list(getOrganizationId(), {
      q: searchQuery.value || undefined,
      status: filters.status || undefined,
      size: 100
    })
    items.value = res.items || res
  } catch (e) {
    console.warn('API недоступен, используем демо:', e.message)
    items.value = demoData()
  } finally {
    loading.value = false
  }
}

function demoData() {
  return [
    { id: '1', firstName: 'Арина', lastName: 'Ковалева', birthDate: '2010-05-12', enrolledOn: '2023-09-01', status: 'ACTIVE', groups: [{ name: 'Группа А1', sectionId: 's1', sectionName: 'Плавание', coachName: 'Алексей Крылов' }] },
    { id: '2', firstName: 'Максим', lastName: 'Литвинов', birthDate: '2009-08-20', enrolledOn: '2023-10-15', status: 'ACTIVE', groups: [{ name: 'Группа А1', sectionId: 's1', sectionName: 'Плавание', coachName: 'Алексей Крылов' }] },
    { id: '3', firstName: 'Ольга', lastName: 'Орлова', birthDate: '2011-03-05', enrolledOn: '2023-11-22', status: 'ACTIVE', groups: [{ name: 'Группа А1', sectionId: 's1', sectionName: 'Плавание', coachName: 'Марина Соколова' }] }
  ]
}

// Перезагружаем при смене поиска/статуса
let t
watch([searchQuery, () => filters.status], () => {
  clearTimeout(t)
  t = setTimeout(() => {
    currentPage.value = 1
    load()
  }, 300)
})

onMounted(load)

// ─────────── Вычисляемые ───────────
const uniqueGroups = computed(() => [
  ...new Set(items.value.flatMap(a => (a.groups || []).map(g => g.name)))
])

const uniqueSections = computed(() => [
  ...new Set(items.value.flatMap(a => (a.groups || []).map(g => g.sectionName).filter(Boolean)))
])

const filteredAthletes = computed(() => {
  let result = items.value.filter(a => {
    const groups = a.groups || []
    if (filters.group && !groups.some(g => g.name === filters.group)) return false
    if (filters.section && !groups.some(g => g.sectionName === filters.section)) return false
    if (filters.status && a.status !== filters.status) return false
    return true
  })

  result = [...result].sort((a, b) => {
    const nA = (a.lastName + a.firstName).toLowerCase()
    const nB = (b.lastName + b.firstName).toLowerCase()
    return sortAsc.value ? nA.localeCompare(nB) : nB.localeCompare(nA)
  })

  return result
})

const totalCount = computed(() => items.value.length)
const totalPages = computed(() => Math.max(1, Math.ceil(filteredAthletes.value.length / perPage)))

const paginatedAthletes = computed(() => {
  const start = (currentPage.value - 1) * perPage
  return filteredAthletes.value.slice(start, start + perPage)
})

// ─────────── Форматтеры ───────────
function fullName(a) {
  return [a.lastName, a.firstName, a.middleName].filter(Boolean).join(' ')
}
function initialsOf(a) {
  return ((a.lastName?.[0] || '') + (a.firstName?.[0] || '')).toUpperCase() || '?'
}
function ageOf(a) {
  if (!a.birthDate) return '—'
  const birth = new Date(a.birthDate)
  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  const m = now.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) age--
  return `${age} лет`
}
function groupName(a) {
  return a.groups?.[0]?.name || '—'
}
function sectionName(a) {
  return a.groups?.[0]?.sectionName || '—'
}
function coachName(a) {
  return a.groups?.[0]?.coachName || '—'
}
function formatDate(d) {
  if (!d) return '—'
  const [y, m, day] = d.split('-')
  return `${day}.${m}.${y}`
}
function statusLabel(s) {
  return { ACTIVE: 'Активен', ARCHIVED: 'В архиве' }[s] || s
}
function statusClass(s) {
  return { ACTIVE: 'status-green', ARCHIVED: 'status-yellow' }[s] || 'status-yellow'
}

// ─────────── Создание ───────────
const showModal = ref(false)
const form = reactive({
  firstName: '',
  lastName: '',
  middleName: '',
  birthDate: '',
  enrolledOn: ''
})

async function handleSubmit() {
  try {
    await athletesApi.create(getOrganizationId(), {
      firstName: form.firstName,
      lastName: form.lastName,
      middleName: form.middleName || null,
      birthDate: form.birthDate,
      enrolledOn: form.enrolledOn,
      status: 'ACTIVE'
    })
    showModal.value = false
    Object.keys(form).forEach(k => form[k] = '')
    await load()
  } catch (e) {
    alert(`Ошибка: ${e.message}`)
  }
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace {
  flex: 1; padding: 28px 32px 32px;
  display: flex; flex-direction: column; gap: 24px;
  overflow-y: auto;
}

.filter-panel {
  display: flex; gap: 16px; flex-wrap: wrap;
  background: white; padding: 16px; border-radius: 12px;
  border: 1px solid #E3EAE8;
}
.filter-group { display: flex; flex-direction: column; gap: 4px; }
.filter-group label {
  font-size: 11px; font-weight: 700; color: #98A6A2;
  text-transform: uppercase;
}
.filter-group select {
  padding: 8px 12px;
  border: 1px solid #E3EAE8;
  border-radius: 8px;
  font-size: 13px;
  background: #F4F7F8;
  color: #152421;
  cursor: pointer;
  outline: none;
}

.sort-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px;
  border: 1px solid #E3EAE8;
  border-radius: 8px;
  font-size: 13px; font-weight: 600;
  background: #F4F7F8; color: #152421;
  cursor: pointer;
}

.table-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 16px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
  overflow-x: auto;
}
.table-header {
  display: flex; gap: 16px; padding-bottom: 12px;
  border-bottom: 1px solid #E3EAE8; min-width: 900px;
}
.table-row {
  display: flex; gap: 16px; padding: 12px 0;
  border-bottom: 1px solid #E3EAE8; align-items: center;
  min-width: 900px;
}
.col { font-size: 14px; color: #152421; }
.col.name { width: 240px; display: flex; align-items: center; gap: 12px; }
.col.group { width: 160px; }
.col.section { width: 160px; color: #6D7D79; }
.col.coach { width: 180px; }
.col.date { width: 130px; color: #6D7D79; }
.col.status { flex: 1; }

.table-header .col {
  font-size: 12px; font-weight: 700; color: #6D7D79;
  text-transform: uppercase;
}

.avatar-small {
  width: 32px; height: 32px; background: #F4F7F8;
  border-radius: 999px; display: flex;
  justify-content: center; align-items: center;
  font-size: 11px; font-weight: 700; color: #152421;
  flex-shrink: 0;
}
.name-info { display: flex; flex-direction: column; gap: 2px; }
.name-link {
  font-weight: 700; color: #152421; text-decoration: none;
}
.name-link:hover { color: #2E8B57; }
.age { font-size: 12px; color: #6D7D79; }

.status-badge {
  display: inline-block; padding: 4px 12px;
  border-radius: 999px; font-size: 12px; font-weight: 700;
}
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.status-red { background: #FCE2E5; color: #D64545; }

.pagination {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 12px; font-size: 13px; color: #6D7D79;
  min-width: 900px;
}
.pages { display: flex; gap: 8px; }
.page-btn {
  padding: 6px 12px; background: #F4F7F8;
  border: 1px solid #E3EAE8; border-radius: 6px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
}
.page-btn.active { background: #102522; color: white; border-color: #102522; }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.empty-state {
  padding: 40px; text-align: center;
  color: #98A6A2; font-size: 14px;
  border: 1px dashed #E3EAE8;
  border-radius: 12px;
}
</style>