<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader
        title="Мероприятия и события"
        subtitle="Ваши спортивные сборы, соревнования и другие мероприятия"
        :show-search="false"
      >
        <template #actions>
          <div v-if="groupChip" class="coach-card">
            <span>{{ groupChip }}</span>
            <span class="online-dot"></span>
          </div>
        </template>
      </PageHeader>

      <StateBlock v-if="pageState" :kind="pageState.kind" :message="pageState.message" />
      <StateBlock v-else-if="loading" kind="loading" />
      <StateBlock v-else-if="error" kind="error" :message="errorText(error)" />
      <StateBlock v-else-if="!cards.length" kind="empty" message="Вас пока не пригласили ни на одно мероприятие" />

      <div v-else class="events-grid">
        <div
          v-for="event in cards"
          :key="event.id"
          class="event-card"
        >
          <div class="event-header">
            <div class="event-meta">
              <span
                class="event-badge"
                :style="{ background: event.badgeBg, color: event.badgeColor }"
              >
                {{ event.badge }}
              </span>
              <span class="event-date">{{ event.date }}</span>
            </div>
            <div v-if="event.canRespond" class="event-buttons">
              <button
                class="event-action"
                :class="{ confirmed: event.response === 'ACCEPTED' }"
                :disabled="savingId === event.id"
                :title="event.response === 'ACCEPTED' ? 'Нажмите, чтобы отказаться от участия' : ''"
                @click="toggleConfirm(event)"
              >
                {{ savingId === event.id ? 'Сохранение…' : event.response === 'ACCEPTED' ? '✓ Участие подтверждено' : 'Подтвердить участие' }}
              </button>
              <button
                v-if="event.response === 'PENDING'"
                class="event-action event-action--secondary"
                :disabled="savingId === event.id"
                @click="respond(event, 'DECLINED')"
              >
                Не смогу
              </button>
            </div>
          </div>

          <h3 class="event-title">{{ event.title }}</h3>
          <p v-if="event.description" class="event-desc">{{ event.description }}</p>
          <p v-if="actionErrors[event.id]" class="event-error">{{ actionErrors[event.id] }}</p>
          <p v-if="event.participantError" class="event-error">Не удалось загрузить ваш ответ: {{ event.participantError }}</p>

          <div class="event-divider"></div>

          <div class="event-facts">
            <div v-for="fact in event.facts" :key="fact.text" class="event-location">
              <BaseIcon :name="fact.icon" :size="16" color="#6D7D79" />
              <span>{{ fact.text }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { eventsApi } from '../../api/events'
import { groupsApi } from '../../api/groups'
import { myAthletes, selectedAthleteId, loadMyAthletes, getOrganizationId } from '../../utils/session'
import { formatDate, formatTime, formatDateTime, formatMoney, parseDate, addDays, errorText } from '../../utils/format'
import { label } from '../../utils/labels'

const NO_CARD = 'Карточка спортсмена ещё не создана тренером'
const BADGES = {
  CANCELLED: { text: 'Мероприятие отменено', bg: '#FCE2E5', color: '#D64545' },
  COMPLETED: { text: 'Мероприятие завершено', bg: '#EEF1F0', color: '#6D7D79' },
  ACCEPTED: { text: 'Вы участвуете', bg: '#E9F7D5', color: '#2D5B24' },
  DECLINED: { text: 'Вы отказались', bg: '#FCE2E5', color: '#D64545' },
  PENDING: { text: 'Вы приглашены', bg: '#FFF1D6', color: '#A36A16' }
}

// ─────────── Своя карточка ───────────
const athletesReady = ref(false)
const athletesError = ref(null)

const pageState = computed(() => {
  if (!athletesReady.value) return { kind: 'loading', message: '' }
  if (athletesError.value) return { kind: 'error', message: errorText(athletesError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CARD }
  return null
})

// ─────────── Мероприятия и свой ответ ───────────
const rows = ref([]) // { event, participant, participantError }
const loading = ref(false)
const error = ref(null)
const savingId = ref(null)
const actionErrors = ref({})
const groups = ref([])

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  error.value = null
  actionErrors.value = {}
  if (!athleteId) {
    rows.value = []
    groups.value = []
    loading.value = false
    return
  }
  loading.value = true
  loadGroups(athleteId)
  try {
    const org = getOrganizationId()
    // GET /events: спортсмену сервер отдаёт мероприятия, где участвует его карточка.
    // Черновики участникам не показываются (опубликованные, отменённые и завершённые — да).
    const list = await fetchAll(p => eventsApi.list(org, p), { athleteId })
    const visible = list.filter(e => e.status !== 'DRAFT')
    const withResponse = await Promise.all(visible.map(e => participantRow(org, e, athleteId)))
    if (seq !== loadSeq) return
    rows.value = withResponse.filter(r => r.participant || r.participantError)
  } catch (e) {
    if (seq !== loadSeq) return
    rows.value = []
    error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// GET /events/{id}/participants → EventParticipant[]; своя строка — по athleteId
async function participantRow(org, event, athleteId) {
  try {
    const res = await eventsApi.participants(org, event.id)
    const people = Array.isArray(res) ? res : (res?.items || [])
    return { event, participant: people.find(p => p.athleteId === athleteId) || null, participantError: null }
  } catch (e) {
    return { event, participant: null, participantError: e }
  }
}

async function loadGroups(athleteId) {
  try {
    const list = await fetchAll(p => groupsApi.list(getOrganizationId(), p), { athleteId, status: 'ACTIVE' })
    if (athleteId === selectedAthleteId.value) groups.value = list
  } catch (_) {
    groups.value = []
  }
}

onMounted(async () => {
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (e) {
    athletesError.value = e
  } finally {
    athletesReady.value = true
  }
  if (!athletesError.value) load()
})

watch(selectedAthleteId, () => {
  if (athletesReady.value && !athletesError.value) load()
})

const groupChip = computed(() => {
  const names = groups.value.map(g => g.name).filter(Boolean)
  if (!names.length) return ''
  if (names.length === 1) return `Группа: ${names[0]}`
  return names.length === 2 ? `Группы: ${names.join(', ')}` : `Группы: ${names[0]} и ещё ${names.length - 1}`
})

// ─────────── Карточки: сначала предстоящие (по дате), затем прошедшие, отменённые и завершённые ───────────
const cards = computed(() => {
  const now = new Date()
  const items = rows.value.map(row => ({ row, start: eventStart(row.event), end: eventEnd(row.event) }))
  const upcoming = x => x.row.event.status === 'PUBLISHED' && x.end >= now
  return [
    ...items.filter(upcoming).sort((a, b) => a.start - b.start),
    ...items.filter(x => !upcoming(x)).sort((a, b) => b.start - a.start)
  ].map(x => toCard(x.row))
})

function toCard({ event: e, participant: p, participantError }) {
  const response = p?.response || null
  const deadline = e.responseDeadline ? new Date(e.responseDeadline) : null
  const open = e.status === 'PUBLISHED' && (!deadline || new Date() < deadline)
  const badge = BADGES[e.status] || BADGES[response] || { text: label('eventStatus', e.status), bg: '#EEF1F0', color: '#6D7D79' }
  const facts = []
  if (e.type === 'FUNDRAISER') {
    if (e.collectionDueOn) facts.push({ icon: 'calendar', text: `Срок сбора: ${formatDate(e.collectionDueOn)}` })
  } else {
    facts.push({ icon: 'map-pin', text: e.location || 'Место уточняется' })
  }
  if (e.costPerAthlete) facts.push({ icon: 'credit-card', text: `Взнос: ${formatMoney(e.costPerAthlete)}` })
  if (deadline && e.status === 'PUBLISHED') {
    facts.push({ icon: 'clock', text: `${open ? 'Ответить до' : 'Приём ответов закрыт'} ${formatDateTime(deadline)}` })
  }
  if (e.requiredDocumentTypes?.length) {
    facts.push({ icon: 'file-text', text: `Документы: ${e.requiredDocumentTypes.map(t => label('documentType', t)).join(', ')}` })
  }
  if (p?.respondedAt && response !== 'PENDING') {
    facts.push({ icon: 'check', text: `Ответ отправлен ${formatDateTime(p.respondedAt)}` })
  }
  return {
    id: e.id,
    title: e.title,
    description: e.description || '',
    date: `${label('eventType', e.type)} · ${dateText(e)}`,
    badge: badge.text,
    badgeBg: badge.bg,
    badgeColor: badge.color,
    response,
    canRespond: open && !!p,
    participantError: participantError ? errorText(participantError) : '',
    facts
  }
}

function eventStart(e) {
  if (e.startsAt) return new Date(e.startsAt)
  if (e.collectionDueOn) return parseDate(e.collectionDueOn)
  return new Date(e.createdAt)
}

function eventEnd(e) {
  if (e.endsAt) return new Date(e.endsAt)
  if (e.startsAt) return new Date(e.startsAt)
  if (e.collectionDueOn) return addDays(parseDate(e.collectionDueOn), 1) // срок сбора — до конца дня
  return new Date(e.createdAt)
}

// «15 октября — 25 октября 2026», «12 сентября 2026, 12:00», «до 20 октября 2026»
function dateText(e) {
  if (!e.startsAt) return e.collectionDueOn ? `до ${formatDate(e.collectionDueOn)}` : '—'
  const start = new Date(e.startsAt)
  const end = e.endsAt ? new Date(e.endsAt) : null
  if (end && formatDate(start) !== formatDate(end)) {
    return `${formatDate(start, { withYear: start.getFullYear() !== end.getFullYear() })} — ${formatDate(end)}`
  }
  return `${formatDate(start)}, ${formatTime(start)}`
}

// ─────────── Ответ на участие: PATCH /events/{id}/participants/{athleteId} { response } ───────────
async function toggleConfirm(card) {
  if (card.response === 'ACCEPTED') {
    if (!confirm(`Отказаться от участия в «${card.title}»? Тренер увидит ваш ответ.`)) return
    await respond(card, 'DECLINED')
  } else {
    await respond(card, 'ACCEPTED')
  }
}

async function respond(card, response) {
  if (savingId.value) return
  const athleteId = selectedAthleteId.value
  savingId.value = card.id
  actionErrors.value = { ...actionErrors.value, [card.id]: '' }
  try {
    const org = getOrganizationId()
    await eventsApi.updateParticipant(org, card.id, athleteId, { response })
    // Свежая строка участника с сервера (время ответа); если не загрузилась — ответ уже сохранён
    const row = rows.value.find(r => r.event.id === card.id)
    if (!row) return
    const fresh = await participantRow(org, row.event, athleteId)
    const participant = fresh.participant || { ...row.participant, response, respondedAt: new Date().toISOString() }
    rows.value = rows.value.map(r => (r.event.id === card.id ? { ...r, participant, participantError: null } : r))
  } catch (e) {
    actionErrors.value = { ...actionErrors.value, [card.id]: errorText(e) }
  } finally {
    savingId.value = null
  }
}

// Все страницы списка Page<T> (size ≤ 100), не больше maxPages запросов
async function fetchAll(request, params = {}, maxPages = 10) {
  const first = await request({ ...params, page: 0, size: 100 })
  const pages = Math.min(first?.totalPages || 1, maxPages)
  const rest = await Promise.all(
    Array.from({ length: pages - 1 }, (_, i) => request({ ...params, page: i + 1, size: 100 }))
  )
  return rest.reduce((all, page) => all.concat(page?.items || []), first?.items || [])
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.coach-card {
  height: 44px; padding: 0 16px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #6D7D79;
}
.online-dot { width: 6px; height: 6px; background: #B7F34B; border-radius: 50%; }

.events-grid {
  display: flex; flex-direction: column; gap: 20px;
}

.event-card {
  padding: 24px; background: white;
  border: 1px solid #E3EAE8; border-radius: 16px;
  display: flex; flex-direction: column; gap: 16px;
}

.event-header {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
  flex-wrap: wrap;
}
.event-meta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.event-badge {
  padding: 4px 12px;
  font-size: 12px; font-weight: 700;
  border-radius: 999px;
}
.event-date { font-size: 14px; font-weight: 600; color: #6D7D79; }

.event-buttons { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.event-action {
  padding: 8px 16px;
  background: #B7F34B; color: #102522;
  border: none; border-radius: 8px;
  font-size: 13px; font-weight: 700;
  cursor: pointer; transition: opacity 0.2s;
}
.event-action:hover { opacity: 0.9; }
.event-action:disabled { opacity: 0.6; cursor: default; }
.event-action.confirmed {
  background: #E9F7D5; color: #2D5B24;
}
.event-action--secondary {
  background: white; color: #6D7D79;
  border: 1px solid #E3EAE8;
}

.event-title {
  font-size: 20px; font-weight: 700; color: #152421;
  margin: 0;
}
.event-desc {
  font-size: 14px; color: #6D7D79; line-height: 1.5;
  margin: 0;
  white-space: pre-line;
}
.event-error { font-size: 13px; color: #D64545; margin: 0; }
.event-divider { height: 1px; background: #E3EAE8; }
.event-facts { display: flex; flex-wrap: wrap; gap: 8px 24px; }
.event-location {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: #6D7D79;
}
</style>
