<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        title="Профиль родителя"
        subtitle="Контакты, семейные связи и уведомления"
        :show-search="false"
      >
        <template #actions>
            <router-link to="/parent/announcements" class="icon-btn" title="Объявления" aria-label="Объявления">
                <BaseIcon name="bell" :size="18" color="#152421" />
                <span v-if="hasUnread" class="icon-badge"></span>
            </router-link>
            <BaseButton disabled title="Изменение ФИО и контактов появится позже">
                <BaseIcon name="edit" :size="16" color="#102522" />
                Редактировать
            </BaseButton>
        </template>
      </PageHeader>

      <!-- Profile Summary Card -->
      <section class="profile-summary-card card">
        <div class="avatar-large">{{ initials(displayName) }}</div>
        <div class="profile-info">
          <div class="profile-badges">
            <span v-for="role in roles" :key="role" class="badge badge--dark">{{ label('role', role) }}</span>
            <span class="badge badge--green">Доступ подтверждён</span>
          </div>
          <h2 class="profile-name">{{ displayName }}</h2>
          <div class="profile-contacts">
            <div class="contact-item">
              <BaseIcon name="mail" :size="15" color="#6D7D79" />
              <span>{{ me?.email || '—' }}</span>
            </div>
            <div class="contact-item">
              <BaseIcon name="briefcase" :size="15" color="#6D7D79" />
              <span>{{ organizationName }}</span>
            </div>
          </div>
        </div>
        <div class="profile-facts">
          <div class="fact-row">
            <span class="fact-label">Статус профиля</span>
            <span class="fact-value fact-value--green">Подтверждён</span>
          </div>
          <div class="fact-row">
            <span class="fact-label">Сеанс активен до</span>
            <span class="fact-value">{{ sessionUntil }}</span>
          </div>
        </div>
      </section>

      <!-- Main Content Grid -->
      <div class="content-grid">
        <!-- Left Column: Details -->
        <div class="main-column">
          <!-- Contact Info Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Контактная информация</h3>
              <p class="card-subtitle">Основные данные представителя</p>
            </div>
            <div class="info-grid">
              <div class="info-column">
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="user" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">ФИО</span>
                    <span class="field-value">{{ displayName }}</span>
                  </div>
                </div>
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="mail" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Email</span>
                    <span class="field-value">{{ me?.email || '—' }}</span>
                  </div>
                </div>
              </div>
              <div class="info-column">
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="users" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Родство</span>
                    <span class="field-value">{{ relationshipText }}</span>
                  </div>
                </div>
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="map-pin" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Адрес секции</span>
                    <span class="field-value">{{ organizationAddress }}</span>
                  </div>
                </div>
              </div>
            </div>
            <p class="field-note">Телефон, предпочтительный канал связи, город и изменение ФИО — раздел появится позже.</p>
          </div>

          <!-- Linked Children Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Связанные дети</h3>
              <p class="card-subtitle">{{ childrenSubtitle }}</p>
            </div>
            <StateBlock v-if="childrenState" :kind="childrenState.kind" :message="childrenState.message" />
            <div v-else class="children-list">
              <div v-for="(child, i) in children" :key="child.id" class="linked-child-item">
                <div class="child-avatar" :class="i % 2 ? 'green' : 'blue'">{{ child.initials }}</div>
                <div class="child-details">
                  <span class="child-name">{{ child.name }}</span>
                  <span class="child-age">{{ child.age }}</span>
                </div>
                <div class="child-program">
                  <span class="program-name">{{ child.program }}</span>
                  <span class="program-group">{{ child.group }}</span>
                </div>
                <BaseButton variant="outline" class="open-btn" @click="openChild(child.id)">Открыть</BaseButton>
              </div>
            </div>
          </div>

          <!-- Emergency Contact Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Экстренный контакт</h3>
            </div>
            <div class="emergency-contact">
              <div class="emergency-icon"><BaseIcon name="siren" :size="19" color="#D64545" /></div>
              <div class="emergency-details">
                <span class="emergency-name">Раздел появится позже</span>
                <span class="emergency-info">Указать экстренный контакт в кабинете пока нельзя — сообщите его тренеру.</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Right Column: Settings -->
        <div class="settings-column">
          <!-- Notifications Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Настройки уведомлений</h3>
              <p class="card-subtitle">Выберите, какие события не пропустить</p>
            </div>
            <p class="soon-text">Раздел появится позже — выбрать, какие уведомления получать, пока нельзя. Уведомления приходят в колокольчик в шапке страницы.</p>
          </div>

          <!-- Family Access Status Card -->
          <div class="status-card">
            <div class="status-icon">
              <BaseIcon name="users-round" :size="19" color="#B7F34B" />
            </div>
            <div class="status-copy">
              <span class="status-label">Семейный доступ</span>
              <span class="status-value">{{ familyValue }}</span>
              <span class="status-description">{{ familyDescription }}</span>
            </div>
            <router-link to="/parent/dashboard" class="status-action">Обзор →</router-link>
          </div>

          <!-- Security Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Безопасность и доступ</h3>
            </div>
            <div class="security-actions">
              <div class="security-action-item" role="button" tabindex="0" @click="openPassword" @keydown.enter="openPassword">
                <BaseIcon name="key" :size="17" color="#152421" />
                <div class="action-copy">
                  <span class="action-title">Изменить пароль</span>
                  <span class="action-description">После смены нужно войти заново</span>
                </div>
                <BaseIcon name="chevron-right" :size="15" color="#98A6A2" />
              </div>
              <div class="security-action-item security-action-item--disabled" aria-disabled="true">
                <BaseIcon name="shield-check" :size="17" color="#98A6A2" />
                <div class="action-copy">
                  <span class="action-title">Двухфакторная защита</span>
                  <span class="action-description">Раздел появится позже</span>
                </div>
              </div>
              <div class="security-action-item security-action-item--danger" role="button" tabindex="0" @click="handleLogout" @keydown.enter="handleLogout">
                <BaseIcon name="log-out" :size="17" color="#D64545" />
                <div class="action-copy">
                  <span class="action-title">Выйти из аккаунта</span>
                  <span class="action-description">Завершить текущую сессию</span>
                </div>
                <BaseIcon name="chevron-right" :size="15" color="#98A6A2" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Смена пароля (PUT /auth/password) -->
      <BaseModal
        v-model="showPasswordModal"
        title="Изменить пароль"
        :submit-label="changingPassword ? 'Сохранение…' : 'Изменить пароль'"
        @submit="handleChangePassword"
      >
        <BaseInput id="pwd-current" v-model="passwordForm.current" type="password" label="Текущий пароль" />
        <BaseInput id="pwd-new" v-model="passwordForm.next" type="password" label="Новый пароль" placeholder="От 15 до 128 символов" />
        <BaseInput id="pwd-repeat" v-model="passwordForm.repeat" type="password" label="Повторите новый пароль" />
        <p class="form-hint">После смены пароля все сеансы завершатся — войдите снова с новым паролем.</p>
        <p v-if="passwordError" class="form-error" role="alert">{{ passwordError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  logout, clearSession, currentUser, currentOrganization, myAthletes, loadMyAthletes, selectAthlete, getOrganizationId
} from '../../utils/session'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { authApi } from '../../api/auth'
import { organizationsApi } from '../../api/organizations'
import { groupsApi } from '../../api/groups'
import { sectionsApi } from '../../api/sections'
import { announcementsApi } from '../../api/announcements'
import {
  errorText, formatTime, formatDateTime, toIsoDate, parseDate, fullName, initials, ageYears
} from '../../utils/format'
import { label } from '../../utils/labels'

function plural(n, one, few, many) {
  const m10 = n % 10
  const m100 = n % 100
  if (m10 === 1 && m100 !== 11) return one
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return few
  return many
}

const unique = list => [...new Set(list.filter(Boolean))]

// Все страницы списка Page<T> (size не больше 100)
async function fetchAll(request, maxPages = 10) {
  const items = []
  for (let page = 0; page < maxPages; page++) {
    const res = await request({ page, size: 100 })
    items.push(...(res?.items || []))
    if (page + 1 >= (res?.totalPages || 0)) break
  }
  return items
}

// ─────────── Аккаунт (GET /me) ───────────
const me = ref(currentUser.value)

async function loadMe() {
  try {
    me.value = await authApi.me()
  } catch (_) {
    // остаются данные сессии; 401 обработает общий обработчик входа
  }
}

const displayName = computed(() => me.value?.fullName?.trim() || me.value?.email || '—')
const roles = computed(() => currentOrganization.value?.roles || [])

const sessionUntil = computed(() => {
  const until = parseDate(me.value?.expiresAt)
  if (!until || isNaN(until)) return '—'
  return toIsoDate(until) === toIsoDate(new Date()) ? `Сегодня, ${formatTime(until)}` : formatDateTime(until)
})

// ─────────── Организация (GET /organizations/{id}) ───────────
const organization = ref(null)
const orgLoading = ref(true)

async function loadOrganization() {
  orgLoading.value = true
  try {
    organization.value = await organizationsApi.get(getOrganizationId())
  } catch (_) {
    organization.value = null
  } finally {
    orgLoading.value = false
  }
}

const organizationName = computed(() => organization.value?.name || currentOrganization.value?.organizationName || '—')
const organizationAddress = computed(() => {
  if (orgLoading.value) return '…'
  if (!organization.value) return '—'
  return organization.value.address || 'Не указан'
})

// ─────────── Дети: свои карточки (parentLinks) и их текущие группы ───────────
const childrenReady = ref(false)
const childrenError = ref(null)
const groupsByChild = ref(new Map()) // athleteId → Group[]
const groupsLoaded = ref(false)
const sections = ref([])

// Родство из связи родителя с карточкой ребёнка (задаёт тренер)
function relationshipOf(athlete) {
  const userId = me.value?.userId || currentUser.value?.userId
  return (athlete.parents || []).find(p => p.parentUserId === userId)?.relationship?.trim() || ''
}

async function loadChildGroups() {
  const org = getOrganizationId()
  const [groupLists, sectionList] = await Promise.all([
    Promise.allSettled(myAthletes.value.map(a =>
      fetchAll(p => groupsApi.list(org, { athleteId: a.id, status: 'ACTIVE', ...p })))),
    fetchAll(p => sectionsApi.list(org, p)).catch(() => [])
  ])
  const map = new Map()
  myAthletes.value.forEach((a, i) => {
    if (groupLists[i]?.status === 'fulfilled') map.set(a.id, groupLists[i].value)
  })
  groupsByChild.value = map
  sections.value = sectionList
  groupsLoaded.value = true
}

const sectionById = computed(() => new Map(sections.value.map(s => [s.id, s])))

const children = computed(() => myAthletes.value.map(a => {
  const age = ageYears(a.birthDate)
  const groups = groupsByChild.value.get(a.id)
  const program = groups ? unique(groups.map(g => sectionById.value.get(g.sectionId)?.name)).join(', ') : ''
  const groupNames = groups ? groups.map(g => g.name).join(', ') : ''
  return {
    id: a.id,
    name: fullName(a),
    firstName: a.firstName || fullName(a),
    sectionsText: program,
    initials: initials(fullName(a)),
    age: [age === null || isNaN(age) ? '' : `${age} ${plural(age, 'год', 'года', 'лет')}`, relationshipOf(a)]
      .filter(Boolean).join(' · ') || '—',
    program: !groupsLoaded.value ? '…' : groups ? program || 'Секция не указана' : 'Группы не загрузились',
    group: !groupsLoaded.value ? '' : groups ? groupNames || 'Нет текущих групп' : ''
  }
}))

const childrenState = computed(() => {
  if (!childrenReady.value) return { kind: 'loading', message: '' }
  if (childrenError.value) return { kind: 'error', message: errorText(childrenError.value) }
  if (!myAthletes.value.length) return { kind: 'empty', message: 'Ребёнок ещё не привязан — обратитесь к тренеру' }
  return null
})

const childrenSubtitle = computed(() => {
  const n = myAthletes.value.length
  if (!childrenReady.value) return 'Загрузка…'
  return n
    ? `${n} ${plural(n, 'ребёнок привязан', 'ребёнка привязаны', 'детей привязаны')} к вашему аккаунту`
    : 'Связь с ребёнком добавляет тренер'
})

const relationshipText = computed(() => unique(myAthletes.value.map(relationshipOf)).join(', ') || 'Не указано')

const familyValue = computed(() => {
  const n = myAthletes.value.length
  return n ? `${n} ${plural(n, 'профиль ребёнка', 'профиля детей', 'профилей детей')}` : 'Нет привязанных детей'
})

// «Имя · секция, Имя · секция» — по каждому ребёнку
const familyDescription = computed(() => {
  if (!myAthletes.value.length) return 'Связь с ребёнком добавляет тренер'
  return children.value.map(c => [c.firstName, c.sectionsText].filter(Boolean).join(' · ')).join(', ')
})

const logoutRouter = useRouter()

function openChild(id) {
  selectAthlete(id)
  logoutRouter.push('/parent/dashboard')
}

// ─────────── Отметка у колокольчика: непрочитанные объявления ───────────
const hasUnread = ref(false)

async function loadUnread() {
  try {
    const res = await announcementsApi.list(getOrganizationId(), { status: 'PUBLISHED', unread: true, size: 1 })
    hasUnread.value = (res?.totalElements || 0) > 0
  } catch (_) {
    hasUnread.value = false // отметка в шапке не критична
  }
}

// ─────────── Смена пароля (PUT /auth/password) ───────────
const showPasswordModal = ref(false)
const changingPassword = ref(false)
const passwordError = ref('')
const passwordForm = reactive({ current: '', next: '', repeat: '' })

function openPassword() {
  Object.assign(passwordForm, { current: '', next: '', repeat: '' })
  passwordError.value = ''
  showPasswordModal.value = true
}

async function handleChangePassword() {
  if (changingPassword.value) return
  if (!passwordForm.current) { passwordError.value = 'Введите текущий пароль.'; return }
  if (passwordForm.next.length < 15 || passwordForm.next.length > 128) {
    passwordError.value = 'Новый пароль должен содержать от 15 до 128 символов.'
    return
  }
  if (passwordForm.next !== passwordForm.repeat) { passwordError.value = 'Пароли не совпадают.'; return }
  if (passwordForm.next === passwordForm.current) { passwordError.value = 'Новый пароль совпадает с текущим.'; return }
  changingPassword.value = true
  passwordError.value = ''
  try {
    await authApi.changePassword(passwordForm.current, passwordForm.next)
    // Сервер завершил все сеансы, включая этот, — на вход с сообщением «Пароль изменён»
    showPasswordModal.value = false
    clearSession()
    logoutRouter.push({ path: '/', query: { reset: '1' } })
  } catch (e) {
    if (e?.code === 'INVALID_CREDENTIALS') passwordError.value = 'Текущий пароль указан неверно.'
    else if (e?.fieldErrors?.some(f => f.field === 'newPassword')) passwordError.value = 'Новый пароль должен содержать от 15 до 128 символов.'
    else passwordError.value = errorText(e)
  } finally {
    changingPassword.value = false
  }
}

onMounted(async () => {
  loadMe()
  loadOrganization()
  loadUnread()
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (e) {
    childrenError.value = e
  } finally {
    childrenReady.value = true
  }
  if (!childrenError.value && myAthletes.value.length) {
    loadChildGroups().catch(() => { groupsLoaded.value = true })
  }
})

async function handleLogout() {
  await logout()
  logoutRouter.push('/')
}
</script>

<style scoped>
/* --- Стили аналогичны профилю тренера, с небольшими отличиями --- */
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.card { background: white; border: 1px solid #E3EAE8; border-radius: 20px; padding: 24px; box-shadow: 0 8px 24px rgba(23, 52, 46, 0.04); display: flex; flex-direction: column; gap: 18px; }
.profile-summary-card { display: flex; align-items: center; gap: 20px; }
.avatar-large { width: 104px; height: 104px; background: #E9F7D5; border-radius: 999px; display: flex; justify-content: center; align-items: center; font-size: 30px; font-weight: 800; color: #2E8B57; flex-shrink: 0; }
.profile-info { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.profile-badges { display: flex; gap: 8px; }
.badge { padding: 5px 10px; border-radius: 999px; font-size: 10px; font-weight: 700; text-transform: uppercase; }
.badge--dark { background: #102522; color: #B7F34B; }
.badge--green { background: #E9F7D5; color: #2E8B57; }
.profile-name { font-size: 24px; font-weight: 700; color: #152421; }
.profile-contacts { display: flex; gap: 20px; align-items: center; flex-wrap: wrap; }
.contact-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #6D7D79; }
.profile-facts { width: 230px; display: flex; flex-direction: column; gap: 10px; }
.fact-row { display: flex; justify-content: space-between; align-items: center; }
.fact-label { font-size: 11px; color: #6D7D79; }
.fact-value { font-size: 11px; font-weight: 600; color: #152421; }
.fact-value--green { color: #2E8B57; font-weight: 700; }
.content-grid { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }
.main-column, .settings-column { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; flex-direction: column; gap: 4px; }
.card-title { font-size: 18px; font-weight: 700; color: #152421; }
.card-subtitle { font-size: 12px; color: #6D7D79; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.info-column { display: flex; flex-direction: column; }
.info-field { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #E3EAE8; }
.info-field:last-child { border-bottom: none; }
.field-icon { width: 34px; height: 34px; background: #F0F4F4; border-radius: 8px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; }
.field-copy { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.field-label { font-size: 10px; font-weight: 700; color: #98A6A2; text-transform: uppercase; }
.field-value { font-size: 14px; font-weight: 600; color: #152421; overflow-wrap: anywhere; }
.field-note { font-size: 12px; color: #98A6A2; }
.children-list { display: flex; flex-direction: column; gap: 10px; }
.linked-child-item { display: flex; align-items: center; gap: 12px; padding: 14px; background: #F0F4F4; border-radius: 12px; }
.child-avatar { width: 44px; height: 44px; border-radius: 999px; display: flex; justify-content: center; align-items: center; font-size: 13px; font-weight: 700; flex-shrink: 0; }
.child-avatar.blue { background: #DDECFB; color: #35678E; }
.child-avatar.green { background: #E9F7D5; color: #2E8B57; }
.child-details { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.child-name { font-size: 14px; font-weight: 700; color: #152421; }
.child-age { font-size: 11px; color: #6D7D79; }
.child-program { width: 180px; display: flex; flex-direction: column; gap: 3px; }
.program-name { font-size: 12px; font-weight: 600; color: #152421; }
.program-group { font-size: 11px; color: #6D7D79; }
.open-btn { height: 44px; padding: 0 16px; font-size: 13px; font-weight: 700; }
.emergency-contact { display: flex; align-items: center; gap: 14px; }
.emergency-icon { width: 42px; height: 42px; background: #FCE2E5; border-radius: 12px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; }
.emergency-details { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.emergency-name { font-size: 14px; font-weight: 700; color: #152421; }
.emergency-info { font-size: 11px; color: #6D7D79; }
.soon-text { font-size: 13px; color: #6D7D79; line-height: 1.5; }
.status-card { background: #102522; padding: 18px; border-radius: 16px; display: flex; align-items: center; gap: 14px; }
.status-icon { width: 42px; height: 42px; background: #19332F; border-radius: 12px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; }
.status-copy { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.status-label { font-size: 10px; font-weight: 700; color: #98A6A2; text-transform: uppercase; }
.status-value { font-size: 16px; font-weight: 700; color: white; }
.status-description { font-size: 10px; color: #98A6A2; }
.status-action { font-size: 11px; font-weight: 700; color: #B7F34B; text-decoration: none; white-space: nowrap; }
.security-actions { display: flex; flex-direction: column; gap: 8px; }
.security-action-item { display: flex; align-items: center; gap: 10px; padding: 12px; background: #F0F4F4; border-radius: 12px; cursor: pointer; transition: background 0.2s; }
.security-action-item:hover { background: #E9F0EE; }
.security-action-item--disabled { cursor: default; opacity: 0.7; }
.security-action-item--disabled:hover { background: #F0F4F4; }
.action-copy { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.action-title { font-size: 13px; font-weight: 600; color: #152421; }
.action-description { font-size: 10px; color: #6D7D79; }
.security-action-item--danger .action-title { color: #D64545; }
.form-hint { font-size: 13px; color: #6D7D79; line-height: 1.4; }
.form-error { font-size: 13px; color: #D64545; }
.icon-btn {
  position: relative;
  width: 44px;
  height: 44px;
  background: white;
  border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  flex-shrink: 0;
}

.icon-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  background: #D64545;
  border-radius: 50%;
  border: 2px solid white;
  box-sizing: content-box;
  pointer-events: none;
}
</style>
