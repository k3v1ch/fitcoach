<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader
        title="Профиль атлета"
        subtitle="Личные данные, спортивная информация и уведомления"
        :show-search="false"
      >
        <template #actions>
            <BaseButton @click="showEditModal = true">
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
            <span class="badge badge--dark">{{ label('role', 'ATHLETE') }}</span>
            <span v-if="statusBadge" class="badge" :class="statusBadge.cls">{{ statusBadge.text }}</span>
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
            <span class="fact-label">Статус карточки</span>
            <span class="fact-value" :class="{ 'fact-value--green': card?.status === 'ACTIVE' }">{{ cardStatusText }}</span>
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
          <!-- Personal Data Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Личные данные</h3>
              <p class="card-subtitle">Информация спортсмена</p>
            </div>
            <StateBlock v-if="cardState" :kind="cardState.kind" :message="cardState.message" />
            <template v-else>
              <div class="info-grid">
                <div class="info-column">
                  <div class="info-field">
                    <div class="field-icon"><BaseIcon name="mail" :size="16" color="#6D7D79" /></div>
                    <div class="field-copy">
                      <span class="field-label">Email</span>
                      <span class="field-value">{{ me?.email || '—' }}</span>
                    </div>
                  </div>
                  <div class="info-field">
                    <div class="field-icon"><BaseIcon name="medal" :size="16" color="#6D7D79" /></div>
                    <div class="field-copy">
                      <span class="field-label">Спортивный уровень</span>
                      <span class="field-value">{{ rankText }}</span>
                    </div>
                  </div>
                  <div class="info-field">
                    <div class="field-icon"><BaseIcon name="user" :size="16" color="#6D7D79" /></div>
                    <div class="field-copy">
                      <span class="field-label">Возраст</span>
                      <span class="field-value">{{ ageText }}</span>
                    </div>
                  </div>
                </div>
                <div class="info-column">
                  <div class="info-field">
                    <div class="field-icon"><BaseIcon name="cake" :size="16" color="#6D7D79" /></div>
                    <div class="field-copy">
                      <span class="field-label">Дата рождения</span>
                      <span class="field-value">{{ formatDate(card?.birthDate) }}</span>
                    </div>
                  </div>
                  <div class="info-field">
                    <div class="field-icon"><BaseIcon name="calendar-check" :size="16" color="#6D7D79" /></div>
                    <div class="field-copy">
                      <span class="field-label">Начало занятий</span>
                      <span class="field-value">{{ formatDate(card?.enrolledOn) }}</span>
                    </div>
                  </div>
                  <div class="info-field">
                    <div class="field-icon"><BaseIcon name="briefcase" :size="16" color="#6D7D79" /></div>
                    <div class="field-copy">
                      <span class="field-label">Организация</span>
                      <span class="field-value">{{ organizationName }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <p class="field-note">Данные карточки ведёт тренер. Телефон и город — раздел появится позже.</p>
            </template>
          </div>

          <!-- Sports Section Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Спортивная секция</h3>
              <p class="card-subtitle">Текущая группа и тренер</p>
            </div>
            <StateBlock v-if="cardState" :kind="cardState.kind" :message="cardState.message" />
            <StateBlock v-else-if="sportsLoading" kind="loading" />
            <StateBlock v-else-if="groupsError" kind="error" :message="errorText(groupsError)" />
            <StateBlock v-else-if="!primaryGroup" kind="empty" message="Вы пока не зачислены в группу — обратитесь к тренеру" />
            <template v-else>
              <div class="section-summary">
                <div class="section-icon"><BaseIcon name="shield" :size="22" color="#B7F34B" /></div>
                <div class="section-details">
                  <span class="section-name">{{ sectionTitle }}</span>
                  <span v-if="sectionSubtitle" class="section-group">{{ sectionSubtitle }}</span>
                  <div class="section-tags">
                    <span v-for="tag in venueTags" :key="tag" class="tag tag--green">{{ tag }}</span>
                    <span class="tag tag--blue">{{ weekdaysTag }}</span>
                  </div>
                </div>
                <div class="coach-info">
                  <span class="coach-label">Тренер</span>
                  <span v-if="coachNames.length" class="coach-name">{{ coachNames.join(', ') }}</span>
                  <span v-else class="coach-name coach-name--muted">Раздел появится позже</span>
                </div>
              </div>
              <div class="training-facts">
                <div class="training-fact fact--green">
                  <span class="fact-label">В ГРУППЕ</span>
                  <span class="fact-value">{{ groupSizeText }}</span>
                </div>
                <div class="training-fact fact--blue">
                  <span class="fact-label">ТРЕНИРОВОК</span>
                  <span class="fact-value">{{ weekCountText }}</span>
                </div>
                <div class="training-fact fact--yellow" :title="attendanceHint">
                  <span class="fact-label">ПОСЕЩАЕМОСТЬ ЗА {{ ATTENDANCE_DAYS }} ДНЕЙ</span>
                  <span class="fact-value">{{ attendanceText }}</span>
                </div>
              </div>
            </template>
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
            <p class="soon-text">Раздел появится позже — выбрать, какие уведомления получать, пока нельзя. Уведомления о тренировках, мероприятиях и объявлениях приходят в колокольчик в шапке страницы.</p>
          </div>

          <!-- Medical Clearance Status Card -->
          <div class="status-card">
            <div class="status-icon">
              <BaseIcon name="heart-pulse" :size="19" color="#B7F34B" />
            </div>
            <div class="status-copy">
              <span class="status-label">Медицинский допуск</span>
              <span class="status-value">{{ medical.value }}</span>
              <span v-if="medical.description" class="status-description">{{ medical.description }}</span>
            </div>
            <a v-if="medicalFileUrl" :href="medicalFileUrl" class="status-action">Справка →</a>
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

      <!-- Edit Modal: карточку спортсмена меняет только тренер -->
      <BaseModal v-model="showEditModal" title="Редактировать профиль" submit-label="Понятно" @submit="showEditModal = false">
        <p class="modal-text">ФИО, дату рождения и дату начала занятий в карточке спортсмена ведёт тренер. Если что-то указано неверно, сообщите ему.</p>
        <p class="modal-text">Самостоятельное редактирование профиля — телефона, города и других данных — раздел появится позже.</p>
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
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  logout, clearSession, currentUser, currentOrganization, myAthletes, selectedAthleteId, selectedAthlete,
  loadMyAthletes, getOrganizationId, hasPermission
} from '../../utils/session'
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { authApi } from '../../api/auth'
import { athletesApi } from '../../api/athletes'
import { groupsApi } from '../../api/groups'
import { sectionsApi } from '../../api/sections'
import { trainingsApi } from '../../api/trainings'
import { attendanceApi } from '../../api/attendance'
import { documentsApi, filesApi } from '../../api/documents'
import { dictionariesApi } from '../../api/dictionaries'
import { organizationsApi } from '../../api/organizations'
import {
  formatDate, formatDateShort, formatTime, formatDateTime, formatWeekday, toIsoDate, toIsoDateTime,
  periodRange, addDays, parseDate, fullName, initials, ageYears, errorText
} from '../../utils/format'
import { label } from '../../utils/labels'

const NO_CARD = 'Карточка спортсмена ещё не создана тренером'
const ATTENDANCE_DAYS = 90

const showEditModal = ref(false)

// ─────────── Аккаунт (GET /me) ───────────
const me = ref(currentUser.value)

async function loadMe() {
  try {
    me.value = await authApi.me()
  } catch (_) {
    // остаются данные сессии; 401 обработает общий обработчик входа
  }
}

const sessionUntil = computed(() => {
  const until = parseDate(me.value?.expiresAt)
  if (!until || isNaN(until)) return '—'
  return toIsoDate(until) === toIsoDate(new Date()) ? `Сегодня, ${formatTime(until)}` : formatDateTime(until)
})

const organizationName = computed(() => currentOrganization.value?.organizationName || '—')

// ─────────── Своя карточка спортсмена (GET /athletes/{id}) ───────────
const athletesReady = ref(false)
const athletesError = ref(null)
const athlete = ref(null)
const cardLoading = ref(false)
const cardError = ref(null)

const card = computed(() => athlete.value || selectedAthlete.value)

const cardState = computed(() => {
  if (!athletesReady.value) return { kind: 'loading', message: '' }
  if (athletesError.value) return { kind: 'error', message: errorText(athletesError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CARD }
  if (!card.value && cardLoading.value) return { kind: 'loading', message: '' }
  if (!card.value && cardError.value) return { kind: 'error', message: errorText(cardError.value) }
  return null
})

const displayName = computed(() => {
  const cardName = card.value ? fullName(card.value) : ''
  if (cardName && cardName !== '—') return cardName
  return me.value?.fullName?.trim() || me.value?.email || '—'
})

const cardStatusText = computed(() => {
  if (!athletesReady.value) return '…'
  if (!card.value) return 'Не создана'
  return label('athleteStatus', card.value.status)
})

const statusBadge = computed(() => {
  if (!card.value) return null
  if (card.value.status === 'ARCHIVED') return { text: label('athleteStatus', 'ARCHIVED'), cls: 'badge--gray' }
  if (groups.value.length) return { text: 'В составе группы', cls: 'badge--green' }
  return { text: label('athleteStatus', card.value.status), cls: 'badge--green' }
})

const ageText = computed(() => {
  const age = ageYears(card.value?.birthDate)
  return age === null || isNaN(age) ? '—' : `${age} ${plural(age, 'год', 'года', 'лет')}`
})

// ─────────── Спортивные данные ───────────
const groups = ref([])
const groupsError = ref(null)
const weekTrainings = ref([])
const weekError = ref(null)
const attendance = ref(null) // AttendanceSummary
const attendanceError = ref(null)
const ranks = ref([])
const ranksError = ref(null)
const medicalDocs = ref([])
const medicalError = ref(null)
const sportsLoading = ref(false)
const coachNames = ref([])

const sections = ref([])
const venues = ref([])

let loadSeq = 0
async function loadSports() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  athlete.value = null
  groups.value = []
  weekTrainings.value = []
  attendance.value = null
  ranks.value = []
  medicalDocs.value = []
  coachNames.value = []
  cardError.value = groupsError.value = weekError.value = attendanceError.value = ranksError.value = medicalError.value = null
  if (!athleteId) {
    cardLoading.value = false
    sportsLoading.value = false
    return
  }
  cardLoading.value = true
  sportsLoading.value = true
  let org
  try {
    org = getOrganizationId()
  } catch (e) {
    cardError.value = groupsError.value = medicalError.value = e
    cardLoading.value = sportsLoading.value = false
    return
  }
  const today = new Date()
  const week = periodRange('week', today)
  const [cardRes, groupRes, weekRes, attendanceRes, rankRes, medicalRes] = await Promise.allSettled([
    athletesApi.get(org, athleteId),
    fetchAll(p => groupsApi.list(org, p), { athleteId, status: 'ACTIVE' }),
    // GET /trainings: [from, to) — занятия групп спортсмена на этой неделе
    fetchAll(p => trainingsApi.list(org, p), {
      athleteId, from: toIsoDateTime(week.from), to: toIsoDateTime(week.to)
    }),
    // GET /attendance: итоги посещаемости за период (summary считается по всему периоду)
    attendanceApi.list(org, {
      athleteId, from: toIsoDate(addDays(today, -ATTENDANCE_DAYS)), to: toIsoDate(today), page: 0, size: 1
    }),
    fetchAll(p => athletesApi.ranks(org, athleteId, p)),
    // Медицинские справки спортсмена (athleteId обязателен: без него сервер не сужает выборку)
    fetchAll(p => documentsApi.list(org, p), { athleteId, type: 'MEDICAL_CERTIFICATE' })
  ])
  if (seq !== loadSeq) return
  if (cardRes.status === 'fulfilled') athlete.value = cardRes.value
  else cardError.value = cardRes.reason
  if (groupRes.status === 'fulfilled') groups.value = groupRes.value
  else groupsError.value = groupRes.reason
  if (weekRes.status === 'fulfilled') weekTrainings.value = weekRes.value
  else weekError.value = weekRes.reason
  if (attendanceRes.status === 'fulfilled') attendance.value = attendanceRes.value?.summary || null
  else attendanceError.value = attendanceRes.reason
  if (rankRes.status === 'fulfilled') ranks.value = rankRes.value
  else ranksError.value = rankRes.reason
  if (medicalRes.status === 'fulfilled') medicalDocs.value = medicalRes.value
  else medicalError.value = medicalRes.reason
  cardLoading.value = false
  sportsLoading.value = false
  loadCoaches(seq)
}

// Имена тренеров отдаёт только GET /members (право members.read) — у спортсмена его обычно нет
async function loadCoaches(seq) {
  const ids = primaryGroup.value?.coachIds || []
  if (!ids.length || !hasPermission('members.read')) return
  try {
    const members = await fetchAll(p => organizationsApi.members(getOrganizationId(), p), { role: 'TRAINER' })
    if (seq !== loadSeq) return
    const byId = new Map(members.map(m => [m.userId, m.fullName]))
    coachNames.value = ids.map(id => byId.get(id)).filter(Boolean)
  } catch (_) {
    coachNames.value = []
  }
}

// Секции и площадки — для подписей; без них блок показывает название группы
async function loadReference() {
  try {
    const org = getOrganizationId()
    const [sectionList, venueList] = await Promise.allSettled([
      fetchAll(p => sectionsApi.list(org, p)),
      fetchAll(p => dictionariesApi.list(org, 'venues', p))
    ])
    sections.value = sectionList.status === 'fulfilled' ? sectionList.value : []
    venues.value = venueList.status === 'fulfilled' ? venueList.value : []
  } catch (_) {
    // нет активной организации — блоки покажут ошибку сами
  }
}

onMounted(async () => {
  loadMe()
  loadReference()
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (e) {
    athletesError.value = e
  } finally {
    athletesReady.value = true
  }
  if (!athletesError.value) loadSports()
})

watch(selectedAthleteId, () => {
  if (athletesReady.value && !athletesError.value) loadSports()
})

// ─────────── Разряд ───────────
const currentRank = computed(() => [...ranks.value]
  .sort((a, b) => String(b.assignedOn || '').localeCompare(String(a.assignedOn || '')))[0] || null)

const rankText = computed(() => {
  if (sportsLoading.value) return '…'
  if (ranksError.value) return '—'
  const rank = currentRank.value
  if (!rank) return 'Разряд не присвоен'
  if (!rank.validUntil) return rank.name
  const expired = rank.validUntil < toIsoDate(new Date())
  return `${rank.name} (${expired ? 'срок истёк' : 'до'} ${formatDateShort(rank.validUntil)})`
})

// ─────────── Секция и группа ───────────
const primaryGroup = computed(() => groups.value[0] || null)

const sectionTitle = computed(() => {
  const group = primaryGroup.value
  const section = sections.value.find(s => s.id === group?.sectionId)
  return [section?.name, group?.name].filter(Boolean).join(' · ')
})

const sectionSubtitle = computed(() => {
  const others = groups.value.slice(1).map(g => g.name).filter(Boolean)
  return [
    primaryGroup.value?.description,
    others.length ? `Также в ${others.length === 1 ? 'группе' : 'группах'}: ${others.join(', ')}` : null
  ].filter(Boolean).join(' · ')
})

const groupWeekTrainings = computed(() => weekTrainings.value
  .filter(t => t.groupId === primaryGroup.value?.id && t.status !== 'CANCELLED'))

const venueTags = computed(() => [...new Set(groupWeekTrainings.value
  .map(t => venues.value.find(v => v.id === t.venueId)?.name)
  .filter(Boolean))].slice(0, 2))

const weekdaysTag = computed(() => {
  if (weekError.value) return 'Расписание недоступно'
  const days = [...new Set(groupWeekTrainings.value.map(t => (new Date(t.startsAt).getDay() + 6) % 7))]
    .sort((a, b) => a - b)
  if (!days.length) return 'Нет занятий на этой неделе'
  const monday = periodRange('week', new Date()).from
  return days.map(i => formatWeekday(addDays(monday, i))).join(' · ')
})

const groupSizeText = computed(() => {
  const n = primaryGroup.value?.athleteCount || 0
  return `${n} ${plural(n, 'спортсмен', 'спортсмена', 'спортсменов')}`
})

const weekCountText = computed(() => {
  if (weekError.value) return '—'
  const n = weekTrainings.value.filter(t => t.status !== 'CANCELLED').length
  return `${n} на этой неделе`
})

const attendanceText = computed(() => {
  if (attendanceError.value) return '—'
  const percent = attendance.value?.attendancePercent
  if (percent === null || percent === undefined || percent === '') return 'Нет отметок'
  return `${Math.round(Number(percent))}%`
})

const attendanceHint = computed(() => {
  const s = attendance.value
  if (attendanceError.value) return errorText(attendanceError.value)
  if (!s) return ''
  return `Был: ${s.present} · Болел: ${s.sick} · Не был: ${s.absent} · Не отмечено: ${s.unmarked}`
})

// ─────────── Медицинский допуск (GET /documents?type=MEDICAL_CERTIFICATE) ───────────
const medical = computed(() => {
  if (!athletesReady.value || sportsLoading.value) return { value: 'Загрузка…', description: '', doc: null }
  if (!selectedAthleteId.value) return { value: 'Нет данных', description: NO_CARD, doc: null }
  if (medicalError.value) return { value: 'Нет данных', description: errorText(medicalError.value), doc: null }
  // Актуальная справка — с самым поздним сроком действия, затем самая свежая по дате выдачи
  const doc = [...medicalDocs.value].sort((a, b) =>
    String(b.validUntil || '').localeCompare(String(a.validUntil || ''))
    || String(b.issuedOn || b.createdAt || '').localeCompare(String(a.issuedOn || a.createdAt || '')))[0]
  if (!doc) return { value: 'Справка не загружена', description: 'Передайте медицинскую справку тренеру', doc: null }
  const today = toIsoDate(new Date())
  const value = !doc.validUntil
    ? 'Срок действия не указан'
    : doc.validUntil < today ? `Срок истёк ${formatDateShort(doc.validUntil)}` : `Действует до ${formatDateShort(doc.validUntil)}`
  const description = [doc.title, doc.issuedOn ? `выдана ${formatDateShort(doc.issuedOn)}` : null].filter(Boolean).join(' · ')
  return { value, description, doc }
})

const medicalFileUrl = computed(() => {
  const fileId = medical.value.doc?.file?.id
  if (!fileId) return ''
  try {
    return filesApi.contentUrl(getOrganizationId(), fileId)
  } catch (_) {
    return ''
  }
})

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

const logoutRouter = useRouter()

async function handleLogout() {
  await logout()
  logoutRouter.push('/')
}

// ─────────── Помощники ───────────
// Все страницы списка Page<T> (size ≤ 100), не больше maxPages запросов
async function fetchAll(request, params = {}, maxPages = 10) {
  const first = await request({ ...params, page: 0, size: 100 })
  const pages = Math.min(first?.totalPages || 1, maxPages)
  const rest = await Promise.all(
    Array.from({ length: pages - 1 }, (_, i) => request({ ...params, page: i + 1, size: 100 }))
  )
  return rest.reduce((all, page) => all.concat(page?.items || []), first?.items || [])
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
.badge--gray { background: #EEF1F0; color: #888888; }
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
.field-note { font-size: 12px; color: #98A6A2; }
.section-summary { display: flex; align-items: center; gap: 14px; padding: 16px; background: #F0F4F4; border-radius: 16px; }
.section-icon { width: 52px; height: 52px; background: #102522; border-radius: 12px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; }
.section-details { flex: 1; display: flex; flex-direction: column; gap: 5px; }
.section-name { font-size: 16px; font-weight: 700; color: #152421; }
.section-group { font-size: 12px; color: #6D7D79; }
.section-tags { display: flex; gap: 6px; margin-top: 4px; flex-wrap: wrap; }
.tag { padding: 5px 9px; border-radius: 999px; font-size: 10px; font-weight: 700; }
.tag--green { background: #E9F7D5; color: #2E8B57; }
.tag--blue { background: #DDECFB; color: #35678E; }
.coach-info { width: 190px; display: flex; flex-direction: column; gap: 4px; }
.coach-label { font-size: 10px; font-weight: 700; color: #98A6A2; text-transform: uppercase; }
.coach-name { font-size: 13px; font-weight: 700; color: #152421; }
.coach-name--muted { font-size: 12px; font-weight: 600; color: #98A6A2; }
.coach-link { font-size: 11px; color: #35678E; text-decoration: none; }
.training-facts { display: flex; gap: 12px; }
.training-fact { flex: 1; padding: 12px; border-radius: 12px; display: flex; flex-direction: column; gap: 4px; }
.training-fact.fact--green { background: #E9F7D5; }
.training-fact.fact--blue { background: #DDECFB; }
.training-fact.fact--yellow { background: #FFF1D6; }
.training-fact .fact-label { font-size: 10px; }
.training-fact.fact--green .fact-label { color: #2E8B57; }
.training-fact.fact--blue .fact-label { color: #35678E; }
.training-fact.fact--yellow .fact-label { color: #A36A16; }
.training-fact .fact-value { font-size: 15px; font-weight: 700; color: #152421; text-align: left; }
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
.security-action-item--disabled { cursor: default; }
.security-action-item--disabled:hover { background: #F0F4F4; }
.security-action-item--disabled .action-title { color: #6D7D79; }
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
.modal-text { font-size: 14px; color: #6D7D79; line-height: 1.5; }
.form-hint { font-size: 12px; color: #98A6A2; line-height: 1.4; }
.form-error { font-size: 13px; color: #D64545; line-height: 1.4; }
</style>
