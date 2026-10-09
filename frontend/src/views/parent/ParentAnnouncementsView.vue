<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        title="Важные объявления"
        subtitle="Информационная лента новостей, изменений и регламентов"
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
        </template>
      </PageHeader>

      <div class="filters">
        <button
          v-for="f in filters"
          :key="f.id"
          class="filter-btn"
          :class="{ active: activeFilter === f.id }"
          @click="activeFilter = f.id"
        >
          {{ f.label }}
        </button>
      </div>

      <div class="feed">
        <div v-if="feedState" class="announce-card">
          <StateBlock :kind="feedState.kind" :message="feedState.message" />
        </div>

        <template v-else>
          <div
            v-for="a in cards"
            :key="a.id"
            :ref="el => trackCard(el, a)"
            class="announce-card"
            :class="{ important: a.pending }"
          >
            <div class="announce-header">
              <div class="author">
                <span class="author-name">{{ a.author }}</span>
                <span class="author-date">{{ a.date }}</span>
              </div>
              <div class="badges">
                <span v-if="a.isNew" class="badge-new">Новое</span>
                <span v-if="a.archived" class="badge-status status-gray">В архиве</span>
                <span v-if="a.pending" class="badge-confirm">Требуется согласие</span>
                <span v-else-if="a.myResponse" class="badge-status" :class="`status-${tone(a.myResponse)}`">
                  {{ label('announcementResponse', a.myResponse) }}
                </span>
              </div>
            </div>

            <div class="announce-body">
              <h3>{{ a.title }}</h3>
              <p>{{ a.text }}</p>
              <p v-if="a.attachments" class="attachments-note">
                Вложений: {{ a.attachments }} — скачивание файлов в кабинете родителя появится позже.
              </p>
            </div>

            <div v-if="a.requiresResponse" class="confirm-area">
              <span>{{ a.prompt }}</span>
              <div v-if="a.canRespond" class="confirm-actions">
                <button v-if="a.myResponse !== 'ACCEPTED'" class="btn-confirm" :disabled="!!busyId" @click="accept(a)">
                  {{ acceptingId === a.id ? 'Отправка…' : 'Я согласен' }}
                </button>
                <button v-if="a.myResponse !== 'DECLINED'" class="btn-decline" :disabled="!!busyId" @click="openDecline(a)">
                  Не согласен
                </button>
              </div>
            </div>
            <p v-if="cardErrors[a.id]" class="card-error" role="alert">{{ cardErrors[a.id] }}</p>
          </div>
        </template>

        <button v-if="hasMore" class="filter-btn load-more" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? 'Загрузка…' : 'Показать ещё' }}
        </button>
        <p v-if="moreError" class="card-error" role="alert">{{ moreError }}</p>
      </div>

      <BaseModal
        v-model="declineOpen"
        title="Ответ «Не согласен»"
        :submit-label="declining ? 'Отправка…' : 'Отправить ответ'"
        @submit="submitDecline"
      >
        <p class="modal-text">
          «{{ declineTarget?.title }}»: тренер увидит, что вы не согласны.{{ declineTarget?.deadlineText ? ` Изменить ответ можно${declineTarget.deadlineText}.` : '' }}
        </p>
        <div class="form-field">
          <label class="form-label" for="announcement-comment">Комментарий</label>
          <textarea
            id="announcement-comment"
            v-model="declineComment"
            class="form-textarea"
            rows="3"
            placeholder="Необязательно: например, причина"
          ></textarea>
        </div>
        <p v-if="declineError" class="form-error" role="alert">{{ declineError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { announcementsApi } from '../../api/announcements'
import {
  currentOrganization, myAthletes, selectedAthleteId, selectedAthlete, selectAthlete, getOrganizationId
} from '../../utils/session'
import { parseDate, formatDate, formatTime, formatDateTime, toIsoDate, addDays, fullName, initials, errorText } from '../../utils/format'
import { label, tone } from '../../utils/labels'

const PAGE_SIZE = 20

// Лента адресована родителю и от выбранного ребёнка не зависит. Получатель видит опубликованные
// объявления и архив (6.12, №047); фильтры — параметры GET /announcements
const QUERIES = {
  all: { status: 'PUBLISHED' },
  unread: { status: 'PUBLISHED', unread: true },
  confirm: { status: 'PUBLISHED', requiresResponse: true },
  archive: { status: 'ARCHIVED' }
}
const EMPTY = {
  all: 'Объявлений пока нет',
  unread: 'Все объявления прочитаны',
  confirm: 'Нет объявлений, где нужен ваш ответ',
  archive: 'В архиве пока пусто',
  category: 'В этой категории объявлений нет'
}

const activeFilter = ref('all')
// Категории — из загруженной ленты «Все»; фильтра по категории у API нет, поэтому он локальный
const categories = ref([])

const filters = computed(() => [
  { id: 'all', label: 'Все' },
  { id: 'unread', label: 'Непрочитанные' },
  { id: 'confirm', label: 'Важные подтверждения' },
  ...categories.value.map(c => ({ id: `cat:${c}`, label: c })),
  { id: 'archive', label: 'Архив' }
])

const isCategory = id => id.startsWith('cat:')
const queryKey = computed(() => (isCategory(activeFilter.value) ? 'all' : activeFilter.value))

// ─────────── загрузка ───────────

const items = ref([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const loadingMore = ref(false)
const error = ref(null)
const moreError = ref('')
const newIds = reactive(new Set()) // непрочитанные на момент загрузки: «Новое» держится до ухода со страницы

const hasMore = computed(() => !loading.value && !error.value && page.value + 1 < totalPages.value)

function remember(list, key) {
  list.forEach(a => { if (!a.readAt) newIds.add(a.id) })
  if (key !== 'all') return
  const found = new Set(categories.value)
  for (const a of list) {
    const category = a.category?.trim()
    if (category) found.add(category)
  }
  if (found.size > categories.value.length) categories.value = [...found]
}

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const key = queryKey.value
  resetObserver()
  loading.value = true
  loadingMore.value = false
  error.value = null
  moreError.value = ''
  items.value = []
  page.value = 0
  totalPages.value = 0
  try {
    const res = await announcementsApi.list(getOrganizationId(), { ...QUERIES[key], page: 0, size: PAGE_SIZE })
    if (seq !== loadSeq) return
    items.value = res?.items || []
    totalPages.value = res?.totalPages || 0
    remember(items.value, key)
  } catch (e) {
    if (seq === loadSeq) error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  const seq = loadSeq
  const key = queryKey.value
  const next = page.value + 1
  loadingMore.value = true
  moreError.value = ''
  try {
    const res = await announcementsApi.list(getOrganizationId(), { ...QUERIES[key], page: next, size: PAGE_SIZE })
    if (seq !== loadSeq) return
    // Пока листали, могли выйти новые объявления и сдвинуть страницы: повторы не показываем
    const known = new Set(items.value.map(a => a.id))
    const fresh = (res?.items || []).filter(a => !known.has(a.id))
    items.value = [...items.value, ...fresh]
    page.value = next
    totalPages.value = res?.totalPages || 0
    remember(fresh, key)
  } catch (e) {
    if (seq === loadSeq) moreError.value = errorText(e)
  } finally {
    if (seq === loadSeq) loadingMore.value = false
  }
}

onMounted(load)
watch(queryKey, load)

const feedState = computed(() => {
  if (loading.value) return { kind: 'loading', message: '' }
  if (error.value) return { kind: 'error', message: errorText(error.value) }
  if (!cards.value.length) {
    return { kind: 'empty', message: EMPTY[isCategory(activeFilter.value) ? 'category' : queryKey.value] }
  }
  return null
})

// ─────────── карточки ───────────

const startOfDay = d => new Date(d.getFullYear(), d.getMonth(), d.getDate())

// «Сегодня, 10:12» / «Вчера, 18:30» / «12 сентября 2026»
function relativeDateTime(value) {
  const d = parseDate(value)
  if (!d || isNaN(d)) return ''
  const today = startOfDay(new Date())
  if (toIsoDate(d) === toIsoDate(today)) return `Сегодня, ${formatTime(d)}`
  if (toIsoDate(d) === toIsoDate(addDays(today, -1))) return `Вчера, ${formatTime(d)}`
  return formatDate(d)
}

function promptOf(a, canRespond, deadline) {
  const answer = a.myResponse ? `Ваш ответ: «${label('announcementResponse', a.myResponse)}».` : ''
  if (canRespond) {
    const until = deadline ? ` до ${formatDateTime(deadline)}` : ''
    return a.myResponse
      ? `${answer}${deadline ? ` Изменить его можно${until}.` : ''}`
      : `Вы ознакомились с объявлением и согласны с условиями?${deadline ? ` Ответьте${until}.` : ''}`
  }
  if (answer) return answer
  return a.status === 'ARCHIVED'
    ? 'Объявление в архиве — ответ больше не принимается.'
    : `Срок ответа истёк${deadline ? ` ${formatDateTime(deadline)}` : ''}.`
}

const visibleItems = computed(() => {
  const f = activeFilter.value
  return isCategory(f) ? items.value.filter(a => a.category?.trim() === f.slice(4)) : items.value
})

const cards = computed(() => {
  const now = new Date()
  const organizationName = currentOrganization.value?.organizationName || 'Секция'
  return visibleItems.value.map(a => {
    const deadline = a.responseDeadline ? new Date(a.responseDeadline) : null
    // Ответ принимается по опубликованному объявлению с запросом согласия до срока (6.12, №053)
    const canRespond = a.requiresResponse && a.status === 'PUBLISHED' && (!deadline || now < deadline)
    return {
      id: a.id,
      title: a.title,
      text: a.text,
      author: a.category?.trim() || organizationName,
      date: relativeDateTime(a.publishedAt || a.createdAt),
      isNew: newIds.has(a.id),
      unread: !a.readAt,
      archived: a.status === 'ARCHIVED',
      requiresResponse: a.requiresResponse,
      myResponse: a.myResponse,
      canRespond,
      pending: canRespond && !a.myResponse,
      prompt: a.requiresResponse ? promptOf(a, canRespond, deadline) : '',
      deadlineText: deadline ? ` до ${formatDateTime(deadline)}` : '',
      attachments: (a.attachmentFileIds || []).length
    }
  })
})

// ─────────── прочтение (PUT /announcements/{id}/read) ───────────
// Объявление считается прочитанным, когда его карточка заняла хотя бы половину себя или половину экрана.

const readRequested = new Set()
let observer = null
let observed = new WeakMap() // элемент карточки → id объявления

function trackCard(el, card) {
  if (!el || !card.unread || readRequested.has(card.id) || observed.has(el)) return
  if (typeof IntersectionObserver === 'undefined') {
    markRead(card.id)
    return
  }
  if (!observer) observer = new IntersectionObserver(onIntersect, { threshold: [0, 0.25, 0.5] })
  observed.set(el, card.id)
  observer.observe(el)
}

function onIntersect(list) {
  for (const entry of list) {
    if (!entry.isIntersecting) continue
    const viewport = entry.rootBounds?.height || window.innerHeight
    if (entry.intersectionRatio < 0.5 && entry.intersectionRect.height < viewport / 2) continue
    observer.unobserve(entry.target)
    markRead(observed.get(entry.target))
  }
}

function resetObserver() {
  observer?.disconnect()
  observed = new WeakMap()
}

async function markRead(id) {
  if (!id || readRequested.has(id)) return
  readRequested.add(id)
  try {
    await announcementsApi.markRead(getOrganizationId(), id)
    const a = items.value.find(x => x.id === id)
    if (a && !a.readAt) a.readAt = new Date().toISOString()
  } catch (_) {
    readRequested.delete(id) // не критично: отметим при следующей загрузке ленты
  }
}

onBeforeUnmount(() => observer?.disconnect())

// ─────────── ответ на запрос согласия (POST /announcements/{id}/responses) ───────────

const busyId = ref(null)
const acceptingId = ref(null)
const cardErrors = reactive({})

async function sendResponse(id, response, comment) {
  const org = getOrganizationId()
  await announcementsApi.respond(org, id, response, comment)
  // Актуальный ответ — в карточке объявления (myResponse — последний из истории)
  const fresh = await announcementsApi.get(org, id).catch(() => null)
  const i = items.value.findIndex(a => a.id === id)
  if (i >= 0) items.value.splice(i, 1, fresh || { ...items.value[i], myResponse: response })
}

async function accept(card) {
  if (busyId.value) return
  busyId.value = card.id
  acceptingId.value = card.id
  cardErrors[card.id] = ''
  try {
    await sendResponse(card.id, 'ACCEPTED', null)
  } catch (e) {
    cardErrors[card.id] = errorText(e)
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

function openDecline(card) {
  if (busyId.value) return
  declineTarget.value = card
  declineComment.value = ''
  declineError.value = ''
  declineOpen.value = true
}

async function submitDecline() {
  if (declining.value || !declineTarget.value) return
  const id = declineTarget.value.id
  declining.value = true
  busyId.value = id
  declineError.value = ''
  cardErrors[id] = ''
  try {
    await sendResponse(id, 'DECLINED', declineComment.value.trim() || null)
    declineOpen.value = false
  } catch (e) {
    declineError.value = errorText(e)
  } finally {
    declining.value = false
    busyId.value = null
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

.filters { display: flex; gap: 12px; flex-wrap: wrap; }
.filter-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.filter-btn.active { background: #102522; color: white; border-color: #102522; }
.load-more { align-self: center; }
.load-more:disabled { opacity: 0.6; cursor: default; }

.feed { display: flex; flex-direction: column; gap: 16px; }
.announce-card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  outline: 1px solid #E3EAE8; outline-offset: -1px;
  display: flex; flex-direction: column; gap: 16px;
}
.announce-card.important {
  outline: 2px solid #B7F34B; outline-offset: -2px;
}

.announce-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.author { display: flex; gap: 12px; align-items: center; }
.author-name { font-size: 13px; font-weight: 700; color: #152421; }
.author-date { font-size: 12px; color: #98A6A2; }
.badges { display: flex; gap: 8px; }
.badge-new, .badge-confirm, .badge-status {
  padding: 4px 10px; border-radius: 99px;
  font-size: 11px; font-weight: 700;
}
.badge-new { background: #B7F34B; color: #102522; }
.badge-confirm { background: #FFF1D6; color: #8B6914; }
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF1D6; color: #8B6914; }
.status-red { background: #FCE2E5; color: #D64545; }
.status-blue { background: #DDECFB; color: #3B82F6; }
.status-gray { background: #F4F7F8; color: #888888; }

.announce-body h3 { font-size: 18px; font-weight: 700; color: #152421; margin-bottom: 8px; }
.announce-body p { font-size: 13px; color: #6D7D79; line-height: 1.6; white-space: pre-line; }
.announce-body .attachments-note { margin-top: 8px; font-size: 12px; color: #98A6A2; }

.confirm-area {
  padding: 16px; background: #F4F7F8;
  border-radius: 12px;
  display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap;
}
.confirm-area span { font-size: 13px; font-weight: 500; color: #152421; }
.confirm-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.btn-confirm {
  padding: 8px 16px; background: #102522;
  border: none; border-radius: 8px;
  font-size: 12px; font-weight: 700; color: white;
  cursor: pointer;
}
.btn-decline {
  padding: 8px 16px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 12px; font-weight: 700; color: #6D7D79;
  cursor: pointer;
}
.btn-confirm:disabled, .btn-decline:disabled { opacity: 0.6; cursor: not-allowed; }
.card-error { font-size: 13px; color: #D64545; }

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
