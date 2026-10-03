<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Группы и секции"
        :subtitle="subtitle"
        v-model="searchQuery"
        search-placeholder="Поиск группы..."
      >
        <template #actions>
          <BaseButton v-if="canWrite" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать группу
          </BaseButton>
        </template>
      </PageHeader>

      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-btn"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          {{ tab.name }}
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
        <template v-if="!loading && !loadError">
          <div
            v-for="group in groups"
            :key="group.id"
            class="table-row"
            @click="goToGroup(group.id)"
          >
            <div class="col name">{{ group.name }}</div>
            <div class="col direction">{{ sectionName(group.sectionId) }}</div>
            <div class="col coach">{{ coachText(group.coachIds) }}</div>
            <div class="col count">{{ athleteCountText(group.athleteCount) }}</div>
            <div class="col schedule">{{ scheduleText(group.id) }}</div>
            <div class="col status">
              <span class="status-badge" :class="`status-${tone(group.status)}`">{{ label('recordStatus', group.status) }}</span>
            </div>
          </div>
        </template>
      </div>

      <div v-if="loading" class="empty-state">
        <StateBlock kind="loading" />
      </div>
      <div v-else-if="loadError" class="empty-state">
        <StateBlock kind="error" :message="loadError" />
      </div>
      <div v-else-if="groups.length === 0" class="empty-state">
        <StateBlock kind="empty" :message="emptyMessage" />
      </div>

      <BaseModal
        v-model="showModal"
        title="Создать группу"
        :submit-label="saving ? 'Сохранение…' : 'Создать'"
        @submit="handleSubmit"
      >
        <BaseInput id="group-name" v-model="form.name" label="Название группы" placeholder="Группа А2" />
        <div class="field">
          <label class="field-label" for="group-section">Секция</label>
          <select id="group-section" v-model="form.sectionId" class="field-control">
            <option value="" disabled>Выберите секцию</option>
            <option v-for="section in activeSections" :key="section.id" :value="section.id">{{ section.name }}</option>
          </select>
        </div>
        <div class="field">
          <span class="field-label">Тренеры</span>
          <div v-if="coachOptions.length" class="check-list">
            <label v-for="coach in coachOptions" :key="coach.userId" class="check-item">
              <input v-model="form.coachIds" type="checkbox" :value="coach.userId" />
              {{ coach.fullName }}
            </label>
          </div>
          <p v-else class="form-hint">В организации нет активных тренеров.</p>
        </div>
        <BaseInput id="group-description" v-model="form.description" label="Описание" placeholder="Необязательно" />
        <p v-if="sectionsError" class="form-error">Не удалось загрузить секции: {{ sectionsError }}</p>
        <p v-else-if="!activeSections.length" class="form-hint">
          Сначала создайте секцию на странице
          <router-link to="/trainer/sections">«Секции»</router-link>.
        </p>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { groupsApi } from '../../api/groups'
import { sectionsApi } from '../../api/sections'
import { trainingsApi } from '../../api/trainings'
import { organizationsApi } from '../../api/organizations'
import { getOrganizationId, hasPermission, hasRole, currentUser } from '../../utils/session'
import { errorText, formatTime, toIsoDateTime, addDays } from '../../utils/format'
import { label, tone } from '../../utils/labels'

const ALL = 'all'
const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']
const FIELD_LABELS = { name: 'Название', sectionId: 'Секция', coachIds: 'Тренеры', description: 'Описание', status: 'Статус' }

const router = useRouter()
const canWrite = computed(() => hasPermission('groups.write'))
const isTrainer = computed(() => hasRole('TRAINER'))

// ─────────── Группы: поиск (q) и вкладка секции (sectionId) — параметры сервера ───────────
const searchQuery = ref('')
const activeTab = ref(ALL)
const groups = ref([])
const loading = ref(true)
const loadError = ref('')

let loadSeq = 0
async function loadGroups() {
  const seq = ++loadSeq
  loading.value = true
  loadError.value = ''
  try {
    const items = await fetchAll(params => groupsApi.list(getOrganizationId(), params), {
      q: searchQuery.value.trim(),
      sectionId: activeTab.value === ALL ? null : activeTab.value
    })
    if (seq === loadSeq) groups.value = items
  } catch (e) {
    if (seq === loadSeq) {
      groups.value = []
      loadError.value = errorText(e)
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// Подзаголовок: активные группы текущего тренера (у председателя — все активные группы)
const ownCount = ref(null)
async function loadOwnCount() {
  try {
    const page = await groupsApi.list(getOrganizationId(), {
      status: 'ACTIVE',
      coachId: isTrainer.value ? currentUser.value?.userId : null,
      size: 1
    })
    ownCount.value = page.totalElements
  } catch (_) {
    ownCount.value = null
  }
}

const subtitle = computed(() => {
  const n = ownCount.value
  if (n === null) return 'Группы организации'
  const word = plural(n, 'активная группа', 'активные группы', 'активных групп')
  return isTrainer.value ? `Всего ${n} ${word} под вашим руководством` : `Всего ${n} ${word} в организации`
})

const emptyMessage = computed(() => {
  const q = searchQuery.value.trim()
  if (q) return `Ничего не найдено по запросу «${q}»`
  if (activeTab.value !== ALL) return 'В этой секции групп пока нет'
  return canWrite.value ? 'Групп пока нет — создайте первую' : 'Групп пока нет'
})

function athleteCountText(n = 0) {
  return `${n} ${plural(n, 'человек', 'человека', 'человек')}`
}

const goToGroup = (id) => router.push(`/trainer/groups/${id}`)

// ─────────── Секции: вкладки, направление группы, выбор в форме ───────────
const sections = ref([])
const sectionsError = ref('')

async function loadSections() {
  try {
    sections.value = await fetchAll(params => sectionsApi.list(getOrganizationId(), params))
    sectionsError.value = ''
  } catch (e) {
    sections.value = []
    sectionsError.value = errorText(e)
  }
}

const activeSections = computed(() => sections.value.filter(s => s.status === 'ACTIVE'))
const tabs = computed(() => [
  { id: ALL, name: 'Все группы' },
  ...activeSections.value.map(s => ({ id: s.id, name: s.name }))
])

function sectionName(id) {
  return sections.value.find(s => s.id === id)?.name || '—'
}

// ─────────── Тренеры (участники с ролью TRAINER) ───────────
const trainers = ref([])

async function loadTrainers() {
  try {
    trainers.value = await fetchAll(params => organizationsApi.members(getOrganizationId(), params), { role: 'TRAINER' })
  } catch (_) {
    trainers.value = [] // нет members.read — имена тренеров недоступны
  }
}

function trainerName(id) {
  const member = trainers.value.find(m => m.userId === id)
  if (member) return member.fullName
  return id === currentUser.value?.userId ? currentUser.value.fullName : null
}

function coachText(ids = []) {
  if (!ids.length) return '—'
  const names = ids.map(trainerName).filter(Boolean)
  if (!names.length) return `${ids.length} ${plural(ids.length, 'тренер', 'тренера', 'тренеров')}`
  return ids.length > 1 ? `${names[0]} +${ids.length - 1}` : names[0]
}

// В форме — только активные тренеры (сервер требует активную роль TRAINER)
const coachOptions = computed(() => {
  const active = trainers.value.filter(m => m.status === 'ACTIVE')
  if (active.length) return active
  const me = currentUser.value
  return isTrainer.value && me ? [{ userId: me.userId, fullName: me.fullName }] : []
})

// ─────────── Расписание: занятия групп на ближайшие 7 дней ───────────
const weekTrainings = ref([])
const scheduleLoaded = ref(false)

async function loadWeekSchedule() {
  const from = new Date()
  from.setHours(0, 0, 0, 0)
  try {
    weekTrainings.value = await fetchAll(params => trainingsApi.list(getOrganizationId(), params), {
      from: toIsoDateTime(from),
      to: toIsoDateTime(addDays(from, 7))
    })
    scheduleLoaded.value = true
  } catch (_) {
    weekTrainings.value = []
    scheduleLoaded.value = false
  }
}

// «Пн, Ср, Пт · 09:00, Сб · 12:00» по неотменённым занятиям недели
const scheduleByGroup = computed(() => {
  const byGroup = {}
  for (const training of weekTrainings.value) {
    if (training.status === 'CANCELLED') continue
    const list = byGroup[training.groupId] || (byGroup[training.groupId] = [])
    list.push(training)
  }
  const result = {}
  for (const [groupId, list] of Object.entries(byGroup)) result[groupId] = scheduleSummary(list)
  return result
})

function scheduleSummary(list) {
  const byTime = new Map()
  for (const training of list) {
    const start = new Date(training.startsAt)
    const time = formatTime(start)
    const day = (start.getDay() + 6) % 7
    const days = byTime.get(time) || []
    if (!days.includes(day)) days.push(day)
    byTime.set(time, days)
  }
  return [...byTime.entries()]
    .map(([time, days]) => ({ time, days: days.sort((a, b) => a - b) }))
    .sort((a, b) => a.days[0] - b.days[0] || a.time.localeCompare(b.time))
    .map(({ time, days }) => `${days.map(d => WEEKDAYS[d]).join(', ')} · ${time}`)
    .join(', ')
}

function scheduleText(groupId) {
  if (!scheduleLoaded.value) return '—'
  return scheduleByGroup.value[groupId] || 'Нет занятий на неделе'
}

// ─────────── Создание группы ───────────
const showModal = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive({ name: '', sectionId: '', coachIds: [], description: '' })

function openCreate() {
  const me = currentUser.value?.userId
  let sectionId = ''
  if (activeTab.value !== ALL) sectionId = activeTab.value
  else if (activeSections.value.length === 1) sectionId = activeSections.value[0].id
  Object.assign(form, {
    name: '',
    sectionId,
    coachIds: coachOptions.value.some(c => c.userId === me) ? [me] : [],
    description: ''
  })
  formError.value = ''
  showModal.value = true
}

async function handleSubmit() {
  if (saving.value) return
  const name = form.name.trim()
  if (!name) { formError.value = 'Укажите название группы.'; return }
  if (!form.sectionId) { formError.value = 'Выберите секцию.'; return }
  if (!form.coachIds.length) { formError.value = 'Выберите хотя бы одного тренера.'; return }

  saving.value = true
  formError.value = ''
  try {
    await groupsApi.create(getOrganizationId(), {
      name,
      sectionId: form.sectionId,
      coachIds: [...form.coachIds],
      description: form.description.trim() || null,
      status: 'ACTIVE'
    })
    showModal.value = false
    await Promise.all([loadGroups(), loadOwnCount()])
  } catch (e) {
    formError.value = formErrorText(e)
  } finally {
    saving.value = false
  }
}

// ─────────── Жизненный цикл ───────────
let searchTimer = null
watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadGroups, 300)
})
watch(activeTab, loadGroups)
onBeforeUnmount(() => clearTimeout(searchTimer))

onMounted(() => {
  loadGroups()
  loadOwnCount()
  loadSections()
  loadTrainers()
  loadWeekSchedule()
})

// ─────────── Помощники ───────────
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

// Текст ошибки формы: имена полей из fieldErrors — по-русски
function formErrorText(e) {
  const fieldErrors = (e?.fieldErrors || []).map(f => ({ ...f, field: FIELD_LABELS[f.field] || f.field }))
  return errorText({ status: e?.status, message: e?.message, fieldErrors })
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
  text-align: center; white-space: nowrap;
}
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-blue { background: #DDECFB; color: #35678E; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.status-gray { background: #EEF1F0; color: #888888; }

/* Поля формы в стиле BaseInput */
.field { display: flex; flex-direction: column; gap: 6px; width: 100%; }
.field-label { font-size: 12px; color: var(--color-gray-text); font-weight: 500; }
.field-control {
  width: 100%; min-height: 40px; padding: 8px 12px;
  border: 1px solid var(--color-gray-border); border-radius: 8px;
  font-size: 14px; font-family: inherit; color: var(--color-dark);
  background: var(--color-white); outline: none;
}
.field-control:focus { border-color: var(--color-primary); }
.check-list {
  display: flex; flex-direction: column; gap: 8px;
  max-height: 180px; overflow-y: auto;
  padding: 10px 12px; border: 1px solid var(--color-gray-border); border-radius: 8px;
}
.check-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #152421; cursor: pointer; }
.form-hint { font-size: 13px; color: #6D7D79; }
.form-hint a { color: #35678E; }
.form-error { font-size: 13px; color: #D64545; }

@media (max-width: 1200px) {
  .table-header, .table-row { gap: 12px; padding: 16px; }
  .col.schedule, .col.direction { display: none; }
}
</style>
