<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Управление пользователями"
        subtitle="Управление правами доступа и ролями"
        v-model="searchQuery"
        search-placeholder="Поиск по имени..."
      >
        <template #actions>
          <BaseButton v-if="canAdd" @click="openAdd">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить пользователя
          </BaseButton>
        </template>
      </PageHeader>

      <div class="tabs-row">
        <button class="tab-btn active">Пользователи</button>
        <button class="tab-btn" @click="$router.push('/trainer/management/directories')">
          Справочники
        </button>
      </div>

      <div class="filter-bar">
        <select v-model="filters.role" class="filter-select" aria-label="Роль">
          <option value="">Все роли</option>
          <option v-for="o in roleOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <select v-model="filters.status" class="filter-select" aria-label="Статус">
          <option value="">Все статусы</option>
          <option v-for="o in statusOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
      </div>

      <p v-if="notice" class="notice">
        {{ notice }}
        <router-link to="/trainer/athletes">Открыть спортсменов →</router-link>
      </p>

      <div class="table-card">
        <div class="table-header">
          <div class="col user">Пользователь</div>
          <div class="col email">Email</div>
          <div class="col role">Роль</div>
          <div class="col status">Статус</div>
          <div class="col last">Карточки</div>
          <div class="col actions"></div>
        </div>
        <StateBlock v-if="loading" kind="loading" />
        <StateBlock v-else-if="loadError" kind="error" :message="loadError" />
        <StateBlock v-else-if="!members.length" kind="empty" :message="emptyMessage" />
        <template v-else>
          <div v-for="user in members" :key="user.userId" class="table-row">
            <div class="col user">
              <div class="avatar">{{ initials(user.fullName) }}</div>
              <span>{{ user.fullName || '—' }}<span v-if="user.userId === myId" class="me-mark"> (вы)</span></span>
            </div>
            <div class="col email">{{ emailOf(user) }}</div>
            <div class="col role">
              <span v-for="role in user.roles" :key="role" class="role-badge" :class="ROLE_CLASS[role] || 'role-gray'">{{ label('role', role) }}</span>
            </div>
            <div class="col status">
              <span class="dot" :style="{ background: STATUS_COLOR[user.status] || '#98A6A2' }"></span>
              {{ label('memberStatus', user.status) }}
            </div>
            <div class="col last">
              <template v-if="cardsOf(user).length">
                <router-link
                  v-for="card in cardsOf(user)"
                  :key="card.id"
                  :to="`/trainer/athletes/${card.id}`"
                  class="card-link"
                >{{ card.label }}</router-link>
              </template>
              <span v-else>—</span>
            </div>
            <div class="col actions">
              <button
                v-if="cardsOf(user).length === 1"
                class="icon-btn"
                title="Открыть карточку спортсмена"
                @click="$router.push(`/trainer/athletes/${cardsOf(user)[0].id}`)"
              >✏️</button>
            </div>
          </div>
        </template>
      </div>

      <BaseModal
        v-model="showAdd"
        title="Добавить пользователя"
        :submit-label="adding ? 'Добавление…' : 'Добавить'"
        @submit="handleAdd"
      >
        <div class="field">
          <label class="field-label" for="member-role">Роль</label>
          <select id="member-role" v-model="addForm.role" class="field-control">
            <option value="PARENT">Родитель</option>
            <option value="ATHLETE">Спортсмен</option>
          </select>
        </div>
        <BaseInput id="member-email" v-model="addForm.email" type="email" label="Email аккаунта" placeholder="name@example.ru" />
        <p class="form-hint">
          Пользователь должен сам зарегистрироваться и подтвердить email. После добавления привяжите аккаунт
          в карточке спортсмена: родителя — в связях ребёнка, спортсмена — как его аккаунт.
        </p>
        <p v-if="addError" class="form-error">{{ addError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { organizationsApi } from '../../api/organizations'
import { athletesApi } from '../../api/athletes'
import { getOrganizationId, hasPermission, hasRole, currentUser } from '../../utils/session'
import { errorText, initials, fullName } from '../../utils/format'
import { label, options } from '../../utils/labels'

const ROLE_CLASS = { TRAINER: 'role-blue', PARENT: 'role-orange', ATHLETE: 'role-green', AGENCY: 'role-gray' }
const STATUS_COLOR = { ACTIVE: '#2E8B57', BLOCKED: '#D64545' }
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const roleOptions = options('role')
const statusOptions = options('memberStatus')
// Добавлять по email может тренер с members.write (родитель или спортсмен)
const canAdd = computed(() => hasRole('TRAINER') && hasPermission('members.write'))
const myId = computed(() => currentUser.value?.userId)

// ─────────── Участники: роль, статус и поиск (q) — параметры сервера ───────────
const searchQuery = ref('')
const filters = reactive({ role: '', status: '' })
const members = ref([])
const loading = ref(true)
const loadError = ref('')

let loadSeq = 0
async function loadMembers() {
  const seq = ++loadSeq
  loading.value = true
  loadError.value = ''
  try {
    const items = await fetchAll(params => organizationsApi.members(getOrganizationId(), params), {
      q: searchQuery.value.trim(),
      role: filters.role || null,
      status: filters.status || null
    })
    if (seq === loadSeq) members.value = items
  } catch (e) {
    if (seq === loadSeq) {
      members.value = []
      loadError.value = errorText(e)
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

const emptyMessage = computed(() => {
  const q = searchQuery.value.trim()
  if (q) return `Никого не найдено по запросу «${q}»`
  if (filters.role || filters.status) return 'Пользователей с такими условиями нет'
  return 'Пользователей пока нет'
})

// ─────────── Связи: email и дети родителей (GET /parents), карточки спортсменов (userId) ───────────
const parentsById = ref(new Map())
const athleteCardsByUser = ref(new Map())

async function loadLinks() {
  const [parents, athletes] = await Promise.allSettled([
    fetchAll(params => organizationsApi.parents(getOrganizationId(), params)),
    fetchAll(params => athletesApi.list(getOrganizationId(), params))
  ])
  parentsById.value = parents.status === 'fulfilled'
    ? new Map(parents.value.map(p => [p.userId, p]))
    : new Map()
  const byUser = new Map()
  if (athletes.status === 'fulfilled') {
    for (const athlete of athletes.value) {
      if (!athlete.userId) continue
      const list = byUser.get(athlete.userId) || []
      list.push(athlete)
      byUser.set(athlete.userId, list)
    }
  }
  athleteCardsByUser.value = byUser
}

// Контакты участника API отдаёт только для родителей (GET /parents) и для себя (/me)
function emailOf(user) {
  if (user.userId === myId.value) return currentUser.value?.email || '—'
  return parentsById.value.get(user.userId)?.email || '—'
}

// Карточки строки: своя карточка спортсмена (по userId) и дети родителя
const cardsByUser = computed(() => {
  const result = new Map()
  for (const user of members.value) {
    const cards = []
    const own = athleteCardsByUser.value.get(user.userId) || []
    for (const athlete of own) {
      cards.push({ id: athlete.id, label: own.length > 1 ? `Своя карточка: ${fullName(athlete)}` : 'Своя карточка' })
    }
    for (const child of parentsById.value.get(user.userId)?.athletes || []) {
      if (!cards.some(c => c.id === child.id)) cards.push({ id: child.id, label: child.fullName })
    }
    result.set(user.userId, cards)
  }
  return result
})
const cardsOf = (user) => cardsByUser.value.get(user.userId) || []

// ─────────── Добавление родителя или спортсмена по email ───────────
const showAdd = ref(false)
const adding = ref(false)
const addError = ref('')
const addForm = reactive({ role: 'PARENT', email: '' })
const notice = ref('')

function openAdd() {
  Object.assign(addForm, { role: 'PARENT', email: '' })
  addError.value = ''
  showAdd.value = true
}

async function handleAdd() {
  if (adding.value) return
  const email = addForm.email.trim()
  if (!EMAIL_RE.test(email)) {
    addError.value = 'Укажите корректный email.'
    return
  }
  adding.value = true
  addError.value = ''
  try {
    const member = addForm.role === 'ATHLETE'
      ? await organizationsApi.addAthlete(getOrganizationId(), email)
      : await organizationsApi.addParent(getOrganizationId(), email)
    showAdd.value = false
    const who = member?.fullName || email
    notice.value = addForm.role === 'ATHLETE'
      ? `${who} добавлен(а) как спортсмен. Укажите этот аккаунт в карточке спортсмена.`
      : `${who} добавлен(а) как родитель. Привяжите аккаунт к ребёнку в карточке спортсмена.`
    await Promise.all([loadMembers(), loadLinks()])
  } catch (e) {
    addError.value = e?.fieldErrors?.some(f => f.field === 'email')
      ? 'Укажите корректный email.'
      : errorText(e)
  } finally {
    adding.value = false
  }
}

// ─────────── Жизненный цикл ───────────
let searchTimer = null
watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadMembers, 300)
})
watch(() => [filters.role, filters.status], loadMembers)
onBeforeUnmount(() => clearTimeout(searchTimer))

onMounted(() => {
  loadMembers()
  loadLinks()
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
</script>

<style scoped>
/* стили как раньше */
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

.filter-bar { display: flex; gap: 12px; flex-wrap: wrap; }
.filter-select {
  padding: 8px 12px; border: 1px solid #E3EAE8;
  border-radius: 10px; background: white;
  font-size: 13px; color: #152421;
  cursor: pointer; outline: none;
}

.notice {
  padding: 12px 16px; background: #E9F7D5;
  border-radius: 12px; font-size: 13px; color: #2E8B57;
  display: flex; gap: 12px; flex-wrap: wrap; align-items: center;
}
.notice a { color: #102522; font-weight: 700; }

.table-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 12px;
  overflow-x: auto;
}
.table-header, .table-row {
  display: flex; align-items: center; gap: 16px;
  min-width: 900px;
}
.table-header { padding-bottom: 12px; border-bottom: 1px solid #E3EAE8; }
.table-row { padding: 12px 0; border-bottom: 1px solid #E3EAE8; }

.col { font-size: 14px; color: #152421; }
.col.user { flex: 1; display: flex; align-items: center; gap: 12px; font-weight: 600; }
.col.email { width: 220px; color: #6D7D79; font-size: 13px; overflow-wrap: anywhere; }
.col.role { width: 140px; display: flex; gap: 4px; flex-wrap: wrap; }
.col.status { width: 140px; display: flex; align-items: center; gap: 6px; font-size: 13px; }
.col.last { width: 160px; color: #6D7D79; font-size: 13px; display: flex; flex-direction: column; gap: 2px; }
.col.actions { width: 100px; display: flex; justify-content: flex-end; gap: 6px; }

.table-header .col {
  font-size: 12px; font-weight: 700; color: #6D7D79;
  text-transform: uppercase;
}

.avatar {
  width: 32px; height: 32px;
  background: #F4F7F8; border-radius: 999px;
  display: flex; justify-content: center; align-items: center;
  font-size: 11px; font-weight: 700;
  flex-shrink: 0;
}
.me-mark { font-weight: 500; color: #98A6A2; }

.role-badge {
  display: inline-block; padding: 4px 8px;
  border-radius: 6px; font-size: 11px; font-weight: 700;
}
.role-blue { background: #E3F2FD; color: #2196F3; }
.role-orange { background: #FFF3E0; color: #FF9800; }
.role-green { background: #E8F5E9; color: #2E8B57; }
.role-gray { background: #EEF1F0; color: #888888; }

.dot { width: 6px; height: 6px; border-radius: 50%; }

.card-link { color: #35678E; text-decoration: none; font-size: 13px; }
.card-link:hover { text-decoration: underline; }

.icon-btn {
  padding: 6px; background: #F4F7F8;
  border: none; border-radius: 8px;
  cursor: pointer; font-size: 12px;
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
.form-hint { font-size: 13px; color: #6D7D79; line-height: 1.4; }
.form-error { font-size: 13px; color: #D64545; }
</style>
