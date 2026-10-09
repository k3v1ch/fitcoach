<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Спортсмены"
        :subtitle="subtitle"
        v-model="searchQuery"
        search-placeholder="Поиск по ФИО..."
      >
        <template #actions>
          <BaseButton v-if="canWrite" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить атлета
          </BaseButton>
        </template>
      </PageHeader>

      <!-- Фильтры -->
      <div class="filter-panel">
        <div class="filter-group">
          <label for="athletes-filter-group">Группа</label>
          <select id="athletes-filter-group" v-model="filters.groupId">
            <option value="">Все</option>
            <option v-for="g in groupOptions" :key="g.id" :value="g.id">
              {{ g.name }}{{ g.status === 'ARCHIVED' ? ' (архив)' : '' }}
            </option>
          </select>
        </div>
        <div class="filter-group">
          <label for="athletes-filter-section">Секция</label>
          <select id="athletes-filter-section" v-model="filters.sectionId">
            <option value="">Все</option>
            <option v-for="s in sectionOptions" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="filter-group">
          <label for="athletes-filter-status">Статус</label>
          <select id="athletes-filter-status" v-model="filters.status">
            <option value="">Все</option>
            <option v-for="o in statusOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <div class="filter-group sort-group">
          <label>Сортировка</label>
          <button class="sort-btn" @click="sortAsc = !sortAsc">
            ФИО
            <BaseIcon name="chevron-down" :size="12" :style="{ transform: sortAsc ? 'rotate(180deg)' : 'none' }" />
          </button>
        </div>
      </div>

      <!-- Таблица -->
      <div class="table-card">
        <p v-if="refError" class="ref-error">{{ refError }}</p>

        <StateBlock v-if="loading" kind="loading" />
        <StateBlock v-else-if="loadError" kind="error" :message="loadError" />
        <StateBlock v-else-if="paginatedAthletes.length === 0" kind="empty" :message="emptyMessage" />
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
              <div class="avatar-small">{{ initials(fullName(athlete)) }}</div>
              <div class="name-info">
                <router-link :to="`/trainer/athletes/${athlete.id}`" class="name-link">
                  {{ fullName(athlete) }}
                </router-link>
                <span class="age">{{ ageText(athlete) }}</span>
              </div>
            </div>
            <div class="col group">{{ groupText(athlete) }}</div>
            <div class="col section">{{ sectionText(athlete) }}</div>
            <div class="col coach">{{ coachText(athlete) }}</div>
            <div class="col date">{{ formatDateShort(athlete.enrolledOn) }}</div>
            <div class="col status">
              <span class="status-badge" :class="`status-${tone(athlete.status)}`">
                {{ label('athleteStatus', athlete.status) }}
              </span>
            </div>
          </div>
        </template>

        <!-- Пагинация -->
        <div v-if="!loading && !loadError && totalPages > 1" class="pagination">
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
        :submit-label="saving ? 'Сохранение…' : 'Сохранить'"
        @submit="handleSubmit"
      >
        <BaseInput id="athlete-last-name" v-model="form.lastName" label="Фамилия" placeholder="Иванов" />
        <BaseInput id="athlete-first-name" v-model="form.firstName" label="Имя" placeholder="Иван" />
        <BaseInput id="athlete-middle-name" v-model="form.middleName" label="Отчество (необязательно)" placeholder="Иванович" />
        <BaseInput id="athlete-birth-date" v-model="form.birthDate" type="date" label="Дата рождения" />
        <BaseInput id="athlete-enrolled-on" v-model="form.enrolledOn" type="date" label="Дата зачисления" />
        <template v-if="canLinkAccount">
          <BaseInput
            id="athlete-email"
            v-model="form.email"
            type="email"
            label="Email аккаунта спортсмена (необязательно)"
            placeholder="name@example.ru"
          />
          <p class="form-hint">
            Аккаунт должен быть зарегистрирован и подтверждён — тогда спортсмен увидит свою карточку, расписание и начисления.
          </p>
        </template>
        <div class="field">
          <label class="field-label" for="athlete-note">Заметка (необязательно)</label>
          <textarea id="athlete-note" v-model="form.note" class="field-control field-textarea" rows="3"></textarea>
        </div>
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
import { athletesApi } from '../../api/athletes'
import { groupsApi } from '../../api/groups'
import { sectionsApi } from '../../api/sections'
import { organizationsApi } from '../../api/organizations'
import { getOrganizationId, hasPermission, hasRole, currentUser } from '../../utils/session'
import { errorText, formatDateShort, fullName, initials, ageYears, toIsoDate } from '../../utils/format'
import { label, options, tone } from '../../utils/labels'

const PER_PAGE = 20
const MAX_PAGES = 20 // по 100 записей: до 2000 спортсменов
const DETAIL_CONCURRENCY = 4
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const FIELD_LABELS = {
  firstName: 'Имя', lastName: 'Фамилия', middleName: 'Отчество', birthDate: 'Дата рождения',
  enrolledOn: 'Дата зачисления', status: 'Статус', note: 'Заметка', userId: 'Аккаунт', email: 'Email'
}
const statusOptions = options('athleteStatus')

// Запись — только роль тренера с нужным правом (сервер проверяет то же)
const canWrite = computed(() => hasRole('TRAINER') && hasPermission('athletes.write'))
const canLinkAccount = computed(() => canWrite.value && hasPermission('members.write'))

// ─────────── Состояние ───────────
const searchQuery = ref('')
const filters = reactive({ groupId: '', sectionId: '', status: '' })
const sortAsc = ref(true)
const currentPage = ref(1)
const loading = ref(false)
const loadError = ref('')
const items = ref([])
const total = ref(0)

// Справочники для колонок «Группа», «Секция», «Тренер»: в Athlete групп нет — составы берём из GroupDetail
const sections = ref([])
const groups = ref([])
const groupsByAthlete = ref({})
const trainers = ref([])
const refError = ref('')

// ─────────── Загрузка с API ───────────
// Все страницы списка (size ≤ 100), последовательно — чтобы не упираться в лимит частоты запросов
async function fetchAll(request, params = {}) {
  const first = await request({ ...params, page: 0, size: 100 })
  const list = [...(first.items || [])]
  const pages = Math.min(first.totalPages || 1, MAX_PAGES)
  for (let page = 1; page < pages; page++) {
    const next = await request({ ...params, page, size: 100 })
    list.push(...(next.items || []))
  }
  return { items: list, total: first.totalElements ?? list.length }
}

async function mapLimit(list, limit, fn) {
  const out = new Array(list.length)
  let next = 0
  async function worker() {
    while (next < list.length) {
      const index = next++
      out[index] = await fn(list[index])
    }
  }
  await Promise.all(Array.from({ length: Math.min(limit, list.length) }, worker))
  return out
}

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  loading.value = true
  loadError.value = ''
  try {
    const res = await fetchAll(params => athletesApi.list(getOrganizationId(), params), {
      q: searchQuery.value.trim() || undefined,
      status: filters.status || undefined
    })
    if (seq !== loadSeq) return
    items.value = res.items
    total.value = res.total
  } catch (e) {
    if (seq !== loadSeq) return
    items.value = []
    total.value = 0
    loadError.value = errorText(e)
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

async function loadReference() {
  const org = getOrganizationId()
  const problems = []
  const [sectionRes, groupRes, trainerRes] = await Promise.allSettled([
    fetchAll(params => sectionsApi.list(org, params)),
    fetchAll(params => groupsApi.list(org, params)),
    fetchAll(params => organizationsApi.members(org, params), { role: 'TRAINER' })
  ])

  if (sectionRes.status === 'fulfilled') sections.value = sectionRes.value.items
  else problems.push(`секции (${errorText(sectionRes.reason)})`)

  // Нет members.read — имена тренеров просто не показываем
  if (trainerRes.status === 'fulfilled') trainers.value = trainerRes.value.items
  else if (trainerRes.reason?.status !== 403) problems.push(`тренеров (${errorText(trainerRes.reason)})`)

  if (groupRes.status === 'fulfilled') {
    groups.value = groupRes.value.items
    try {
      const details = await mapLimit(groups.value, DETAIL_CONCURRENCY, g => groupsApi.get(org, g.id))
      const map = {}
      details.forEach((detail, index) => {
        const roster = Array.isArray(detail?.athletes) ? detail.athletes : (detail?.athletes?.items || [])
        for (const member of roster) {
          if (!map[member.athleteId]) map[member.athleteId] = []
          map[member.athleteId].push(groups.value[index])
        }
      })
      groupsByAthlete.value = map
    } catch (e) {
      problems.push(`составы групп (${errorText(e)})`)
    }
  } else {
    problems.push(`группы (${errorText(groupRes.reason)})`)
  }

  refError.value = problems.length ? `Не удалось загрузить ${problems.join(', ')} — колонки могут быть неполными.` : ''
}

// Поиск — серверный, с задержкой; статус — серверный, сразу
let searchTimer = null
watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    load()
  }, 300)
})
watch(() => filters.status, () => {
  currentPage.value = 1
  load()
})
watch(() => [filters.groupId, filters.sectionId, sortAsc.value], () => {
  currentPage.value = 1
})
// Группа другой секции в фильтре не имеет смысла — сбрасываем
watch(() => filters.sectionId, () => {
  if (filters.groupId && !groupOptions.value.some(g => g.id === filters.groupId)) filters.groupId = ''
})

onMounted(() => {
  load()
  loadReference()
})
onBeforeUnmount(() => clearTimeout(searchTimer))

// ─────────── Вычисляемые ───────────
const sectionNameById = computed(() => Object.fromEntries(sections.value.map(s => [s.id, s.name])))
const trainerNameById = computed(() => Object.fromEntries(trainers.value.map(m => [m.userId, m.fullName])))

const byName = (a, b) => (a.name || '').localeCompare(b.name || '', 'ru')
const sectionOptions = computed(() => [...sections.value].sort(byName))
const groupOptions = computed(() => groups.value
  .filter(g => !filters.sectionId || g.sectionId === filters.sectionId)
  .sort((a, b) => (a.status === b.status ? byName(a, b) : a.status === 'ACTIVE' ? -1 : 1)))

// Текущие группы спортсмена: сначала действующие
function athleteGroups(a) {
  const list = groupsByAthlete.value[a.id] || []
  return [...list].sort((x, y) => (x.status === y.status ? byName(x, y) : x.status === 'ACTIVE' ? -1 : 1))
}

const filteredAthletes = computed(() => {
  const result = items.value.filter(a => {
    const list = groupsByAthlete.value[a.id] || []
    if (filters.groupId && !list.some(g => g.id === filters.groupId)) return false
    if (filters.sectionId && !list.some(g => g.sectionId === filters.sectionId)) return false
    return true
  })
  return result.sort((a, b) => {
    const cmp = fullName(a).localeCompare(fullName(b), 'ru')
    return sortAsc.value ? cmp : -cmp
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredAthletes.value.length / PER_PAGE)))
watch(totalPages, pages => {
  if (currentPage.value > pages) currentPage.value = pages
})

const paginatedAthletes = computed(() => {
  const start = (currentPage.value - 1) * PER_PAGE
  return filteredAthletes.value.slice(start, start + PER_PAGE)
})

const subtitle = computed(() => {
  if (loading.value && !items.value.length) return 'Загрузка…'
  const n = total.value
  if (searchQuery.value.trim() || filters.status) return `Найдено: ${n} ${plural(n, 'спортсмен', 'спортсмена', 'спортсменов')}`
  return `${n} ${plural(n, 'спортсмен', 'спортсмена', 'спортсменов')} под вашим контролем`
})

const emptyMessage = computed(() => {
  const filtered = searchQuery.value.trim() || filters.status || filters.groupId || filters.sectionId
  if (filtered) return 'Спортсменов не найдено — измените поиск или фильтры'
  return canWrite.value ? 'Спортсменов пока нет — добавьте первого' : 'Спортсменов пока нет'
})

// ─────────── Форматтеры ───────────
function plural(n, one, few, many) {
  const mod10 = n % 10
  const mod100 = n % 100
  if (mod10 === 1 && mod100 !== 11) return one
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few
  return many
}
function ageText(a) {
  const age = ageYears(a.birthDate)
  return age === null ? '—' : `${age} ${plural(age, 'год', 'года', 'лет')}`
}
function groupText(a) {
  const list = athleteGroups(a)
  if (!list.length) return '—'
  return list.length > 1 ? `${list[0].name} +${list.length - 1}` : list[0].name
}
function sectionText(a) {
  const first = athleteGroups(a)[0]
  return (first && sectionNameById.value[first.sectionId]) || '—'
}
function coachText(a) {
  const first = athleteGroups(a)[0]
  if (!first) return '—'
  const names = (first.coachIds || [])
    .map(id => trainerNameById.value[id] || (id === currentUser.value?.userId ? currentUser.value.fullName : null))
    .filter(Boolean)
  return names.length ? names.join(', ') : '—'
}
// Ошибка формы: имена полей из fieldErrors — по-русски
function formErrorText(e) {
  const fieldErrors = (e?.fieldErrors || []).map(f => ({ ...f, field: FIELD_LABELS[f.field] || f.field }))
  return errorText({ status: e?.status, message: e?.message, fieldErrors })
}

// ─────────── Создание ───────────
const showModal = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive({
  lastName: '',
  firstName: '',
  middleName: '',
  birthDate: '',
  enrolledOn: '',
  email: '',
  note: ''
})

function openCreate() {
  Object.assign(form, {
    lastName: '', firstName: '', middleName: '', birthDate: '', enrolledOn: toIsoDate(new Date()), email: '', note: ''
  })
  formError.value = ''
  showModal.value = true
}

async function handleSubmit() {
  if (saving.value) return
  formError.value = ''
  const lastName = form.lastName.trim()
  const firstName = form.firstName.trim()
  const email = canLinkAccount.value ? form.email.trim() : ''
  const today = toIsoDate(new Date())
  if (!lastName || !firstName) {
    formError.value = 'Укажите фамилию и имя.'
    return
  }
  if (!form.birthDate || !form.enrolledOn) {
    formError.value = 'Укажите дату рождения и дату зачисления.'
    return
  }
  if (form.birthDate > today) {
    formError.value = 'Дата рождения не может быть в будущем.'
    return
  }
  if (form.enrolledOn < form.birthDate) {
    formError.value = 'Дата зачисления не может быть раньше даты рождения.'
    return
  }
  if (email && !EMAIL_RE.test(email)) {
    formError.value = 'Проверьте email аккаунта спортсмена.'
    return
  }

  saving.value = true
  try {
    const org = getOrganizationId()
    // Аккаунт по email получает роль ATHLETE, его userId записывается в карточку
    const userId = email ? (await organizationsApi.addAthlete(org, email)).userId : null
    await athletesApi.create(org, {
      firstName,
      lastName,
      middleName: form.middleName.trim() || null,
      birthDate: form.birthDate,
      userId,
      status: 'ACTIVE',
      enrolledOn: form.enrolledOn,
      note: form.note.trim() || null
    })
    showModal.value = false
    currentPage.value = 1
    await load()
  } catch (e) {
    formError.value = formErrorText(e)
  } finally {
    saving.value = false
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
.status-blue { background: #DDECFB; color: #3B82F6; }
.status-gray { background: #EEF1F0; color: #888888; }

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

.ref-error { font-size: 13px; color: #D64545; }

/* Форма в модальном окне — в стиле BaseInput */
.field { display: flex; flex-direction: column; gap: 6px; width: 100%; }
.field-label { font-size: 12px; color: var(--color-gray-text); font-weight: 500; }
.field-control {
  width: 100%; min-height: 40px; padding: 8px 12px;
  border: 1px solid var(--color-gray-border); border-radius: 8px;
  font-size: 14px; font-family: inherit; color: var(--color-dark);
  background: var(--color-white); outline: none;
}
.field-control:focus { border-color: var(--color-primary); }
.field-textarea { resize: vertical; }
.form-hint { font-size: 12px; color: #6D7D79; line-height: 1.5; }
.form-error { font-size: 13px; color: #D64545; }
</style>
