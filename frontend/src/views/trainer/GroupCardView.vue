<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        :title="group ? group.name : 'Группа'"
        :subtitle="subtitle"
        :show-back="true"
        :show-search="false"
        @back="$router.push('/trainer/groups')"
      />

      <div v-if="loading" class="card">
        <StateBlock kind="loading" />
      </div>
      <div v-else-if="loadError" class="card">
        <StateBlock kind="error" :message="loadError" />
      </div>

      <template v-else-if="group">
        <div class="tabs-row">
          <div class="tabs">
            <button
              v-for="tab in tabs"
              :key="tab"
              class="tab-btn"
              :class="{ active: activeTab === tab }"
              @click="activeTab = tab"
            >
              {{ tab }}
            </button>
          </div>
          <div class="actions">
            <span v-if="notice" class="notice">{{ notice }}</span>
            <button v-if="canWriteGroups" class="btn-outline" @click="openEdit">Редактировать группу</button>
            <button v-if="canMessage" class="btn-dark" @click="openMessage">Написать всем</button>
          </div>
        </div>

        <!-- Состав группы -->
        <div v-if="activeTab === TAB_ROSTER" class="main-blocks">
          <div class="card athletes-card">
            <div class="card-head">
              <h3>Зарегистрированные спортсмены</h3>
              <button v-if="canEnroll" class="btn-outline" title="Зачислить спортсмена" @click="openEnroll">Зачислить</button>
            </div>
            <StateBlock v-if="!athletes.length" kind="empty" message="В группе пока нет спортсменов" />
            <div v-else class="athlete-list">
              <div v-for="(athlete, index) in athletes" :key="athlete.athleteId" class="athlete-line">
                <div class="avatar" :style="avatarStyle(index)">
                  {{ initials(athlete.fullName) }}
                </div>
                <div class="details link" @click="openAthlete(athlete.athleteId)">
                  <div class="name">{{ athlete.fullName }}</div>
                  <div class="rank">в группе с {{ formatDateShort(athlete.joinedOn) }}</div>
                </div>
                <div class="attendance">
                  <div class="label">ПОСЕЩАЕМОСТЬ</div>
                  <div class="value" :class="percentClass(athleteStats(athlete.athleteId).percent)">
                    {{ athletePercentText(athlete.athleteId) }}
                  </div>
                </div>
                <div class="note">{{ athleteNote(athlete.athleteId) }}</div>
                <button
                  v-if="canWriteGroups"
                  class="line-btn"
                  title="Исключить из группы"
                  aria-label="Исключить из группы"
                  @click="openRemove(athlete)"
                >
                  <BaseIcon name="close" :size="12" color="#D64545" />
                </button>
              </div>
            </div>
          </div>

          <!-- Боковая статистика -->
          <div class="sidebar-stats">
            <div class="card info-card">
              <h3>Статистика посещаемости</h3>
              <StateBlock v-if="attendance.loading" kind="loading" />
              <StateBlock v-else-if="attendance.unavailable" kind="empty" message="Статистика посещаемости появится позже" />
              <StateBlock v-else-if="attendance.error" kind="error" :message="attendance.error" />
              <div v-else class="radial">
                <div class="circle"></div>
                <div>
                  <div class="percent">{{ percentText(groupPercent) }}</div>
                  <div class="desc">{{ groupPercent === null ? `Отметок за ${ATTENDANCE_DAYS} дней пока нет` : `Средний показатель группы за ${ATTENDANCE_DAYS} дней` }}</div>
                </div>
              </div>
            </div>

            <div class="card next-trainings">
              <h3>Ближайшие занятия</h3>
              <StateBlock v-if="schedule.loading" kind="loading" />
              <StateBlock v-else-if="schedule.error" kind="error" :message="schedule.error" />
              <StateBlock v-else-if="!upcoming.length" kind="empty" message="На ближайшие 2 недели занятий нет" />
              <template v-else>
                <div
                  v-for="training in upcoming.slice(0, UPCOMING_LIMIT)"
                  :key="training.id"
                  class="training-item link"
                  @click="openTraining(training)"
                >
                  <div class="date">{{ longDate(training.startsAt) }}</div>
                  <div class="info">{{ formatTime(training.startsAt) }} · {{ training.title }}{{ venueSuffix(training) }}</div>
                </div>
                <button v-if="upcoming.length > UPCOMING_LIMIT" class="link-btn" @click="activeTab = TAB_SCHEDULE">
                  Ещё {{ upcoming.length - UPCOMING_LIMIT }} — открыть расписание группы
                </button>
              </template>
            </div>
          </div>
        </div>

        <!-- Расписание -->
        <div v-else-if="activeTab === TAB_SCHEDULE" class="card">
          <h3>Занятия на 2 недели</h3>
          <StateBlock v-if="schedule.loading" kind="loading" />
          <StateBlock v-else-if="schedule.error" kind="error" :message="schedule.error" />
          <StateBlock v-else-if="!schedule.items.length" kind="empty" message="На ближайшие 2 недели занятий нет" />
          <template v-else>
            <div
              v-for="training in schedule.items"
              :key="training.id"
              class="training-item link"
              @click="openTraining(training)"
            >
              <div class="item-head">
                <div class="date">{{ longDate(training.startsAt) }}</div>
                <span class="status-badge" :class="`status-${tone(training.status)}`">{{ label('trainingStatus', training.status) }}</span>
              </div>
              <div class="info">{{ formatTime(training.startsAt) }}–{{ formatTime(training.endsAt) }} · {{ training.title }}{{ venueSuffix(training) }}</div>
            </div>
          </template>
        </div>

        <!-- Посещаемость -->
        <div v-else-if="activeTab === TAB_ATTENDANCE" class="main-blocks">
          <div class="card">
            <h3>Посещаемость за {{ ATTENDANCE_DAYS }} дней</h3>
            <StateBlock v-if="attendance.loading" kind="loading" />
            <StateBlock v-else-if="attendance.unavailable" kind="empty" message="Статистика посещаемости появится позже" />
            <StateBlock v-else-if="attendance.error" kind="error" :message="attendance.error" />
            <StateBlock v-else-if="!athletes.length" kind="empty" message="В группе пока нет спортсменов" />
            <div v-else class="athlete-list">
              <div v-for="athlete in athletesByAttendance" :key="athlete.athleteId" class="athlete-line">
                <div class="details link" @click="openAthlete(athlete.athleteId)">
                  <div class="name">{{ athlete.fullName }}</div>
                  <div class="rank">{{ athleteNote(athlete.athleteId) }}</div>
                </div>
                <div class="attendance">
                  <div class="label">ПОСЕЩАЕМОСТЬ</div>
                  <div class="value" :class="percentClass(athleteStats(athlete.athleteId).percent)">
                    {{ athletePercentText(athlete.athleteId) }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="card info-card">
            <h3>Итоги группы</h3>
            <StateBlock v-if="attendance.loading" kind="loading" />
            <StateBlock v-else-if="attendance.unavailable" kind="empty" message="Статистика посещаемости появится позже" />
            <StateBlock v-else-if="attendance.error" kind="error" :message="attendance.error" />
            <div v-else class="summary-grid">
              <div v-for="item in summaryItems" :key="item.label" class="summary-item">
                <span class="summary-label">{{ item.label }}</span>
                <span class="summary-value">{{ item.value }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Мероприятия -->
        <div v-else class="card">
          <h3>Мероприятия секции</h3>
          <StateBlock v-if="events.loading" kind="loading" />
          <StateBlock v-else-if="events.error" kind="error" :message="events.error" />
          <StateBlock v-else-if="!events.items.length" kind="empty" message="Предстоящих мероприятий секции нет" />
          <template v-else>
            <div v-for="event in events.items" :key="event.id" class="training-item">
              <div class="item-head">
                <div class="date">{{ event.title }}</div>
                <span class="status-badge" :class="`status-${tone(event.status)}`">{{ label('eventStatus', event.status) }}</span>
              </div>
              <div class="info">{{ eventInfo(event) }}</div>
            </div>
          </template>
        </div>
      </template>

      <!-- Редактирование группы -->
      <BaseModal
        v-model="edit.open"
        title="Редактировать группу"
        :submit-label="edit.saving ? 'Сохранение…' : 'Сохранить'"
        @submit="submitEdit"
      >
        <BaseInput id="group-edit-name" v-model="edit.name" label="Название группы" placeholder="Группа А2" />
        <BaseInput id="group-edit-description" v-model="edit.description" label="Описание" placeholder="Необязательно" />
        <div class="field">
          <span class="field-label">Тренеры</span>
          <div class="check-list">
            <label v-for="coach in coachChoices" :key="coach.userId" class="check-item">
              <input v-model="edit.coachIds" type="checkbox" :value="coach.userId" />
              {{ coach.fullName }}
            </label>
          </div>
        </div>
        <div class="field">
          <label class="field-label" for="group-edit-status">Статус</label>
          <select id="group-edit-status" v-model="edit.status" class="field-control" :disabled="group?.status === 'ARCHIVED'">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
          <p v-if="group?.status === 'ARCHIVED'" class="form-hint">Архивную группу нельзя вернуть в работу.</p>
        </div>
        <p v-if="edit.error" class="form-error">{{ edit.error }}</p>
      </BaseModal>

      <!-- Зачисление -->
      <BaseModal
        v-model="enroll.open"
        title="Зачислить спортсмена"
        :submit-label="enroll.saving ? 'Сохранение…' : 'Зачислить'"
        @submit="submitEnroll"
      >
        <BaseInput id="enroll-search" v-model="enroll.q" label="Спортсмен" placeholder="Фамилия или имя" />
        <div class="pick-list">
          <StateBlock v-if="enroll.loading" kind="loading" />
          <StateBlock v-else-if="enroll.listError" kind="error" :message="enroll.listError" />
          <StateBlock v-else-if="!enrollOptions.length" kind="empty" message="Подходящих спортсменов не найдено" />
          <template v-else>
            <button
              v-for="candidate in enrollOptions"
              :key="candidate.id"
              type="button"
              class="pick-item"
              :class="{ selected: enroll.athleteId === candidate.id }"
              @click="enroll.athleteId = candidate.id"
            >
              <span>{{ fullName(candidate) }}</span>
              <span class="pick-meta">{{ candidate.birthDate ? `д. р. ${formatDateShort(candidate.birthDate)}` : '' }}</span>
            </button>
          </template>
        </div>
        <BaseInput id="enroll-joined-on" v-model="enroll.joinedOn" type="date" label="Дата зачисления" />
        <p v-if="enroll.error" class="form-error">{{ enroll.error }}</p>
      </BaseModal>

      <!-- Исключение -->
      <BaseModal
        v-model="removal.open"
        title="Исключить из группы"
        :submit-label="removal.saving ? 'Сохранение…' : 'Исключить'"
        @submit="submitRemove"
      >
        <p class="modal-text">
          {{ removal.athlete?.fullName }} — участие в группе завершится в указанную дату.
          Посещения и платежи сохранятся.
        </p>
        <BaseInput id="removal-left-on" v-model="removal.leftOn" type="date" label="Дата выхода из группы" />
        <p v-if="removal.error" class="form-error">{{ removal.error }}</p>
      </BaseModal>

      <!-- Сообщение группе (объявление) -->
      <BaseModal
        v-model="message.open"
        title="Написать всем"
        :submit-label="message.saving ? 'Отправка…' : 'Отправить'"
        @submit="submitMessage"
      >
        <BaseInput id="message-title" v-model="message.title" label="Тема" placeholder="Например: перенос тренировки" />
        <div class="field">
          <label class="field-label" for="message-text">Текст</label>
          <textarea id="message-text" v-model="message.text" class="field-control field-textarea" rows="5"></textarea>
        </div>
        <p v-if="message.resolveError" class="form-error">Не удалось определить получателей: {{ message.resolveError }}</p>
        <p v-else class="form-hint">{{ recipientsHint }}</p>
        <p v-if="message.error" class="form-error">{{ message.error }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { api } from '../../api/index'
import { groupsApi } from '../../api/groups'
import { sectionsApi } from '../../api/sections'
import { trainingsApi } from '../../api/trainings'
import { attendanceApi } from '../../api/attendance'
import { athletesApi } from '../../api/athletes'
import { eventsApi } from '../../api/events'
import { announcementsApi } from '../../api/announcements'
import { dictionariesApi } from '../../api/dictionaries'
import { organizationsApi } from '../../api/organizations'
import { getOrganizationId, hasPermission, hasRole, currentUser } from '../../utils/session'
import {
  errorText, formatDate, formatDateShort, formatDateTime, formatTime, fullName, initials,
  toIsoDate, toIsoDateTime, addDays
} from '../../utils/format'
import { label, options, tone } from '../../utils/labels'

const TAB_ROSTER = 'Состав группы'
const TAB_SCHEDULE = 'Расписание'
const TAB_ATTENDANCE = 'Посещаемость'
const TAB_EVENTS = 'Мероприятия'
const tabs = [TAB_ROSTER, TAB_SCHEDULE, TAB_ATTENDANCE, TAB_EVENTS]
const ATTENDANCE_DAYS = 30
const SCHEDULE_DAYS = 14
const UPCOMING_LIMIT = 3
const FIELD_LABELS = {
  name: 'Название', description: 'Описание', coachIds: 'Тренеры', status: 'Статус',
  athleteId: 'Спортсмен', joinedOn: 'Дата зачисления', leftOn: 'Дата выхода',
  title: 'Тема', text: 'Текст', recipientUserIds: 'Получатели'
}
const AVATAR_TONES = [
  { background: '#E9F7D5', color: '#2D5B24' },
  { background: '#DDECFB', color: '#35678E' },
  { background: '#FFF8E6', color: '#F2B705' },
  { background: '#FCE2E5', color: '#D64545' }
]
const statusOptions = options('recordStatus')

const route = useRoute()
const router = useRouter()
const groupId = computed(() => route.params.id)
const activeTab = ref(TAB_ROSTER)

// ─────────── Группа и состав (GroupDetail) ───────────
const group = ref(null)
const athletes = ref([])
const loading = ref(true)
const loadError = ref('')
const sectionName = ref('')

// ─────────── Права (запись — только роль TRAINER с нужным правом) ───────────
const isTrainer = computed(() => hasRole('TRAINER'))
const canWriteGroups = computed(() => isTrainer.value && hasPermission('groups.write'))
const canEnroll = computed(() => canWriteGroups.value && group.value?.status === 'ACTIVE')
const canMessage = computed(() => isTrainer.value && hasPermission('announcements.write') && athletes.value.length > 0)

// В коде бэкенда GroupDetail.athletes — массив GroupAthlete (в документе — Page): принимаем оба вида
function rosterOf(detail) {
  const list = Array.isArray(detail.athletes) ? detail.athletes : (detail.athletes?.items || [])
  return [...list].sort((a, b) => (a.fullName || '').localeCompare(b.fullName || '', 'ru'))
}

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  loading.value = true
  loadError.value = ''
  try {
    const detail = await groupsApi.get(getOrganizationId(), groupId.value)
    if (seq !== loadSeq) return
    group.value = detail.group
    athletes.value = rosterOf(detail)
  } catch (e) {
    if (seq !== loadSeq) return
    group.value = null
    athletes.value = []
    loadError.value = errorText(e)
    return
  } finally {
    if (seq === loadSeq) loading.value = false
  }
  loadSection()
  loadSchedule()
  loadAttendance()
  if (activeTab.value === TAB_EVENTS) loadEvents()
}

// Тихое обновление после зачисления/исключения
async function refreshGroup() {
  try {
    const detail = await groupsApi.get(getOrganizationId(), groupId.value)
    group.value = detail.group
    athletes.value = rosterOf(detail)
  } catch (e) {
    loadError.value = errorText(e)
  }
}

// Отдельного GET секции нет — ищем название в списке секций
async function loadSection() {
  const id = groupId.value
  try {
    const list = await fetchAll(params => sectionsApi.list(getOrganizationId(), params))
    if (id === groupId.value) sectionName.value = list.find(s => s.id === group.value?.sectionId)?.name || ''
  } catch (_) {
    if (id === groupId.value) sectionName.value = ''
  }
}

const subtitle = computed(() => {
  if (!group.value) return loading.value ? 'Загрузка…' : ''
  const n = athletes.value.length
  const parts = [`Секция: ${sectionName.value || '—'}`, `${n} ${plural(n, 'спортсмен', 'спортсмена', 'спортсменов')}`]
  const names = (group.value.coachIds || []).map(trainerName).filter(Boolean)
  if (names.length) parts.push(`${names.length > 1 ? 'Тренеры' : 'Тренер'}: ${names.join(', ')}`)
  if (group.value.status === 'ARCHIVED') parts.push('В архиве')
  return parts.join(' · ')
})

function avatarStyle(index) {
  return AVATAR_TONES[index % AVATAR_TONES.length]
}

function openAthlete(athleteId) {
  router.push(`/trainer/athletes/${athleteId}`)
}

// ─────────── Тренеры и площадки (общие справочники организации) ───────────
const trainers = ref([])
const venues = ref([])

async function loadTrainers() {
  try {
    trainers.value = await fetchAll(params => organizationsApi.members(getOrganizationId(), params), { role: 'TRAINER' })
  } catch (_) {
    trainers.value = [] // нет members.read — имена тренеров недоступны
  }
}

async function loadVenues() {
  try {
    venues.value = await fetchAll(params => dictionariesApi.list(getOrganizationId(), 'venues', params))
  } catch (_) {
    venues.value = []
  }
}

function trainerName(id) {
  const member = trainers.value.find(m => m.userId === id)
  if (member) return member.fullName
  return id === currentUser.value?.userId ? currentUser.value.fullName : null
}

function venueSuffix(training) {
  const name = venues.value.find(v => v.id === training.venueId)?.name
  return name ? ` (${name})` : ''
}

// ─────────── Занятия группы на 2 недели ───────────
const schedule = reactive({ loading: false, error: '', items: [] })

async function loadSchedule() {
  const id = groupId.value
  const from = new Date()
  schedule.loading = true
  schedule.error = ''
  try {
    const items = await fetchAll(params => trainingsApi.list(getOrganizationId(), params), {
      groupId: id,
      from: toIsoDateTime(from),
      to: toIsoDateTime(addDays(from, SCHEDULE_DAYS))
    })
    if (id === groupId.value) schedule.items = items
  } catch (e) {
    if (id === groupId.value) {
      schedule.items = []
      schedule.error = errorText(e)
    }
  } finally {
    if (id === groupId.value) schedule.loading = false
  }
}

const upcoming = computed(() => schedule.items.filter(t => t.status === 'PLANNED'))

function openTraining(training) {
  router.push(`/trainer/trainings/${training.id}/report`)
}

// «Пятница, 18 сентября»
function longDate(value) {
  const text = new Date(value).toLocaleDateString('ru-RU', { weekday: 'long', day: 'numeric', month: 'long' })
  return text.charAt(0).toUpperCase() + text.slice(1)
}

// ─────────── Посещаемость за 30 дней (GET /attendance: журнал + summary) ───────────
const attendance = reactive({ loading: false, error: '', unavailable: false, summary: null, items: [] })

async function loadAttendance() {
  const id = groupId.value
  const today = new Date()
  attendance.loading = true
  attendance.error = ''
  attendance.unavailable = false
  try {
    const params = { groupId: id, from: toIsoDate(addDays(today, -(ATTENDANCE_DAYS - 1))), to: toIsoDate(today) }
    const { first, items } = await fetchPages(p => attendanceApi.list(getOrganizationId(), p), params)
    if (id !== groupId.value) return
    attendance.summary = first.summary || null
    attendance.items = items
  } catch (e) {
    if (id !== groupId.value) return
    attendance.summary = null
    attendance.items = []
    // Эндпоинт журнала ещё может отсутствовать на сервере — это не ошибка группы
    if ([404, 405, 501].includes(e.status)) attendance.unavailable = true
    else attendance.error = errorText(e)
  } finally {
    if (id === groupId.value) attendance.loading = false
  }
}

const attendanceByAthlete = computed(() => {
  const map = {}
  for (const item of attendance.items) {
    const s = map[item.athleteId] || (map[item.athleteId] = { PRESENT: 0, SICK: 0, ABSENT: 0, UNMARKED: 0 })
    if (item.status in s) s[item.status] += 1
  }
  return map
})

// Процент = PRESENT / (PRESENT + SICK + ABSENT) × 100; без отметок — null
function athleteStats(athleteId) {
  const s = attendanceByAthlete.value[athleteId] || { PRESENT: 0, SICK: 0, ABSENT: 0, UNMARKED: 0 }
  const marked = s.PRESENT + s.SICK + s.ABSENT
  return { ...s, percent: marked ? (s.PRESENT / marked) * 100 : null }
}

function athletePercentText(athleteId) {
  if (attendance.loading) return '…'
  if (attendance.error || attendance.unavailable) return '—'
  return percentText(athleteStats(athleteId).percent)
}

// Только ненулевые отметки: «Был 1 · Не был 1»
function athleteNote(athleteId) {
  if (attendance.loading || attendance.error || attendance.unavailable) return ''
  const s = athleteStats(athleteId)
  const parts = [['Был', s.PRESENT], ['Болел', s.SICK], ['Не был', s.ABSENT], ['Не отмечен', s.UNMARKED]]
    .filter(([, n]) => n > 0)
    .map(([word, n]) => `${word} ${n}`)
  return parts.length ? parts.join(' · ') : `Нет отметок за ${ATTENDANCE_DAYS} дней`
}

const totals = computed(() => {
  const s = attendance.summary
  if (s) return { trainings: s.trainingCount, present: s.present, sick: s.sick, absent: s.absent, unmarked: s.unmarked }
  const t = { trainings: new Set(attendance.items.map(i => i.trainingId)).size, present: 0, sick: 0, absent: 0, unmarked: 0 }
  for (const item of attendance.items) {
    const key = String(item.status || '').toLowerCase()
    if (key in t && key !== 'trainings') t[key] += 1
  }
  return t
})

const groupPercent = computed(() => {
  const s = attendance.summary
  if (s && s.attendancePercent !== undefined) {
    return s.attendancePercent === null ? null : Number(s.attendancePercent)
  }
  const { present, sick, absent } = totals.value
  const marked = present + sick + absent
  return marked ? (present / marked) * 100 : null
})

const summaryItems = computed(() => [
  { label: 'Посещаемость', value: percentText(groupPercent.value) },
  { label: 'Занятий', value: totals.value.trainings ?? '—' },
  { label: 'Был', value: totals.value.present ?? '—' },
  { label: 'Болел', value: totals.value.sick ?? '—' },
  { label: 'Не был', value: totals.value.absent ?? '—' },
  { label: 'Не отмечено', value: totals.value.unmarked ?? '—' }
])

// Сначала те, кому нужно внимание: низкий процент, затем без отметок
const athletesByAttendance = computed(() => [...athletes.value].sort((a, b) => {
  const pa = athleteStats(a.athleteId).percent
  const pb = athleteStats(b.athleteId).percent
  if (pa === null && pb === null) return 0
  if (pa === null) return 1
  if (pb === null) return -1
  return pa - pb
}))

function percentText(value) {
  if (value === null || value === undefined || isNaN(Number(value))) return '—'
  return `${Number(value).toLocaleString('ru-RU', { maximumFractionDigits: 1 })}%`
}

function percentClass(value) {
  if (value === null || value === undefined) return 'none'
  if (value >= 80) return ''
  return value >= 60 ? 'mid' : 'low'
}

// ─────────── Мероприятия секции группы ───────────
const events = reactive({ loading: false, error: '', items: [], loadedFor: null })

async function loadEvents() {
  const id = groupId.value
  const sectionId = group.value?.sectionId
  if (!sectionId) return
  events.loading = true
  events.error = ''
  try {
    const items = await fetchAll(params => eventsApi.list(getOrganizationId(), params), { from: toIsoDate(new Date()) })
    if (id !== groupId.value) return
    // GET /events в коде бэкенда не принимает sectionId — отбираем мероприятия секции на клиенте
    events.items = items
      .filter(e => e.sectionId === sectionId)
      .sort((a, b) => String(a.startsAt || a.collectionDueOn || '').localeCompare(String(b.startsAt || b.collectionDueOn || '')))
    events.loadedFor = id
  } catch (e) {
    if (id !== groupId.value) return
    events.items = []
    events.error = errorText(e)
  } finally {
    if (id === groupId.value) events.loading = false
  }
}

function eventInfo(event) {
  const parts = [label('eventType', event.type)]
  if (event.startsAt) parts.push(formatDateTime(event.startsAt))
  else if (event.collectionDueOn) parts.push(`сбор до ${formatDate(event.collectionDueOn)}`)
  if (event.location) parts.push(event.location)
  return parts.join(' · ')
}

// ─────────── Редактирование группы (PATCH: name, description, coachIds, status) ───────────
const edit = reactive({ open: false, saving: false, error: '', name: '', description: '', coachIds: [], status: 'ACTIVE' })

// Активные тренеры + текущие тренеры группы (чтобы их можно было снять)
const coachChoices = computed(() => {
  const choices = new Map()
  for (const m of trainers.value) if (m.status === 'ACTIVE') choices.set(m.userId, m.fullName)
  const me = currentUser.value
  if (isTrainer.value && me && !choices.has(me.userId)) choices.set(me.userId, me.fullName)
  for (const id of group.value?.coachIds || []) {
    if (choices.has(id)) continue
    const member = trainers.value.find(m => m.userId === id)
    choices.set(id, member ? `${member.fullName} (доступ приостановлен)` : 'Тренер')
  }
  return [...choices].map(([userId, name]) => ({ userId, fullName: name }))
})

function openEdit() {
  const g = group.value
  Object.assign(edit, {
    open: true, saving: false, error: '',
    name: g.name,
    description: g.description || '',
    coachIds: [...(g.coachIds || [])],
    status: g.status
  })
}

function sameIds(a, b) {
  return a.length === b.length && a.every(id => b.includes(id))
}

async function submitEdit() {
  if (edit.saving) return
  const g = group.value
  const name = edit.name.trim()
  const description = edit.description.trim() || null
  if (!name) { edit.error = 'Укажите название группы.'; return }
  if (!edit.coachIds.length) { edit.error = 'Выберите хотя бы одного тренера.'; return }

  const patch = {}
  if (name !== g.name) patch.name = name
  if (description !== (g.description || null)) patch.description = description
  if (!sameIds(edit.coachIds, g.coachIds || [])) patch.coachIds = [...edit.coachIds]
  if (edit.status !== g.status) patch.status = edit.status
  if (!Object.keys(patch).length) { edit.open = false; return }
  if (patch.status === 'ARCHIVED' &&
      !confirm(`Архивировать группу «${g.name}»? Вернуть её в работу будет нельзя; история состава, занятий и платежей сохранится.`)) return

  edit.saving = true
  edit.error = ''
  try {
    group.value = await groupsApi.update(getOrganizationId(), g.id, patch)
    edit.open = false
  } catch (e) {
    edit.error = formErrorText(e)
  } finally {
    edit.saving = false
  }
}

// ─────────── Зачисление спортсмена ───────────
const enroll = reactive({
  open: false, saving: false, error: '',
  q: '', loading: false, listError: '', results: [],
  athleteId: '', joinedOn: ''
})

function openEnroll() {
  Object.assign(enroll, {
    open: true, saving: false, error: '',
    q: '', listError: '', results: [],
    athleteId: '', joinedOn: toIsoDate(new Date())
  })
  searchAthletes()
}

let enrollTimer = null
let enrollSeq = 0
watch(() => enroll.q, () => {
  if (!enroll.open) return
  clearTimeout(enrollTimer)
  enrollTimer = setTimeout(searchAthletes, 300)
})

async function searchAthletes() {
  const seq = ++enrollSeq
  enroll.loading = true
  enroll.listError = ''
  try {
    const page = await athletesApi.list(getOrganizationId(), { q: enroll.q.trim(), status: 'ACTIVE', size: 20 })
    if (seq === enrollSeq) enroll.results = page.items || []
  } catch (e) {
    if (seq === enrollSeq) {
      enroll.results = []
      enroll.listError = errorText(e)
    }
  } finally {
    if (seq === enrollSeq) enroll.loading = false
  }
}

const enrollOptions = computed(() => {
  const inGroup = new Set(athletes.value.map(a => a.athleteId))
  return enroll.results.filter(a => !inGroup.has(a.id))
})

async function submitEnroll() {
  if (enroll.saving) return
  if (!enroll.athleteId) { enroll.error = 'Выберите спортсмена.'; return }
  if (!enroll.joinedOn) { enroll.error = 'Укажите дату зачисления.'; return }
  enroll.saving = true
  enroll.error = ''
  try {
    await groupsApi.addAthlete(getOrganizationId(), group.value.id, enroll.athleteId, enroll.joinedOn)
    enroll.open = false
    await refreshGroup()
  } catch (e) {
    enroll.error = formErrorText(e)
  } finally {
    enroll.saving = false
  }
}

// ─────────── Исключение спортсмена (завершение текущего периода участия) ───────────
const removal = reactive({ open: false, saving: false, error: '', athlete: null, leftOn: '' })

function openRemove(athlete) {
  Object.assign(removal, { open: true, saving: false, error: '', athlete, leftOn: toIsoDate(new Date()) })
}

async function submitRemove() {
  if (removal.saving) return
  const athlete = removal.athlete
  if (!removal.leftOn) { removal.error = 'Укажите дату выхода из группы.'; return }
  if (athlete.joinedOn && removal.leftOn < athlete.joinedOn) {
    removal.error = `Дата выхода не может быть раньше даты зачисления (${formatDate(athlete.joinedOn)}).`
    return
  }
  removal.saving = true
  removal.error = ''
  try {
    await groupsApi.removeAthlete(getOrganizationId(), group.value.id, athlete.athleteId, removal.leftOn)
    removal.open = false
    await refreshGroup()
  } catch (e) {
    removal.error = formErrorText(e)
  } finally {
    removal.saving = false
  }
}

// ─────────── «Написать всем»: объявление спортсменам группы с аккаунтом и их родителям ───────────
const message = reactive({
  open: false, saving: false, error: '',
  title: '', text: '',
  resolving: false, resolveError: '', recipients: [],
  draftId: null
})
const notice = ref('')
let noticeTimer = null

async function openMessage() {
  Object.assign(message, {
    open: true, saving: false, error: '',
    title: '', text: '',
    resolving: true, resolveError: '', recipients: [],
    draftId: null
  })
  try {
    message.recipients = await resolveRecipients()
  } catch (e) {
    message.resolveError = errorText(e)
  } finally {
    message.resolving = false
  }
}

async function resolveRecipients() {
  const org = getOrganizationId()
  const cards = await Promise.all(athletes.value.map(a => athletesApi.get(org, a.athleteId)))
  // Заблокированных участников сервер не примет получателями — исключаем, если список доступен
  let blocked = new Set()
  try {
    const list = await fetchAll(params => organizationsApi.members(org, params), { status: 'BLOCKED' })
    blocked = new Set(list.map(m => m.userId))
  } catch (_) { /* нет members.read — проверит сервер */ }
  const ids = new Set()
  for (const card of cards) {
    if (card.userId) ids.add(card.userId)
    for (const parent of card.parents || []) if (parent.parentUserId) ids.add(parent.parentUserId)
  }
  return [...ids].filter(id => !blocked.has(id))
}

const recipientsHint = computed(() => {
  if (message.resolving) return 'Определяем получателей…'
  const n = message.recipients.length
  if (!n) return 'У спортсменов группы нет аккаунтов и привязанных родителей — сообщение некому доставить.'
  return `Объявление получат ${n} ${plural(n, 'человек', 'человека', 'человек')}: спортсмены группы с аккаунтом и их родители.`
})

async function submitMessage() {
  if (message.saving || message.resolving) return
  const title = message.title.trim()
  const text = message.text.trim()
  if (!title) { message.error = 'Укажите тему сообщения.'; return }
  if (!text) { message.error = 'Напишите текст сообщения.'; return }
  if (message.resolveError || !message.recipients.length) { message.error = 'Некому отправить сообщение.'; return }

  const write = {
    title,
    text,
    category: group.value.name.slice(0, 100),
    recipientUserIds: [...message.recipients],
    requiresResponse: false,
    responseDeadline: null,
    attachmentFileIds: []
  }
  message.saving = true
  message.error = ''
  try {
    const org = getOrganizationId()
    // Черновик создаётся один раз: при повторной попытке публикуем уже созданный
    if (!message.draftId) message.draftId = (await announcementsApi.create(org, write)).id
    await publishAnnouncement(org, message.draftId, write)
    message.open = false
    showNotice(`Объявление отправлено: ${write.recipientUserIds.length} ${plural(write.recipientUserIds.length, 'получатель', 'получателя', 'получателей')}`)
  } catch (e) {
    message.error = message.draftId
      ? `Черновик сохранён, но не опубликован: ${formErrorText(e)}`
      : formErrorText(e)
  } finally {
    message.saving = false
  }
}

// В коде бэкенда статус объявления — обязательный query-параметр PATCH, тело — полный AnnouncementWrite;
// announcementsApi.update query-параметры не передаёт, поэтому публикуем здесь
function publishAnnouncement(org, announcementId, write) {
  return api.patch(`/organizations/${org}/announcements/${announcementId}?status=PUBLISHED`, write)
}

function showNotice(text) {
  notice.value = text
  clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => { notice.value = '' }, 6000)
}

// ─────────── Жизненный цикл ───────────
function resetGroupState() {
  group.value = null
  athletes.value = []
  sectionName.value = ''
  Object.assign(schedule, { loading: false, error: '', items: [] })
  Object.assign(attendance, { loading: false, error: '', unavailable: false, summary: null, items: [] })
  Object.assign(events, { loading: false, error: '', items: [], loadedFor: null })
}

watch(groupId, (id, previous) => {
  // У /trainer/athletes/:id тоже есть id — при уходе со страницы группу не загружаем
  if (!id || id === previous || route.name !== 'trainer-group-card') return
  activeTab.value = TAB_ROSTER
  resetGroupState()
  load()
})

watch(activeTab, tab => {
  if (tab === TAB_EVENTS && group.value && events.loadedFor !== groupId.value && !events.loading) loadEvents()
})

onMounted(() => {
  load()
  loadTrainers()
  loadVenues()
})

onBeforeUnmount(() => {
  clearTimeout(enrollTimer)
  clearTimeout(noticeTimer)
})

// ─────────── Помощники ───────────
// Все страницы списка (size ≤ 100 по контракту), не больше maxPages запросов; first — первая страница целиком
async function fetchPages(request, params = {}, maxPages = 10) {
  const first = await request({ ...params, page: 0, size: 100 })
  const pages = Math.min(first.totalPages || 1, maxPages)
  const rest = await Promise.all(
    Array.from({ length: pages - 1 }, (_, i) => request({ ...params, page: i + 1, size: 100 }))
  )
  return { first, items: rest.reduce((all, page) => all.concat(page.items || []), first.items || []) }
}

async function fetchAll(request, params = {}, maxPages = 10) {
  return (await fetchPages(request, params, maxPages)).items
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

.tabs-row {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 16px;
}
.tabs { display: flex; gap: 12px; flex-wrap: wrap; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }

.actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.btn-outline {
  padding: 10px 16px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
}
.btn-dark {
  padding: 10px 16px; background: #102522;
  border: none; border-radius: 12px;
  font-size: 13px; font-weight: 700; color: white;
  cursor: pointer;
}
.notice { font-size: 13px; font-weight: 600; color: #2E8B57; }

.main-blocks {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 24px;
}
@media (max-width: 1024px) {
  .main-blocks { grid-template-columns: 1fr; }
}

.card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }

.athlete-list { display: flex; flex-direction: column; gap: 12px; }
.athlete-line {
  display: flex; align-items: center; gap: 16px;
  padding-bottom: 12px; border-bottom: 1px solid #E3EAE8;
}
.avatar {
  width: 40px; height: 40px; border-radius: 999px;
  display: flex; justify-content: center; align-items: center;
  font-size: 13px; font-weight: 700;
  flex-shrink: 0;
}
/* В узкой карточке имя ужимается, чтобы заметке хватило места */
.details { width: 220px; min-width: 140px; }
.name { font-size: 15px; font-weight: 700; color: #152421; }
.rank { font-size: 12px; color: #6D7D79; }
.link { cursor: pointer; }
.details.link:hover .name { text-decoration: underline; }

.attendance { width: 100px; flex-shrink: 0; }
.attendance .label {
  font-size: 11px; font-weight: 700; color: #98A6A2;
  text-transform: uppercase;
}
.attendance .value { font-size: 14px; font-weight: 700; color: #2E8B57; }
.attendance .value.mid { color: #F2B705; }
.attendance .value.low { color: #D64545; }
.attendance .value.none { color: #98A6A2; }

.note { flex: 1; min-width: 110px; font-size: 13px; color: #6D7D79; }
.line-btn {
  width: 28px; height: 28px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer; flex-shrink: 0;
}
.line-btn:hover { border-color: #D64545; }

.sidebar-stats { display: flex; flex-direction: column; gap: 20px; }

.radial {
  display: flex; align-items: center; gap: 16px;
}
.circle {
  width: 72px; height: 72px;
  background: #E9F7D5; border-radius: 9999px;
  border: 6px solid #B7F34B;
  flex-shrink: 0;
}
.percent { font-size: 22px; font-weight: 700; color: #152421; }
.desc { font-size: 13px; color: #6D7D79; }

.training-item {
  padding: 12px; background: #F4F7F8;
  border-radius: 12px; display: flex;
  flex-direction: column; gap: 4px;
}
.training-item .date { font-size: 14px; font-weight: 700; color: #152421; }
.training-item .info { font-size: 13px; color: #6D7D79; }
.item-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.link-btn {
  background: none; padding: 0; text-align: left;
  font-size: 13px; font-weight: 600; color: #35678E;
  cursor: pointer;
}

.status-badge { padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; white-space: nowrap; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF8E6; color: #F2B705; }
.status-red { background: #FCE2E5; color: #D64545; }
.status-blue { background: #DDECFB; color: #35678E; }
.status-gray { background: #EEF1F0; color: #888888; }

.summary-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.summary-item {
  padding: 12px; background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; gap: 4px;
}
.summary-label { font-size: 11px; font-weight: 700; color: #98A6A2; text-transform: uppercase; }
.summary-value { font-size: 18px; font-weight: 700; color: #152421; }

/* Формы в модальных окнах — в стиле BaseInput */
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
.check-list {
  display: flex; flex-direction: column; gap: 8px;
  max-height: 180px; overflow-y: auto;
  padding: 10px 12px; border: 1px solid var(--color-gray-border); border-radius: 8px;
}
.check-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #152421; cursor: pointer; }
.pick-list {
  display: flex; flex-direction: column; gap: 6px;
  max-height: 240px; overflow-y: auto;
}
.pick-item {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
  padding: 10px 12px; background: white; text-align: left;
  border: 1px solid #E3EAE8; border-radius: 10px;
  font-size: 14px; color: #152421; cursor: pointer;
}
.pick-item.selected { border-color: #102522; background: #F4F7F8; font-weight: 600; }
.pick-meta { font-size: 12px; color: #98A6A2; }
.modal-text { font-size: 14px; color: #152421; line-height: 1.5; }
.form-hint { font-size: 13px; color: #6D7D79; }
.form-error { font-size: 13px; color: #D64545; }
</style>
