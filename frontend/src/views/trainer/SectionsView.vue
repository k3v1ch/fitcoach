<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Секции"
        :subtitle="subtitle"
        v-model="searchQuery"
        search-placeholder="Поиск секции..."
      >
        <template #actions>
          <BaseButton v-if="canWrite" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить секцию
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

      <div v-if="loading" class="empty-state">
        <StateBlock kind="loading" />
      </div>
      <div v-else-if="loadError" class="empty-state">
        <StateBlock kind="error" :message="loadError" />
      </div>
      <div v-else-if="sections.length === 0" class="empty-state">
        <StateBlock kind="empty" :message="emptyMessage" />
      </div>

      <div v-else class="sections-grid">
        <div v-for="section in sections" :key="section.id" class="section-card">
          <div class="section-header">
            <h3>{{ section.name }}</h3>
            <div class="header-side">
              <span class="status-badge" :class="`status-${tone(section.status)}`">{{ label('recordStatus', section.status) }}</span>
              <button v-if="canWrite" class="icon-btn" title="Изменить секцию" @click="openEdit(section)">
                <BaseIcon name="edit" :size="14" color="#152421" />
              </button>
            </div>
          </div>
          <p class="section-desc">{{ sectionDescription(section) }}</p>
          <div class="section-stats">
            <div class="stat-item">
              <span class="stat-label">Групп</span>
              <span class="stat-value">{{ statText(section.id, 'groups') }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Тренеров</span>
              <span class="stat-value">{{ statText(section.id, 'coaches') }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Спортсменов</span>
              <span class="stat-value">{{ statText(section.id, 'athletes') }}</span>
            </div>
          </div>
        </div>
      </div>

      <BaseModal
        v-model="showModal"
        :title="editing ? 'Изменить секцию' : 'Добавить секцию'"
        :submit-label="saving ? 'Сохранение…' : 'Сохранить'"
        @submit="handleSubmit"
      >
        <BaseInput id="section-name" v-model="form.name" label="Название секции" placeholder="Спортивное плавание" />
        <BaseInput id="section-description" v-model="form.description" label="Описание" placeholder="Тренировки в бассейне 25м" />
        <div :class="{ 'row-2': editing }">
          <div class="field">
            <label class="field-label" for="section-sport-type">Вид спорта</label>
            <select id="section-sport-type" v-model="form.sportTypeId" class="field-control">
              <option value="" disabled>Выберите вид спорта</option>
              <option v-for="type in sportTypeOptions" :key="type.id" :value="type.id">{{ type.name }}</option>
            </select>
          </div>
          <div v-if="editing" class="field">
            <label class="field-label" for="section-status">Статус</label>
            <select id="section-status" v-model="form.status" class="field-control">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </div>
        </div>
        <p v-if="sportTypesError" class="form-error">Не удалось загрузить виды спорта: {{ sportTypesError }}</p>
        <p v-else-if="!sportTypeOptions.length" class="form-hint">
          Сначала добавьте вид спорта в
          <router-link to="/trainer/management/directories">справочниках</router-link>.
        </p>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { sectionsApi } from '../../api/sections'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { getOrganizationId, hasPermission } from '../../utils/session'
import { errorText } from '../../utils/format'
import { label, options, tone } from '../../utils/labels'

const ALL = 'all'
const FIELD_LABELS = { name: 'Название', sportTypeId: 'Вид спорта', description: 'Описание', status: 'Статус' }
const statusOptions = options('recordStatus')

const canWrite = computed(() => hasPermission('sections.write'))

// ─────────── Секции: поиск (q) и вкладка вида спорта (sportTypeId) — параметры сервера ───────────
const searchQuery = ref('')
const activeTab = ref(ALL)
const sections = ref([])
const loading = ref(true)
const loadError = ref('')

let loadSeq = 0
async function loadSections() {
  const seq = ++loadSeq
  loading.value = true
  loadError.value = ''
  try {
    const items = await fetchAll(params => sectionsApi.list(getOrganizationId(), params), {
      q: searchQuery.value.trim(),
      sportTypeId: activeTab.value === ALL ? null : activeTab.value
    })
    if (seq === loadSeq) sections.value = items
  } catch (e) {
    if (seq === loadSeq) {
      sections.value = []
      loadError.value = errorText(e)
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// Подзаголовок: число активных секций без учёта поиска и вкладки
const activeCount = ref(null)
async function loadActiveCount() {
  try {
    const page = await sectionsApi.list(getOrganizationId(), { status: 'ACTIVE', size: 1 })
    activeCount.value = page.totalElements
  } catch (_) {
    activeCount.value = null
  }
}

const subtitle = computed(() => {
  const n = activeCount.value
  if (n === null) return 'Секции клуба'
  return `${n} ${plural(n, 'активная секция', 'активные секции', 'активных секций')} под управлением клуба`
})

const emptyMessage = computed(() => {
  const q = searchQuery.value.trim()
  if (q) return `Ничего не найдено по запросу «${q}»`
  if (activeTab.value !== ALL) return 'В этом виде спорта секций пока нет'
  return canWrite.value ? 'Секций пока нет — добавьте первую' : 'Секций пока нет'
})

// ─────────── Виды спорта (справочник sport-types): вкладки, подписи, выбор в форме ───────────
const sportTypes = ref([])
const sportTypesError = ref('')

async function loadSportTypes() {
  try {
    sportTypes.value = await fetchAll(params => dictionariesApi.list(getOrganizationId(), 'sport-types', params))
    sportTypesError.value = ''
  } catch (e) {
    sportTypes.value = []
    sportTypesError.value = errorText(e)
  }
}

const tabs = computed(() => [
  { id: ALL, name: 'Все' },
  ...sportTypes.value.filter(t => t.status === 'ACTIVE').map(t => ({ id: t.id, name: t.name }))
])

function sportTypeName(id) {
  return sportTypes.value.find(t => t.id === id)?.name || ''
}

function sectionDescription(section) {
  return [sportTypeName(section.sportTypeId), section.description].filter(Boolean).join(' · ') || 'Описание не заполнено'
}

// ─────────── Сводка по секциям: считаем по активным группам ───────────
const groups = ref([])
const groupsLoaded = ref(false)

async function loadGroups() {
  try {
    groups.value = await fetchAll(params => groupsApi.list(getOrganizationId(), params), { status: 'ACTIVE' })
    groupsLoaded.value = true
  } catch (_) {
    groups.value = []
    groupsLoaded.value = false
  }
}

const statsBySection = computed(() => {
  const stats = {}
  for (const group of groups.value) {
    const s = stats[group.sectionId] || (stats[group.sectionId] = { groups: 0, coaches: new Set(), athletes: 0 })
    s.groups += 1
    for (const id of group.coachIds || []) s.coaches.add(id)
    s.athletes += group.athleteCount || 0
  }
  return stats
})

const STAT_WORDS = {
  groups: n => plural(n, 'группа', 'группы', 'групп'),
  coaches: n => plural(n, 'тренер', 'тренера', 'тренеров'),
  athletes: n => plural(n, 'человек', 'человека', 'человек')
}

function statText(sectionId, key) {
  if (!groupsLoaded.value) return '—'
  const s = statsBySection.value[sectionId]
  const n = !s ? 0 : key === 'coaches' ? s.coaches.size : s[key]
  return `${n} ${STAT_WORDS[key](n)}`
}

// ─────────── Создание и изменение секции ───────────
const showModal = ref(false)
const editing = ref(null)
const saving = ref(false)
const formError = ref('')
const form = reactive({ name: '', description: '', sportTypeId: '', status: 'ACTIVE' })

// Архивный вид спорта нельзя выбрать заново, но у редактируемой секции он остаётся в списке
const sportTypeOptions = computed(() =>
  sportTypes.value.filter(t => t.status === 'ACTIVE' || t.id === editing.value?.sportTypeId))

function openCreate() {
  editing.value = null
  Object.assign(form, {
    name: '',
    description: '',
    sportTypeId: activeTab.value !== ALL ? activeTab.value : '',
    status: 'ACTIVE'
  })
  formError.value = ''
  showModal.value = true
}

function openEdit(section) {
  editing.value = section
  Object.assign(form, {
    name: section.name,
    description: section.description || '',
    sportTypeId: section.sportTypeId,
    status: section.status
  })
  formError.value = ''
  showModal.value = true
}

async function handleSubmit() {
  if (saving.value) return
  const name = form.name.trim()
  const description = form.description.trim() || null
  if (!name) { formError.value = 'Укажите название секции.'; return }
  if (!form.sportTypeId) { formError.value = 'Выберите вид спорта.'; return }

  const current = editing.value
  if (current && form.status === 'ARCHIVED' && current.status !== 'ARCHIVED' &&
      !confirm(`Архивировать секцию «${current.name}»? История занятий и финансов сохранится.`)) return

  saving.value = true
  formError.value = ''
  try {
    const org = getOrganizationId()
    if (current) {
      // PATCH: только изменённые поля (пустой PATCH сервер отклоняет)
      const patch = {}
      if (name !== current.name) patch.name = name
      if (form.sportTypeId !== current.sportTypeId) patch.sportTypeId = form.sportTypeId
      if (description !== (current.description || null)) patch.description = description
      if (form.status !== current.status) patch.status = form.status
      if (Object.keys(patch).length) await sectionsApi.update(org, current.id, patch)
    } else {
      await sectionsApi.create(org, { name, sportTypeId: form.sportTypeId, description, status: 'ACTIVE' })
    }
    showModal.value = false
    await Promise.all([loadSections(), loadActiveCount()])
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
  searchTimer = setTimeout(loadSections, 300)
})
watch(activeTab, loadSections)
onBeforeUnmount(() => clearTimeout(searchTimer))

onMounted(() => {
  loadSections()
  loadActiveCount()
  loadSportTypes()
  loadGroups()
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

.sections-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 16px; }
.section-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 16px; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
}
.section-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.section-header h3 { font-size: 18px; font-weight: 700; color: #152421; }
.header-side { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.icon-btn {
  width: 32px; height: 32px;
  background: white; border: 1px solid #E3EAE8; border-radius: 10px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}
.status-badge { padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-blue { background: #DDECFB; color: #35678E; }
.status-gray { background: #EEF1F0; color: #888888; }
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
.form-hint { font-size: 13px; color: #6D7D79; }
.form-hint a { color: #35678E; }
.form-error { font-size: 13px; color: #D64545; }
</style>
