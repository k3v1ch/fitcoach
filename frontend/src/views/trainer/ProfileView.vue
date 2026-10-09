<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Профиль тренера"
        subtitle="Личные данные, рабочая нагрузка и настройки кабинета"
        :show-search="false"
      >
        <template #actions>
            <BaseButton v-if="canEditOrganization" :disabled="!organization" @click="openOrganization">
                <BaseIcon name="edit" :size="16" color="#102522" />
                Редактировать организацию
            </BaseButton>
        </template>
      </PageHeader>

      <!-- Profile Summary Card -->
      <section class="profile-summary-card card">
        <div class="avatar-large">{{ initials(displayName) }}</div>
        <div class="profile-info">
          <div class="profile-badges">
            <span v-for="role in roles" :key="role" class="badge badge--dark">{{ label('role', role) }}</span>
            <span class="badge badge--green">Активен</span>
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
          <!-- Personal & Professional Info Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Личная и профессиональная информация</h3>
              <p class="card-subtitle">Данные аккаунта и организации</p>
            </div>
            <div class="info-grid">
              <!-- Column 1 -->
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
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="award" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Роль в организации</span>
                    <span class="field-value">{{ rolesText }}</span>
                  </div>
                </div>
              </div>
              <!-- Column 2 -->
              <div class="info-column">
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="briefcase" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Организация</span>
                    <span class="field-value">{{ organizationName }}</span>
                  </div>
                </div>
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="map-pin" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Адрес</span>
                    <span class="field-value">{{ orgField(organization?.address, 'Не указан') }}</span>
                  </div>
                </div>
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="clock" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Часовой пояс</span>
                    <span class="field-value">{{ orgField(organization ? timezoneLabel(organization.timezone) : null, '—') }}</span>
                  </div>
                </div>
              </div>
            </div>
            <p v-if="organization?.description" class="org-description">{{ organization.description }}</p>
            <p v-if="orgLoadError" class="form-error">Не удалось загрузить сведения организации: {{ orgLoadError }}</p>
            <p class="field-note">Телефон, специализация, квалификация, дата рождения, стаж и город — раздел появится позже.</p>
          </div>

          <!-- Assigned Sections & Groups Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Закреплённые секции и группы</h3>
              <p class="card-subtitle">{{ groupsSubtitle }}</p>
            </div>
            <StateBlock v-if="groupsLoading" kind="loading" />
            <StateBlock v-else-if="groupsError" kind="error" :message="groupsError" />
            <StateBlock v-else-if="!assignedGroups.length" kind="empty" message="За вами пока не закреплено ни одной группы" />
            <div v-else class="assignments-list">
              <div
                v-for="group in assignedGroups"
                :key="group.id"
                class="assignment-item"
                role="link"
                tabindex="0"
                @click="openGroup(group.id)"
                @keydown.enter="openGroup(group.id)"
              >
                <div class="assignment-icon">
                  <BaseIcon name="users" :size="18" color="#B7F34B" />
                </div>
                <div class="assignment-copy">
                  <h4 class="assignment-title">{{ group.title }}</h4>
                  <p class="assignment-meta">{{ group.meta }}</p>
                </div>
                <div class="assignment-tags">
                  <span v-for="tag in group.tags" :key="tag" class="tag tag--green">{{ tag }}</span>
                </div>
                <BaseIcon name="chevron-right" :size="16" color="#98A6A2" />
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

          <!-- Workload Status Card -->
          <div class="status-card">
            <div class="status-icon">
              <BaseIcon name="calendar-check" :size="19" color="#B7F34B" />
            </div>
            <div class="status-copy">
              <span class="status-label">Рабочая нагрузка</span>
              <span class="status-value">{{ workloadValue }}</span>
              <span class="status-description">{{ workloadDescription }}</span>
            </div>
            <router-link to="/trainer/schedule" class="status-action">Расписание →</router-link>
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

      <!-- Сведения организации (тренер с organization.write) -->
      <BaseModal
        v-model="showOrgModal"
        title="Сведения организации"
        :submit-label="savingOrg ? 'Сохранение…' : 'Сохранить'"
        @submit="handleSaveOrganization"
      >
        <BaseInput id="org-name" v-model="orgForm.name" label="Название" placeholder="Спортивный клуб «Волна»" />
        <div class="form-field">
          <label class="form-label" for="org-description">Описание</label>
          <textarea id="org-description" v-model="orgForm.description" class="form-control" rows="3" placeholder="Необязательно"></textarea>
        </div>
        <BaseInput id="org-address" v-model="orgForm.address" label="Адрес" placeholder="Необязательно" />
        <div class="form-field">
          <label class="form-label" for="org-timezone">Часовой пояс</label>
          <select id="org-timezone" v-model="orgForm.timezone" class="form-control">
            <option v-for="tz in timezoneOptions" :key="tz.value" :value="tz.value">{{ tz.label }}</option>
          </select>
        </div>
        <p class="form-hint">Часовой пояс задаёт границы дней в расписании и отчётах; время уже созданных тренировок не сдвигается.</p>
        <p v-if="orgError" class="form-error">{{ orgError }}</p>
      </BaseModal>

      <!-- Смена пароля -->
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
        <p v-if="passwordError" class="form-error">{{ passwordError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { logout, clearSession, currentUser, currentOrganization, getOrganizationId, hasPermission, hasRole } from '../../utils/session'
import Sidebar from '../../components/layout/Sidebar.vue'
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
import { trainingsApi } from '../../api/trainings'
import { dictionariesApi } from '../../api/dictionaries'
import {
  errorText, formatDate, formatTime, formatDateTime, toIsoDate, toIsoDateTime, periodRange, addDays, parseDate, initials
} from '../../utils/format'
import { label } from '../../utils/labels'

const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']
const ORG_FIELDS = { name: 'Название', description: 'Описание', address: 'Адрес', timezone: 'Часовой пояс' }
// Часовые пояса России (IANA); текущий пояс организации добавляется, если его нет в списке
const TIMEZONES = [
  ['Europe/Kaliningrad', 'Калининград (UTC+2)'],
  ['Europe/Moscow', 'Москва (UTC+3)'],
  ['Europe/Samara', 'Самара (UTC+4)'],
  ['Asia/Yekaterinburg', 'Екатеринбург (UTC+5)'],
  ['Asia/Omsk', 'Омск (UTC+6)'],
  ['Asia/Novosibirsk', 'Новосибирск (UTC+7)'],
  ['Asia/Krasnoyarsk', 'Красноярск (UTC+7)'],
  ['Asia/Irkutsk', 'Иркутск (UTC+8)'],
  ['Asia/Yakutsk', 'Якутск (UTC+9)'],
  ['Asia/Vladivostok', 'Владивосток (UTC+10)'],
  ['Asia/Magadan', 'Магадан (UTC+11)'],
  ['Asia/Kamchatka', 'Камчатка (UTC+12)']
]

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
const rolesText = computed(() => roles.value.map(r => label('role', r)).join(', ') || '—')

const sessionUntil = computed(() => {
  const until = parseDate(me.value?.expiresAt)
  if (!until || isNaN(until)) return '—'
  return toIsoDate(until) === toIsoDate(new Date()) ? `Сегодня, ${formatTime(until)}` : formatDateTime(until)
})

// ─────────── Организация (GET/PATCH /organizations/{id}) ───────────
const organization = ref(null)
const orgLoading = ref(true)
const orgLoadError = ref('')
const canEditOrganization = computed(() => hasRole('TRAINER') && hasPermission('organization.write'))

async function loadOrganization() {
  orgLoading.value = true
  orgLoadError.value = ''
  try {
    organization.value = await organizationsApi.get(getOrganizationId())
  } catch (e) {
    organization.value = null
    orgLoadError.value = errorText(e)
  } finally {
    orgLoading.value = false
  }
}

const organizationName = computed(() => organization.value?.name || currentOrganization.value?.organizationName || '—')

function orgField(value, fallback) {
  if (orgLoading.value) return '…'
  if (!organization.value) return '—'
  return value || fallback
}

function timezoneLabel(zone) {
  return TIMEZONES.find(([value]) => value === zone)?.[1] || zone || '—'
}

const timezoneOptions = computed(() => {
  const list = TIMEZONES.map(([value, text]) => ({ value, label: text }))
  const current = orgForm.timezone
  return current && !list.some(o => o.value === current) ? [{ value: current, label: current }, ...list] : list
})

const showOrgModal = ref(false)
const savingOrg = ref(false)
const orgError = ref('')
const orgForm = reactive({ name: '', description: '', address: '', timezone: 'Europe/Moscow' })

function openOrganization() {
  const org = organization.value
  if (!org) return
  Object.assign(orgForm, {
    name: org.name || '',
    description: org.description || '',
    address: org.address || '',
    timezone: org.timezone || 'Europe/Moscow'
  })
  orgError.value = ''
  showOrgModal.value = true
}

async function handleSaveOrganization() {
  if (savingOrg.value) return
  const name = orgForm.name.trim()
  if (!name) { orgError.value = 'Укажите название организации.'; return }
  if (name.length > 200) { orgError.value = 'Название — не длиннее 200 символов.'; return }
  if (!orgForm.timezone) { orgError.value = 'Выберите часовой пояс.'; return }
  savingOrg.value = true
  orgError.value = ''
  try {
    const saved = await organizationsApi.update(getOrganizationId(), {
      name,
      description: orgForm.description.trim() || null,
      address: orgForm.address.trim() || null,
      timezone: orgForm.timezone
    })
    organization.value = saved
    // Название в боковом меню и переключателе организаций
    if (currentOrganization.value) currentOrganization.value.organizationName = saved.name
    showOrgModal.value = false
  } catch (e) {
    const fieldErrors = (e?.fieldErrors || []).map(f => ({ ...f, field: ORG_FIELDS[f.field] || f.field }))
    orgError.value = errorText({ status: e?.status, message: e?.message, fieldErrors })
  } finally {
    savingOrg.value = false
  }
}

// ─────────── Закреплённые группы (GET /groups?coachId=…) ───────────
const groups = ref([])
const groupsLoading = ref(true)
const groupsError = ref('')
const sections = ref([])
const venues = ref([])

async function loadGroups(userId) {
  groupsLoading.value = true
  groupsError.value = ''
  try {
    groups.value = await fetchAll(params => groupsApi.list(getOrganizationId(), params), { coachId: userId, status: 'ACTIVE' })
  } catch (e) {
    groups.value = []
    groupsError.value = errorText(e)
  } finally {
    groupsLoading.value = false
  }
}

async function loadReference() {
  const [sectionList, venueList] = await Promise.allSettled([
    fetchAll(params => sectionsApi.list(getOrganizationId(), params)),
    fetchAll(params => dictionariesApi.list(getOrganizationId(), 'venues', params))
  ])
  sections.value = sectionList.status === 'fulfilled' ? sectionList.value : []
  venues.value = venueList.status === 'fulfilled' ? venueList.value : []
}

const assignedGroups = computed(() => groups.value.map(group => {
  const section = sections.value.find(s => s.id === group.sectionId)
  const own = weekTrainings.value.filter(t => t.groupId === group.id && t.status !== 'CANCELLED')
  const days = [...new Set(own.map(t => (parseDate(t.startsAt).getDay() + 6) % 7))].sort((a, b) => a - b)
  const count = group.athleteCount || 0
  const meta = [
    `${count} ${plural(count, 'спортсмен', 'спортсмена', 'спортсменов')}`,
    weekLoaded.value ? (days.length ? days.map(d => WEEKDAYS[d]).join(', ') : 'нет занятий на этой неделе') : null
  ].filter(Boolean).join(' · ')
  const tags = [...new Set(own.map(t => venues.value.find(v => v.id === t.venueId)?.name).filter(Boolean))].slice(0, 2)
  return { id: group.id, title: section ? `${section.name} · ${group.name}` : group.name, meta, tags }
}))

const groupsSubtitle = computed(() => {
  if (groupsLoading.value) return 'Загрузка…'
  if (groupsError.value || !groups.value.length) return 'Группы, где вы указаны тренером'
  const sectionCount = new Set(groups.value.map(g => g.sectionId)).size
  const athletes = groups.value.reduce((sum, g) => sum + (g.athleteCount || 0), 0)
  return `${sectionCount} ${plural(sectionCount, 'направление', 'направления', 'направлений')} · ${athletes} ${plural(athletes, 'спортсмен', 'спортсмена', 'спортсменов')}`
})

const openGroup = (id) => logoutRouter.push(`/trainer/groups/${id}`)

// ─────────── Рабочая нагрузка: свои занятия на этой неделе и ближайшее ───────────
const weekTrainings = ref([])
const weekLoaded = ref(false)
const weekError = ref(false)
const nearest = ref(null)

async function loadWorkload(userId) {
  const week = periodRange('week')
  const now = new Date()
  const [weekList, upcoming] = await Promise.allSettled([
    fetchAll(params => trainingsApi.list(getOrganizationId(), params), {
      from: toIsoDateTime(week.from),
      to: toIsoDateTime(week.to),
      coachId: userId
    }),
    trainingsApi.list(getOrganizationId(), {
      from: toIsoDateTime(now),
      to: toIsoDateTime(addDays(now, 60)),
      coachId: userId,
      status: 'PLANNED',
      page: 0,
      size: 1
    })
  ])
  if (weekList.status === 'fulfilled') {
    weekTrainings.value = weekList.value
    weekLoaded.value = true
  } else {
    weekError.value = true
  }
  nearest.value = upcoming.status === 'fulfilled' ? (upcoming.value?.items || [])[0] || null : null
}

const workloadValue = computed(() => {
  if (weekError.value) return 'Нет данных'
  if (!weekLoaded.value) return '…'
  const n = weekTrainings.value.filter(t => t.status !== 'CANCELLED').length
  return `${n} ${plural(n, 'занятие', 'занятия', 'занятий')} на этой неделе`
})

const workloadDescription = computed(() => {
  const n = groups.value.length
  const groupsText = groupsLoading.value ? '' : `${n} ${plural(n, 'группа', 'группы', 'групп')}`
  return [groupsText, nearestText(nearest.value)].filter(Boolean).join(' · ')
})

function nearestText(training) {
  if (!training) return weekLoaded.value ? 'ближайших занятий нет' : ''
  const start = parseDate(training.startsAt)
  const now = new Date()
  if (start <= now) return 'занятие идёт сейчас'
  const key = toIsoDate(start)
  if (key === toIsoDate(now)) return `ближайшее сегодня в ${formatTime(start)}`
  if (key === toIsoDate(addDays(now, 1))) return `ближайшее завтра в ${formatTime(start)}`
  return `ближайшее ${formatDate(start, { withYear: false })} в ${formatTime(start)}`
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
  loadOrganization()
  loadReference()
  await loadMe()
  const userId = me.value?.userId || currentUser.value?.userId
  // Без userId фильтр coachId выпал бы из запроса и показались бы чужие группы
  if (!userId) {
    groupsLoading.value = false
    groupsError.value = 'Не удалось определить пользователя — обновите страницу.'
    weekError.value = true
    return
  }
  loadGroups(userId)
  loadWorkload(userId)
})

const logoutRouter = useRouter()

async function handleLogout() {
  await logout()
  logoutRouter.push('/')
}

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
</script>

<style scoped>
/* --- Стили из предыдущего ответа (оставьте их без изменений) --- */
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
.fact-row { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.fact-label { font-size: 11px; color: #6D7D79; }
.fact-value { font-size: 11px; font-weight: 600; color: #152421; text-align: right; }
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
.assignments-list { display: flex; flex-direction: column; gap: 10px; }
.assignment-item { display: flex; align-items: center; gap: 12px; padding: 14px; background: #F0F4F4; border-radius: 12px; cursor: pointer; transition: background 0.2s; }
.assignment-item:hover { background: #E9F0EE; }
.assignment-icon { width: 40px; height: 40px; background: #102522; border-radius: 12px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; }
.assignment-copy { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.assignment-title { font-size: 14px; font-weight: 700; color: #152421; }
.assignment-meta { font-size: 11px; color: #6D7D79; }
.assignment-tags { display: flex; gap: 6px; }
.tag { padding: 5px 9px; border-radius: 999px; font-size: 10px; font-weight: 700; }
.tag--green { background: #E9F7D5; color: #2E8B57; }
.notification-settings { display: flex; flex-direction: column; gap: 16px; }
.notification-setting { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.setting-copy { display: flex; flex-direction: column; gap: 3px; }
.setting-title { font-size: 13px; font-weight: 600; color: #152421; }
.setting-description { font-size: 11px; color: #6D7D79; }
.toggle-switch { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #E3EAE8; transition: .4s; border-radius: 24px; }
.slider:before { position: absolute; content: ""; height: 20px; width: 20px; left: 2px; bottom: 2px; background-color: white; transition: .4s; border-radius: 50%; }
input:checked + .slider { background-color: #B7F34B; }
input:checked + .slider:before { transform: translateX(20px); background-color: #102522; }
.status-card { background: #102522; padding: 18px; border-radius: 16px; display: flex; align-items: center; gap: 14px; }
.status-icon { width: 42px; height: 42px; background: #19332F; border-radius: 12px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; }
.status-copy { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.status-label { font-size: 10px; font-weight: 700; color: #98A6A2; text-transform: uppercase; }
.status-value { font-size: 16px; font-weight: 700; color: white; }
.status-description { font-size: 10px; color: #98A6A2; }
.status-action { font-size: 11px; font-weight: 700; color: #B7F34B; text-decoration: none; }
.security-actions { display: flex; flex-direction: column; gap: 8px; }
.security-action-item { display: flex; align-items: center; gap: 10px; padding: 12px; background: #F0F4F4; border-radius: 12px; cursor: pointer; transition: background 0.2s; }
.security-action-item:hover { background: #E9F0EE; }
.security-action-item--disabled { cursor: default; opacity: 0.7; }
.security-action-item--disabled:hover { background: #F0F4F4; }
.action-copy { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.action-title { font-size: 13px; font-weight: 600; color: #152421; }
.action-description { font-size: 10px; color: #6D7D79; }
.security-action-item--danger .action-title { color: #D64545; }
.icon-btn { position: relative; width: 44px; height: 44px; background: white; border: 1px solid #E3EAE8; border-radius: 12px; display: flex; justify-content: center; align-items: center; cursor: pointer; }
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
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

/* Состояния и формы */
.org-description { font-size: 13px; color: #6D7D79; line-height: 1.5; white-space: pre-line; }
.field-note { font-size: 12px; color: #98A6A2; }
.soon-text { font-size: 13px; color: #6D7D79; line-height: 1.5; }
.form-field { display: flex; flex-direction: column; gap: 6px; width: 100%; }
.form-label { font-size: 12px; color: var(--color-gray-text); font-weight: 500; }
.form-control {
  width: 100%; min-height: 40px; padding: 8px 12px;
  border: 1px solid var(--color-gray-border); border-radius: 8px;
  font-size: 14px; font-family: inherit; color: var(--color-dark);
  background: var(--color-white); outline: none;
}
.form-control:focus { border-color: var(--color-primary); }
textarea.form-control { resize: vertical; }
.form-hint { font-size: 13px; color: #6D7D79; line-height: 1.4; }
.form-error { font-size: 13px; color: #D64545; }
</style>
