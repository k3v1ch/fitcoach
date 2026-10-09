<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        title="Сборы и поездки"
        subtitle="Сборы, соревнования и другие мероприятия ребёнка"
        :show-search="false"
      >
        <template #actions>
          <div v-if="selectedAthlete" class="child-selector">
            <div class="child-avatar">{{ initials(fullName(selectedAthlete)) }}</div>
            <span>{{ fullName(selectedAthlete) }}</span>
            <template v-if="myAthletes.length > 1">
              <BaseIcon name="chevron-down" :size="14" />
              <select
                class="child-select-overlay"
                aria-label="Выбрать ребёнка"
                :value="selectedAthleteId"
                @change="selectAthlete($event.target.value)"
              >
                <option v-for="a in myAthletes" :key="a.id" :value="a.id">{{ fullName(a) }}</option>
              </select>
            </template>
          </div>
          <router-link to="/parent/announcements" class="icon-btn" aria-label="Объявления">
            <BaseIcon name="bell" :size="19" color="#152421" />
            <span v-if="hasUnread" class="dot"></span>
          </router-link>
        </template>
      </PageHeader>

      <div v-if="pageState" class="camp-card">
        <StateBlock :kind="pageState.kind" :message="pageState.message" />
      </div>

      <div v-else class="camps-list">
        <div v-for="camp in camps" :key="camp.id" class="camp-card">
          <div class="camp-header">
            <div>
              <h3>{{ camp.title }}</h3>
              <div class="camp-meta">
                <template v-for="(part, i) in camp.meta" :key="i">
                  <span v-if="i" class="dot">•</span>
                  <span>{{ part }}</span>
                </template>
              </div>
              <p v-if="camp.description" class="camp-description">{{ camp.description }}</p>
            </div>
            <div class="camp-price">
              <div v-if="camp.price" class="price">{{ camp.price }}</div>
              <span class="status-badge" :class="`status-${camp.statusTone}`">{{ camp.status }}</span>
            </div>
          </div>

          <div class="divider"></div>

          <div class="checklist">
            <div class="checklist-label">НЕОБХОДИМЫЕ ДОКУМЕНТЫ И ЧЕК-ЛИСТ:</div>
            <div v-for="item in camp.checklist" :key="item.key" class="checklist-item">
              <div class="checkbox" :class="{ checked: item.done }">
                <BaseIcon v-if="item.done" name="check" :size="12" color="#2E8B57" />
              </div>
              <span>{{ item.label }}</span>
              <button
                v-if="item.upload"
                class="btn-link"
                :disabled="!!busyId"
                @click="chooseFile(camp, item.type)"
              >
                {{ uploadingKey === `${camp.id}:${item.type}` ? 'Загрузка…' : 'Загрузить' }}
              </button>
            </div>
            <div v-if="!camp.checklist.length" class="checklist-empty">Документы и взносы для участия не требуются</div>
          </div>

          <div v-if="camp.canAccept || camp.canDecline || camp.payable || camp.note" class="camp-actions">
            <button v-if="camp.canAccept" class="btn-primary" :disabled="!!busyId" @click="accept(camp)">
              {{ acceptingId === camp.id ? 'Отправка…' : 'Подтвердить участие' }}
            </button>
            <button v-if="camp.canDecline" class="btn-outline" :disabled="!!busyId" @click="openDecline(camp)">Отказаться от участия</button>
            <button v-if="camp.payable" class="btn-outline" @click="router.push('/parent/payments')">Как оплатить взнос</button>
            <span v-if="camp.note" class="camp-note">{{ camp.note }}</span>
          </div>
          <p v-if="cardErrors[camp.id]" class="camp-error" role="alert">{{ cardErrors[camp.id] }}</p>
        </div>
      </div>

      <!-- Выбор файла для документа из чек-листа: PDF, PNG или JPEG до 10 МБ -->
      <input
        ref="fileInput"
        type="file"
        class="file-input"
        accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
        @change="onFileChosen"
      />

      <BaseModal
        v-model="declineOpen"
        title="Отказ от участия"
        :submit-label="declining ? 'Отправка…' : 'Отказаться'"
        @submit="submitDecline"
      >
        <p class="modal-text">
          «{{ declineTarget?.title }}»: тренер увидит, что {{ childName }} не участвует.{{ declineTarget?.deadlineText ? ` Изменить ответ можно${declineTarget.deadlineText}.` : '' }}
        </p>
        <div class="form-field">
          <label class="form-label" for="decline-comment">Комментарий для тренера</label>
          <textarea
            id="decline-comment"
            v-model="declineComment"
            class="form-textarea"
            rows="3"
            placeholder="Необязательно: например, причина отказа"
          ></textarea>
        </div>
        <p v-if="declineError" class="form-error" role="alert">{{ declineError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { eventsApi } from '../../api/events'
import { chargesApi } from '../../api/charges'
import { documentsApi } from '../../api/documents'
import { announcementsApi } from '../../api/announcements'
import {
  myAthletes, selectedAthleteId, selectedAthlete, loadMyAthletes, selectAthlete, getOrganizationId, hasPermission
} from '../../utils/session'
import {
  parseDate, formatDate, formatTime, formatDateTime, formatMoney, toIsoDate, fullName, initials, errorText
} from '../../utils/format'
import { label, tone } from '../../utils/labels'

const router = useRouter()

const NO_CHILD = 'Ребёнок ещё не привязан — обратитесь к тренеру'
const MAX_FILE_BYTES = 10 * 1024 * 1024 // сервер принимает PDF, PNG и JPEG не больше 10 МБ
// Ответ об участии глазами родителя
const RESPONSE_TEXT = { PENDING: 'Ждёт вашего ответа', ACCEPTED: 'Участие подтверждено', DECLINED: 'Участие отклонено' }

const childName = computed(() => selectedAthlete.value?.firstName || 'ребёнок')

// ─────────── загрузка ───────────

const childrenReady = ref(false)
const childrenError = ref(null)

const entries = ref([])   // { event, participant, charges } — мероприятия выбранного ребёнка
const documents = ref([]) // документы ребёнка — для чек-листа мероприятий
const loading = ref(false)
const error = ref(null)
const hasUnread = ref(false)

const asList = res => (Array.isArray(res) ? res : res?.items || [])

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

// Строка ребёнка в составе мероприятия и его действующие начисления по этому мероприятию.
// GET /events не применяет athleteId: родителю приходят мероприятия всех его детей, поэтому мероприятие
// остаётся в списке, только если выбранный ребёнок в составе участников или ему выставлен взнос.
async function loadEntry(org, event, athleteId) {
  const [people, charges] = await Promise.all([
    eventsApi.participants(org, event.id),
    hasPermission('charges.read')
      ? chargesApi.list(org, { eventId: event.id, athleteId, status: 'ACTIVE', size: 100 })
      : Promise.resolve(null)
  ])
  const participant = asList(people).find(p => p.athleteId === athleteId) || null
  const own = asList(charges).filter(c => c.athleteId === athleteId)
  return participant || own.length ? { event, participant, charges: own } : null
}

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  entries.value = []
  documents.value = []
  error.value = null
  Object.keys(cardErrors).forEach(k => delete cardErrors[k])
  if (!athleteId) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const org = getOrganizationId()
    const [events, docs] = await Promise.all([
      // Сборы, соревнования, медосмотры и денежные сборы, где участвуют дети родителя
      fetchAll(p => eventsApi.list(org, p)),
      hasPermission('documents.read')
        ? fetchAll(p => documentsApi.list(org, { athleteId, ...p }))
        : Promise.resolve([])
    ])
    // Черновик сервер тоже отдаёт, если ребёнок уже в составе, — родителю его не показываем
    const rows = await Promise.all(
      events.filter(e => e.status !== 'DRAFT').map(e => loadEntry(org, e, athleteId))
    )
    if (seq !== loadSeq) return
    entries.value = rows.filter(Boolean)
    documents.value = docs
  } catch (e) {
    if (seq === loadSeq) error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// Красная точка у колокольчика: есть непрочитанные объявления
async function loadUnread() {
  try {
    const res = await announcementsApi.list(getOrganizationId(), { status: 'PUBLISHED', unread: true, size: 1 })
    hasUnread.value = (res?.totalElements || 0) > 0
  } catch (_) {
    hasUnread.value = false // отметка в шапке не критична
  }
}

onMounted(async () => {
  loadUnread()
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (e) {
    childrenError.value = e
  } finally {
    childrenReady.value = true
  }
  if (!childrenError.value) load()
})

watch(selectedAthleteId, () => {
  if (childrenReady.value && !childrenError.value) load()
})

const pageState = computed(() => {
  if (!childrenReady.value) return { kind: 'loading', message: '' }
  if (childrenError.value) return { kind: 'error', message: errorText(childrenError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CHILD }
  if (loading.value) return { kind: 'loading', message: '' }
  if (error.value) return { kind: 'error', message: errorText(error.value) }
  if (!camps.value.length) {
    return { kind: 'empty', message: 'Ребёнок пока не участвует в сборах, соревнованиях и других мероприятиях' }
  }
  return null
})

// ─────────── карточки мероприятий ───────────

const startOf = e => (e.startsAt ? new Date(e.startsAt) : parseDate(e.collectionDueOn))

// Конец мероприятия; у денежного сбора — конец дня срока сбора
function endOf(e) {
  if (e.endsAt) return new Date(e.endsAt)
  if (!e.startsAt && e.collectionDueOn) {
    const d = parseDate(e.collectionDueOn)
    return new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1)
  }
  return startOf(e)
}

// Обычное мероприятие — даты начала и конца, денежный сбор — срок сбора
function eventDates(e) {
  if (!e.startsAt) return e.collectionDueOn ? `Сбор до ${formatDate(e.collectionDueOn)}` : ''
  const start = new Date(e.startsAt)
  const end = e.endsAt ? new Date(e.endsAt) : null
  if (!end || toIsoDate(start) === toIsoDate(end)) {
    return `${formatDate(start)}, ${formatTime(start)}${end ? ` – ${formatTime(end)}` : ''}`
  }
  return `${formatDate(start, { withYear: start.getFullYear() !== end.getFullYear() })} – ${formatDate(end)}`
}

// Взнос по мероприятию: действующие начисления ребёнка (суммы — в копейках, без ошибок округления)
function feeOf(charges) {
  if (!charges.length) return null
  const cents = key => charges.reduce((sum, c) => sum + Math.round(Number(c[key] || 0) * 100), 0)
  const remaining = cents('remainingAmount')
  return {
    amount: cents('amount') / 100,
    remaining: remaining / 100,
    dueOn: charges.filter(c => Number(c.remainingAmount) > 0).map(c => c.dueOn).sort()[0] || null,
    overdue: charges.some(c => c.overdue),
    paidUp: remaining <= 0
  }
}

// Документ нужного типа: прикреплён к участию или есть в карточке и действует на дату мероприятия
function documentState(type, participant, refDate) {
  const linked = new Set(participant?.documentIds || [])
  const ofType = documents.value.filter(d => d.type === type)
  if (ofType.some(d => linked.has(d.id) || !d.validUntil || d.validUntil >= refDate)) return 'ok'
  return ofType.length ? 'expired' : 'missing'
}

function checklistOf(e, participant, fee, active) {
  const items = []
  if (fee) {
    items.push({
      key: 'fee',
      done: fee.paidUp,
      label: fee.paidUp
        ? `Взнос ${formatMoney(fee.amount)} оплачен`
        : `Взнос ${formatMoney(fee.amount)}: осталось оплатить ${formatMoney(fee.remaining)}`
          + (fee.dueOn ? ` до ${formatDate(fee.dueOn)}` : '')
          + (fee.overdue ? ' — срок прошёл' : '')
    })
  } else if (e.costPerAthlete !== null && e.costPerAthlete !== undefined) {
    items.push({ key: 'fee', done: false, label: `Взнос ${formatMoney(e.costPerAthlete)} — тренер ещё не выставил начисление` })
  }
  const refDate = toIsoDate(startOf(e) || new Date())
  // Свои документы родитель загружает в карточку ребёнка (documents.read и связь с ребёнком)
  const canUpload = active && hasPermission('documents.read')
  for (const type of e.requiredDocumentTypes || []) {
    const state = documentState(type, participant, refDate)
    const name = label('documentType', type)
    items.push({
      key: `doc-${type}`,
      type,
      done: state === 'ok',
      label: state === 'ok'
        ? `${name} — есть в карточке ребёнка`
        : state === 'expired' ? `${name} — срок действия истёк, загрузите новый документ` : `${name} — нужно загрузить`,
      upload: state !== 'ok' && canUpload
    })
  }
  return items
}

function statusOf(e, participant) {
  if (e.status === 'CANCELLED') return { text: label('eventStatus', e.status), tone: 'red' }
  if (e.status === 'COMPLETED') return { text: label('eventStatus', e.status), tone: 'gray' }
  if (!participant) return { text: 'Не в составе', tone: 'gray' }
  return {
    text: RESPONSE_TEXT[participant.response] || label('participantResponse', participant.response),
    tone: tone(participant.response)
  }
}

function noteOf(e, participant, deadline, answerable, active) {
  if (!participant) return active ? 'Ребёнок не в составе участников — по взносу обратитесь к тренеру' : ''
  if (!answerable) return active ? 'Срок ответа истёк' : ''
  const until = deadline ? ` до ${formatDateTime(deadline)}` : ''
  if (participant.response === 'PENDING') return deadline ? `Ответьте${until}` : 'Подтвердите участие или откажитесь'
  const when = participant.respondedAt ? ` ${formatDateTime(participant.respondedAt)}` : ''
  const comment = participant.comment ? ` · «${participant.comment}»` : ''
  return `Ответ отправлен${when}${comment}${deadline ? ` · изменить можно${until}` : ''}`
}

const camps = computed(() => {
  const now = new Date()
  const list = entries.value.map(({ event: e, participant, charges }) => {
    const deadline = e.responseDeadline ? new Date(e.responseDeadline) : null
    const active = e.status === 'PUBLISHED' && endOf(e) >= now
    // Ответ принимается по опубликованному мероприятию до срока ответа (6.11, №046)
    const answerable = !!participant && active && (!deadline || now < deadline)
    const fee = feeOf(charges)
    const status = statusOf(e, participant)
    return {
      id: e.id,
      title: e.title,
      meta: [label('eventType', e.type), eventDates(e), e.location].filter(Boolean),
      description: e.description || '',
      price: fee
        ? formatMoney(fee.amount)
        : e.costPerAthlete !== null && e.costPerAthlete !== undefined ? formatMoney(e.costPerAthlete) : '',
      status: status.text,
      statusTone: status.tone,
      checklist: checklistOf(e, participant, fee, active),
      canAccept: answerable && participant.response !== 'ACCEPTED',
      canDecline: answerable && participant.response !== 'DECLINED',
      payable: !!fee && !fee.paidUp,
      note: noteOf(e, participant, deadline, answerable, active),
      deadlineText: deadline ? ` до ${formatDateTime(deadline)}` : '',
      active,
      at: startOf(e)
    }
  })
  // Сначала актуальные — от ближайших, затем прошедшие, завершённые и отменённые — от новых к старым
  const time = c => (c.at && !isNaN(c.at) ? c.at.getTime() : 0)
  return [
    ...list.filter(c => c.active).sort((a, b) => time(a) - time(b)),
    ...list.filter(c => !c.active).sort((a, b) => time(b) - time(a))
  ]
})

// ─────────── ответ об участии (PATCH /events/{id}/participants/{athleteId}) ───────────

const busyId = ref(null)      // мероприятие, по которому идёт запрос: кнопки остальных тоже ждут
const acceptingId = ref(null)
const cardErrors = reactive({})

async function sendResponse(eventId, response, comment) {
  const org = getOrganizationId()
  const athleteId = selectedAthleteId.value
  await eventsApi.updateParticipant(org, eventId, athleteId, { response, comment })
  // Свежая строка участника: ответ, время ответа и комментарий
  const people = await eventsApi.participants(org, eventId).catch(() => null)
  const entry = entries.value.find(x => x.event.id === eventId)
  if (!entry || athleteId !== selectedAthleteId.value) return
  entry.participant = asList(people).find(p => p.athleteId === athleteId)
    || { ...entry.participant, response, comment, respondedAt: new Date().toISOString() }
}

async function accept(camp) {
  if (busyId.value) return
  busyId.value = camp.id
  acceptingId.value = camp.id
  cardErrors[camp.id] = ''
  try {
    await sendResponse(camp.id, 'ACCEPTED', null)
  } catch (e) {
    cardErrors[camp.id] = errorText(e)
  } finally {
    busyId.value = null
    acceptingId.value = null
  }
}

const declineOpen = ref(false)
const declineTarget = ref(null)
const declineComment = ref('')
const declineError = ref('')
const declining = ref(false)

function openDecline(camp) {
  if (busyId.value) return
  declineTarget.value = camp
  declineComment.value = ''
  declineError.value = ''
  declineOpen.value = true
}

async function submitDecline() {
  if (declining.value || !declineTarget.value) return
  const eventId = declineTarget.value.id
  declining.value = true
  busyId.value = eventId
  declineError.value = ''
  cardErrors[eventId] = ''
  try {
    await sendResponse(eventId, 'DECLINED', declineComment.value.trim() || null)
    declineOpen.value = false
  } catch (e) {
    declineError.value = errorText(e)
  } finally {
    declining.value = false
    busyId.value = null
  }
}

// ─────────── документ из чек-листа (POST /documents, multipart) ───────────

const fileInput = ref(null)
const uploadingKey = ref('')
let uploadTarget = null

function chooseFile(camp, type) {
  if (busyId.value || !fileInput.value) return
  uploadTarget = { eventId: camp.id, eventTitle: camp.title, type }
  cardErrors[camp.id] = ''
  fileInput.value.value = ''
  fileInput.value.click()
}

async function onFileChosen(event) {
  const file = event.target.files?.[0]
  const target = uploadTarget
  uploadTarget = null
  if (!file || !target) return
  if (!/\.(pdf|png|jpe?g)$/i.test(file.name)) {
    cardErrors[target.eventId] = 'Подойдёт файл PDF, PNG или JPEG.'
    return
  }
  if (file.size > MAX_FILE_BYTES) {
    cardErrors[target.eventId] = 'Файл больше 10 МБ — уменьшите его и загрузите снова.'
    return
  }
  const athleteId = selectedAthleteId.value
  busyId.value = target.eventId
  uploadingKey.value = `${target.eventId}:${target.type}`
  try {
    const doc = await documentsApi.create(getOrganizationId(), {
      file,
      type: target.type,
      athleteId,
      title: `${label('documentType', target.type)} — ${target.eventTitle}`.slice(0, 200)
    })
    if (athleteId === selectedAthleteId.value) documents.value = [doc, ...documents.value]
  } catch (e) {
    cardErrors[target.eventId] = errorText(e)
  } finally {
    busyId.value = null
    uploadingKey.value = ''
  }
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.child-selector {
  position: relative;
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 13px; font-weight: 600; color: #152421;
}
.child-avatar {
  width: 24px; height: 24px;
  background: #DDECFB; color: #35678E;
  border-radius: 99px;
  display: flex; justify-content: center; align-items: center;
  font-size: 10px; font-weight: 700;
}
/* Прозрачный нативный список поверх плашки ребёнка — переключение без смены вида */
.child-select-overlay {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  opacity: 0; cursor: pointer;
}

.camps-list { display: flex; flex-direction: column; gap: 20px; }
.camp-card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
}
.camp-header { display: flex; justify-content: space-between; gap: 24px; }
.camp-header h3 { font-size: 20px; font-weight: 700; color: #152421; margin-bottom: 6px; }
.camp-meta { display: flex; gap: 12px; flex-wrap: wrap; font-size: 13px; color: #6D7D79; }
.camp-meta .dot { color: #98A6A2; }
.camp-description { margin-top: 10px; font-size: 13px; color: #6D7D79; line-height: 1.5; white-space: pre-line; }

.camp-price { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
.price { font-size: 18px; font-weight: 800; color: #152421; white-space: nowrap; }
.status-badge { padding: 4px 12px; border-radius: 999px; font-size: 11px; font-weight: 700; white-space: nowrap; }
.status-red { background: #FCE2E5; color: #D64545; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF1D6; color: #8B6914; }
.status-blue { background: #DDECFB; color: #3B82F6; }
.status-gray { background: #F4F7F8; color: #888888; }

.divider { height: 1px; background: #E3EAE8; }

.icon-btn {
  position: relative;
  width: 44px; height: 44px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}
.icon-btn .dot {
  position: absolute; top: 11px; right: 10px;
  width: 8px; height: 8px;
  background: #D64545; border-radius: 50%;
}

.checklist { display: flex; flex-direction: column; gap: 12px; }
.checklist-label {
  font-size: 12px; font-weight: 700; color: #6D7D79;
  text-transform: uppercase;
}
.checklist-item {
  display: flex; align-items: center; gap: 12px;
  font-size: 13px; color: #152421;
}
.checkbox {
  width: 20px; height: 20px;
  border: 1.5px solid #98A6A2; border-radius: 6px;
  display: flex; justify-content: center; align-items: center;
  flex-shrink: 0;
}
.checkbox.checked { background: #E9F7D5; border-color: #2E8B57; }
.checklist-empty { font-size: 13px; color: #6D7D79; }
.btn-link {
  margin-left: auto; padding: 4px 0; background: none; border: none;
  font-size: 12px; font-weight: 700; color: #35678E; cursor: pointer; white-space: nowrap;
}
.btn-link:disabled { opacity: 0.5; cursor: not-allowed; }

.camp-actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.btn-primary {
  padding: 10px 16px; background: #B7F34B; border: none;
  border-radius: 8px; font-size: 13px; font-weight: 700;
  color: #102522; cursor: pointer;
}
.btn-outline {
  padding: 10px 16px; background: #F4F7F8; border: none;
  border-radius: 8px; font-size: 13px; font-weight: 600;
  color: #6D7D79; cursor: pointer;
}
.btn-primary:disabled, .btn-outline:disabled { opacity: 0.6; cursor: not-allowed; }
.camp-note { font-size: 12px; color: #6D7D79; line-height: 1.4; }
.camp-error { margin-top: -8px; font-size: 13px; color: #D64545; }

.file-input { display: none; }

.modal-text { font-size: 14px; color: #152421; line-height: 1.5; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.form-textarea {
  width: 100%; padding: 10px 12px; box-sizing: border-box;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 14px; font-family: inherit; color: #152421; resize: vertical; outline: none;
}
.form-textarea:focus { border-color: #B7F34B; }
.form-error { font-size: 13px; color: #D64545; }
</style>
