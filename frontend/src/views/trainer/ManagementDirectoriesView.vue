<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Справочники системы"
        subtitle="Настройка основных параметров и классификаторов"
        v-model="searchQuery"
        search-placeholder="Поиск по справочникам..."
      >
        <template #actions>
          <BaseButton v-if="canWrite" @click="openCreate()">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить запись
          </BaseButton>
        </template>
      </PageHeader>

      <div class="tabs-row">
        <button class="tab-btn" @click="$router.push('/trainer/management/users')">
          Пользователи
        </button>
        <button class="tab-btn active">Справочники</button>
      </div>

      <div class="directories-grid">
        <div v-for="dir in visibleDirectories" :key="dir.key" class="directory-card">
          <div class="card-header">
            <div class="icon-box">
              <BaseIcon :name="dir.icon" :size="22" color="#102522" />
            </div>
            <span class="count">{{ badgeText(dir) }}</span>
          </div>
          <div class="card-body">
            <h3>{{ dir.title }}</h3>
            <StateBlock v-if="dir.api && counts[dir.key].error" kind="error" :message="counts[dir.key].error" />
            <p v-else>{{ dir.description }}</p>
          </div>
          <div class="card-footer">
            <button v-if="dir.api" class="edit-btn" @click="openList(dir.key)">
              <BaseIcon :name="canWrite ? 'edit' : 'file-text'" :size="14" color="#152421" />
              {{ canWrite ? 'Редактировать' : 'Открыть' }}
            </button>
            <span v-else class="footer-note">{{ dir.footer }}</span>
          </div>
        </div>
      </div>

      <!-- Значения справочника -->
      <BaseModal
        v-model="list.open"
        :title="directoryTitle(list.type)"
        :width="640"
        :submit-label="canWrite ? 'Добавить запись' : 'Закрыть'"
        @submit="onListSubmit"
      >
        <p v-if="searchQuery.trim()" class="form-hint">Показаны записи по запросу «{{ searchQuery.trim() }}».</p>
        <StateBlock v-if="list.loading" kind="loading" />
        <StateBlock v-else-if="list.error" kind="error" :message="list.error" />
        <StateBlock v-else-if="!list.items.length" kind="empty" :message="searchQuery.trim() ? 'Ничего не найдено' : 'Записей пока нет'" />
        <div v-else class="item-list">
          <div v-for="item in list.items" :key="item.id" class="item-row">
            <div class="item-main">
              <div class="item-name">{{ item.name }}</div>
              <div v-if="itemMeta(item)" class="item-meta">{{ itemMeta(item) }}</div>
            </div>
            <span class="status-badge" :class="`status-${tone(item.status)}`">{{ label('recordStatus', item.status) }}</span>
            <button v-if="canWrite" class="edit-btn" @click="openEdit(item)">
              <BaseIcon name="edit" :size="14" color="#152421" />
              Изменить
            </button>
          </div>
        </div>
      </BaseModal>

      <!-- Добавление и изменение значения (объявлено после списка — открывается поверх него) -->
      <BaseModal
        v-model="form.open"
        :title="form.id ? 'Изменить запись' : 'Новая запись'"
        :submit-label="form.saving ? 'Сохранение…' : 'Сохранить'"
        @submit="submitForm"
      >
        <div v-if="!form.id" class="field">
          <label class="field-label" for="dictionary-type">Справочник</label>
          <select id="dictionary-type" v-model="form.type" class="field-control">
            <option v-for="dir in apiDirectories" :key="dir.key" :value="dir.key">{{ dir.title }}</option>
          </select>
        </div>
        <p v-else class="form-hint">Справочник: {{ directoryTitle(form.type) }}</p>
        <BaseInput id="dictionary-name" v-model="form.name" label="Название" :placeholder="namePlaceholder" />
        <BaseInput v-if="form.type === 'venues'" id="dictionary-address" v-model="form.address" label="Адрес" placeholder="ул. Спортивная, 1" />
        <BaseInput id="dictionary-description" v-model="form.description" label="Описание" placeholder="Необязательно" />
        <div class="row-2">
          <BaseInput id="dictionary-sort-order" v-model="form.sortOrder" type="number" label="Порядок в списках" placeholder="0" />
          <div class="field">
            <label class="field-label" for="dictionary-status">Статус</label>
            <select id="dictionary-status" v-model="form.status" class="field-control">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </div>
        </div>
        <p v-if="form.error" class="form-error">{{ form.error }}</p>
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
import { dictionariesApi } from '../../api/dictionaries'
import { getOrganizationId, hasPermission } from '../../utils/session'
import { errorText } from '../../utils/format'
import { label, options, tone } from '../../utils/labels'

const FIELD_LABELS = { name: 'Название', description: 'Описание', address: 'Адрес', sortOrder: 'Порядок', status: 'Статус' }
const statusOptions = options('recordStatus')
const systemValues = kind => options(kind).map(o => o.label).join(', ')

// Справочники API (sport-types, training-types, venues) и блоки макета, которых в API нет
const DIRECTORIES = [
  { key: 'sport-types', api: true, title: 'Виды спорта', icon: 'award', placeholder: 'Плавание',
    description: 'Каждая секция относится к одному виду спорта.' },
  { key: 'training-types', api: true, title: 'Типы тренировок', icon: 'activity', placeholder: 'Силовая тренировка',
    description: 'Тип занятия выбирается при планировании тренировки.' },
  { key: 'venues', api: true, title: 'Площадки и залы', icon: 'map', placeholder: 'Бассейн 25 м',
    description: 'Места проведения тренировок с адресами.' },
  { key: 'expense-categories', api: false, title: 'Категории расходов', icon: 'credit-card', badge: 'Скоро',
    description: 'Раздел появится позже.', footer: 'Появится позже' },
  { key: 'document-types', api: false, title: 'Типы документов', icon: 'file-text', badge: 'Системный',
    description: `Значения задаёт система: ${systemValues('documentType')}.`, footer: 'Изменить нельзя' },
  { key: 'athlete-statuses', api: false, title: 'Статусы спортсменов', icon: 'users', badge: 'Системный',
    description: `Значения задаёт система: ${systemValues('athleteStatus')}.`, footer: 'Изменить нельзя' }
]
const apiDirectories = DIRECTORIES.filter(d => d.api)

const canWrite = computed(() => hasPermission('dictionaries.write'))
const searchQuery = ref('')

// При поиске показываем только справочники API — по ним ищет сервер (q)
const visibleDirectories = computed(() => (searchQuery.value.trim() ? apiDirectories : DIRECTORIES))

function directoryTitle(type) {
  return DIRECTORIES.find(d => d.key === type)?.title || 'Справочник'
}

// ─────────── Количество записей в каждом справочнике ───────────
const counts = reactive(Object.fromEntries(apiDirectories.map(d => [d.key, { loading: true, error: '', total: null }])))

let countSeq = 0
async function loadCounts() {
  const seq = ++countSeq
  const q = searchQuery.value.trim()
  await Promise.all(apiDirectories.map(async dir => {
    const state = counts[dir.key]
    state.loading = true
    state.error = ''
    try {
      const page = await dictionariesApi.list(getOrganizationId(), dir.key, { q, size: 1 })
      if (seq === countSeq) state.total = page.totalElements
    } catch (e) {
      if (seq === countSeq) {
        state.total = null
        state.error = errorText(e)
      }
    } finally {
      if (seq === countSeq) state.loading = false
    }
  }))
}

function badgeText(dir) {
  if (!dir.api) return dir.badge
  const state = counts[dir.key]
  if (state.loading) return '…'
  if (state.error || state.total === null) return '—'
  if (searchQuery.value.trim()) return `Найдено: ${state.total}`
  return `${state.total} ${plural(state.total, 'запись', 'записи', 'записей')}`
}

// ─────────── Список значений справочника ───────────
const list = reactive({ open: false, type: null, loading: false, error: '', items: [] })

function openList(type) {
  Object.assign(list, { open: true, type, error: '', items: [] })
  loadList()
}

let listSeq = 0
async function loadList() {
  const seq = ++listSeq
  list.loading = true
  list.error = ''
  try {
    const items = await fetchAll(
      params => dictionariesApi.list(getOrganizationId(), list.type, params),
      { q: searchQuery.value.trim() }
    )
    if (seq === listSeq) list.items = items
  } catch (e) {
    if (seq === listSeq) {
      list.items = []
      list.error = errorText(e)
    }
  } finally {
    if (seq === listSeq) list.loading = false
  }
}

function itemMeta(item) {
  return [item.address, item.description].filter(Boolean).join(' · ')
}

function onListSubmit() {
  if (canWrite.value) openCreate(list.type)
  else list.open = false
}

// ─────────── Добавление и изменение значения ───────────
const form = reactive({
  open: false, saving: false, error: '',
  id: null, original: null, type: 'sport-types',
  name: '', description: '', address: '', sortOrder: '0', status: 'ACTIVE'
})

const namePlaceholder = computed(() => DIRECTORIES.find(d => d.key === form.type)?.placeholder || '')

function openCreate(type) {
  Object.assign(form, {
    open: true, saving: false, error: '',
    id: null, original: null, type: type || 'sport-types',
    name: '', description: '', address: '', sortOrder: '0', status: 'ACTIVE'
  })
}

function openEdit(item) {
  Object.assign(form, {
    open: true, saving: false, error: '',
    id: item.id, original: item, type: item.dictionaryType || list.type,
    name: item.name,
    description: item.description || '',
    address: item.address || '',
    sortOrder: String(item.sortOrder ?? 0),
    status: item.status
  })
}

async function submitForm() {
  if (form.saving) return
  const name = form.name.trim()
  const sortText = String(form.sortOrder ?? '').trim()
  if (!name) { form.error = 'Укажите название.'; return }
  if (sortText && !/^-?\d{1,6}$/.test(sortText)) { form.error = 'Порядок — целое число (до 6 цифр).'; return }

  const original = form.original
  if (original && form.status === 'ARCHIVED' && original.status !== 'ARCHIVED' &&
      !confirm(`Архивировать «${original.name}»? Значение нельзя будет выбрать в новых записях; существующие записи сохранятся.`)) return

  // PATCH справочника в коде бэкенда заменяет все поля записи — всегда отправляем её целиком.
  // address принимается только для venues.
  const body = {
    name,
    description: form.description.trim() || null,
    sortOrder: sortText ? Number(sortText) : 0,
    status: form.status
  }
  if (form.type === 'venues') body.address = form.address.trim() || null

  form.saving = true
  form.error = ''
  try {
    const org = getOrganizationId()
    if (form.id) await dictionariesApi.update(org, form.type, form.id, body)
    else await dictionariesApi.create(org, form.type, body)
    form.open = false
    loadCounts()
    if (list.open && list.type === form.type) loadList()
  } catch (e) {
    form.error = formErrorText(e)
  } finally {
    form.saving = false
  }
}

// ─────────── Жизненный цикл ───────────
let searchTimer = null
watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadCounts, 300)
})
onBeforeUnmount(() => clearTimeout(searchTimer))

onMounted(loadCounts)

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

.tabs-row { display: flex; gap: 8px; padding-bottom: 12px; border-bottom: 1px solid #E3EAE8; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 14px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }

.directories-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.directory-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.icon-box {
  width: 48px; height: 48px;
  background: #E8F5E9; border-radius: 12px;
  display: flex; justify-content: center; align-items: center;
}
.count {
  padding: 4px 10px; background: #F4F7F8;
  border-radius: 20px; font-size: 12px;
  font-weight: 600; color: #6D7D79;
}
.card-body { display: flex; flex-direction: column; gap: 8px; }
.card-body h3 { font-size: 18px; font-weight: 700; color: #152421; }
.card-body p { font-size: 13px; color: #6D7D79; }

.card-footer { padding-top: 16px; border-top: 1px solid #E3EAE8; }
.edit-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 13px; font-weight: 700; color: #152421;
  cursor: pointer;
}
.footer-note { display: inline-block; padding: 10px 0; font-size: 13px; font-weight: 600; color: #98A6A2; }

/* Список значений в модальном окне */
.item-list { display: flex; flex-direction: column; gap: 10px; }
.item-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border: 1px solid #E3EAE8; border-radius: 12px;
}
.item-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.item-name { font-size: 14px; font-weight: 700; color: #152421; }
.item-meta { font-size: 12px; color: #6D7D79; }
.item-row .edit-btn { padding: 8px 12px; flex-shrink: 0; }
.status-badge { padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; white-space: nowrap; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-gray { background: #EEF1F0; color: #888888; }

/* Поля формы в стиле BaseInput */
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
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
.form-error { font-size: 13px; color: #D64545; }
</style>
