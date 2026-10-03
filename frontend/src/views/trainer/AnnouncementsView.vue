<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        v-model="search"
        title="Объявления"
        subtitle="Рассылка родителям и спортсменам: черновики, публикация, прочтения и ответы"
        search-placeholder="Поиск по заголовку..."
      >
        <template #actions>
          <BaseButton v-if="canWrite" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Новое объявление
          </BaseButton>
        </template>
      </PageHeader>

      <div class="filters">
        <button
          v-for="f in STATUS_FILTERS"
          :key="f.value"
          class="filter-btn"
          :class="{ active: statusFilter === f.value }"
          @click="statusFilter = f.value"
        >
          {{ f.label }}
        </button>
      </div>

      <StateBlock v-if="loading && !items.length" kind="loading" />
      <StateBlock v-else-if="loadError" kind="error" :message="loadError" />
      <StateBlock
        v-else-if="!items.length"
        kind="empty"
        :message="search || statusFilter ? 'Ничего не найдено' : 'Объявлений пока нет — создайте первое'"
      />

      <div v-else class="feed">
        <div
          v-for="a in items"
          :key="a.id"
          class="announce-card"
          :class="{ important: a.requiresResponse && a.status === 'PUBLISHED' }"
        >
          <div class="announce-header">
            <div class="author">
              <span class="badge-status" :class="`status-${tone(a.status)}`">{{ label('announcementStatus', a.status) }}</span>
              <span class="author-date">
                {{ a.publishedAt ? `Опубликовано ${formatDateTime(a.publishedAt)}` : `Создано ${formatDateTime(a.createdAt)}` }}
              </span>
              <span v-if="a.category" class="category">{{ a.category }}</span>
            </div>
            <div class="badges">
              <span v-if="a.requiresResponse" class="badge-confirm">
                Нужен ответ до {{ formatDateTime(a.responseDeadline) }}
              </span>
              <span v-if="a.attachmentFileIds?.length" class="badge-files">
                Вложений: {{ a.attachmentFileIds.length }}
              </span>
            </div>
          </div>

          <div class="announce-body">
            <h3>{{ a.title }}</h3>
            <p>{{ a.text }}</p>
          </div>

          <div class="card-actions">
            <button v-if="a.status !== 'DRAFT'" class="btn-secondary" @click="openAudience(a)">
              <BaseIcon name="users" :size="14" color="#152421" />
              Аудитория и ответы
            </button>
            <template v-if="canWrite && a.status === 'DRAFT'">
              <button class="btn-secondary" @click="openEdit(a)">
                <BaseIcon name="edit" :size="14" color="#152421" />
                Изменить
              </button>
              <button class="btn-primary" :disabled="busyId === a.id" @click="publish(a)">
                <BaseIcon name="send" :size="14" color="#FFFFFF" />
                Опубликовать
              </button>
            </template>
            <button
              v-if="canWrite && a.status !== 'ARCHIVED'"
              class="btn-ghost"
              :disabled="busyId === a.id"
              @click="archive(a)"
            >
              В архив
            </button>
          </div>
          <div v-if="actionError[a.id]" class="form-error">{{ actionError[a.id] }}</div>
        </div>

        <button v-if="page + 1 < totalPages" class="btn-more" :disabled="loading" @click="load(page + 1)">
          {{ loading ? 'Загрузка…' : 'Показать ещё' }}
        </button>
      </div>
    </main>

    <!-- Создание и изменение черновика -->
    <BaseModal
      v-model="editorOpen"
      :title="editing ? 'Черновик объявления' : 'Новое объявление'"
      :submit-label="saving ? 'Сохранение…' : (publishNow ? 'Опубликовать' : 'Сохранить черновик')"
      :width="680"
      @submit="save"
    >
      <BaseInput id="ann-title" v-model="form.title" label="Заголовок" placeholder="Например: соревнования в субботу" />
      <div class="field">
        <label for="ann-text" class="field-label">Текст</label>
        <textarea id="ann-text" v-model="form.text" class="field-input textarea" rows="5" placeholder="Что важно знать получателям"></textarea>
      </div>
      <BaseInput id="ann-category" v-model="form.category" label="Категория (необязательно)" placeholder="Сборы, расписание, документы…" />

      <div class="field">
        <div class="field-label">
          Получатели
          <span class="muted">выбрано {{ form.recipientUserIds.length }}</span>
        </div>
        <StateBlock v-if="membersLoading" kind="loading" message="Загрузка участников…" />
        <StateBlock v-else-if="membersError" kind="error" :message="membersError" />
        <template v-else>
          <div class="quick-select">
            <button type="button" class="chip" @click="selectRole('PARENT')">Все родители</button>
            <button type="button" class="chip" @click="selectRole('ATHLETE')">Все спортсмены</button>
            <button type="button" class="chip" @click="selectRole(null)">Все участники</button>
            <button type="button" class="chip" @click="form.recipientUserIds = []">Снять выбор</button>
          </div>
          <input v-model="memberSearch" class="field-input" placeholder="Найти по имени…" />
          <div class="recipients">
            <label v-for="m in visibleMembers" :key="m.userId" class="recipient">
              <input v-model="form.recipientUserIds" type="checkbox" :value="m.userId" />
              <span class="recipient-name">{{ m.fullName || 'Без имени' }}</span>
              <span class="recipient-role">{{ m.roles.map(r => label('role', r)).join(', ') }}</span>
            </label>
            <div v-if="!visibleMembers.length" class="muted">Нет участников — добавьте родителей и спортсменов в разделе «Управление».</div>
          </div>
        </template>
      </div>

      <label class="check-row">
        <input v-model="form.requiresResponse" type="checkbox" />
        Требуется ответ (согласие)
      </label>
      <div v-if="form.requiresResponse" class="field">
        <label for="ann-deadline" class="field-label">Ответить до</label>
        <input id="ann-deadline" v-model="form.responseDeadline" type="datetime-local" class="field-input" />
      </div>

      <label class="check-row">
        <input v-model="publishNow" type="checkbox" />
        Опубликовать сразу — получатели увидят объявление в своём кабинете
      </label>

      <div v-if="formError" class="form-error">{{ formError }}</div>
    </BaseModal>

    <!-- Аудитория: прочтения и ответы -->
    <BaseModal
      v-model="audienceOpen"
      :title="audienceFor ? audienceFor.title : 'Аудитория'"
      submit-label="Закрыть"
      :width="720"
      @submit="audienceOpen = false"
    >
      <StateBlock v-if="audienceLoading" kind="loading" />
      <StateBlock v-else-if="audienceError" kind="error" :message="audienceError" />
      <template v-else>
        <div class="audience-summary">
          <div class="summary-item"><b>{{ recipients.length }}</b><span>получателей</span></div>
          <div class="summary-item"><b>{{ readCount }}</b><span>прочитали</span></div>
          <template v-if="audienceFor?.requiresResponse">
            <div class="summary-item"><b>{{ answerCount.ACCEPTED }}</b><span>согласны</span></div>
            <div class="summary-item"><b>{{ answerCount.DECLINED }}</b><span>не согласны</span></div>
            <div class="summary-item"><b>{{ recipients.length - answerCount.ACCEPTED - answerCount.DECLINED }}</b><span>без ответа</span></div>
          </template>
        </div>

        <table class="audience-table">
          <thead>
            <tr><th>Получатель</th><th>Прочитано</th><th v-if="audienceFor?.requiresResponse">Ответ</th></tr>
          </thead>
          <tbody>
            <tr v-for="r in recipients" :key="r.userId">
              <td>{{ r.fullName || '—' }}</td>
              <td>{{ r.readAt ? formatDateTime(r.readAt) : 'Не прочитано' }}</td>
              <td v-if="audienceFor?.requiresResponse">
                <span v-if="r.response" class="badge-status" :class="`status-${tone(r.response)}`">
                  {{ label('announcementResponse', r.response) }}
                </span>
                <span v-else class="muted">Нет ответа</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="responses.length" class="history">
          <div class="field-label">История ответов</div>
          <div v-for="r in responses" :key="r.id" class="history-row">
            <span class="badge-status" :class="`status-${tone(r.response)}`">{{ label('announcementResponse', r.response) }}</span>
            <span class="history-name">{{ r.fullName }}</span>
            <span class="muted">{{ formatDateTime(r.respondedAt) }}</span>
            <span v-if="r.comment" class="history-comment">«{{ r.comment }}»</span>
          </div>
        </div>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { announcementsApi } from '../../api/announcements'
import { organizationsApi } from '../../api/organizations'
import { getOrganizationId, hasPermission } from '../../utils/session'
import { errorText, formatDateTime } from '../../utils/format'
import { label, tone } from '../../utils/labels'

const STATUS_FILTERS = [
  { value: '', label: 'Все' },
  { value: 'DRAFT', label: 'Черновики' },
  { value: 'PUBLISHED', label: 'Опубликованные' },
  { value: 'ARCHIVED', label: 'Архив' }
]
const PAGE_SIZE = 20

const canWrite = computed(() => hasPermission('announcements.write'))

// --- лента ---
const items = ref([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const loadError = ref('')
const search = ref('')
const statusFilter = ref('')
const busyId = ref(null)
const actionError = reactive({})

async function load(nextPage = 0) {
  loading.value = true
  loadError.value = ''
  try {
    const res = await announcementsApi.list(getOrganizationId(), {
      q: search.value.trim() || undefined,
      status: statusFilter.value || undefined,
      page: nextPage,
      size: PAGE_SIZE
    })
    items.value = nextPage === 0 ? res.items : [...items.value, ...res.items]
    page.value = res.page
    totalPages.value = res.totalPages
  } catch (e) {
    loadError.value = errorText(e)
  } finally {
    loading.value = false
  }
}

let searchTimer = null
watch(search, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(0), 300)
})
watch(statusFilter, () => load(0))
onMounted(() => load(0))

// Тело PATCH — полный AnnouncementWrite; после публикации сервер меняет только статус
function writeOf(a, recipientUserIds = []) {
  return {
    title: a.title,
    text: a.text,
    category: a.category || null,
    recipientUserIds,
    requiresResponse: a.requiresResponse,
    responseDeadline: a.responseDeadline || null,
    attachmentFileIds: a.attachmentFileIds || []
  }
}

async function allRecipients(announcementId) {
  const result = []
  for (let p = 0; p < 50; p++) {
    const res = await announcementsApi.recipients(getOrganizationId(), announcementId, { page: p, size: 100 })
    result.push(...res.items)
    if (p + 1 >= res.totalPages) break
  }
  return result
}

async function publish(a) {
  if (!confirm(`Опубликовать «${a.title}»? После публикации текст и получателей изменить нельзя.`)) return
  busyId.value = a.id
  actionError[a.id] = ''
  try {
    const ids = (await allRecipients(a.id)).map(r => r.userId)
    await announcementsApi.update(getOrganizationId(), a.id, writeOf(a, ids), 'PUBLISHED')
    await load(0)
  } catch (e) {
    actionError[a.id] = errorText(e)
  } finally {
    busyId.value = null
  }
}

async function archive(a) {
  if (!confirm(`Перенести «${a.title}» в архив? Получатели по-прежнему смогут его прочитать.`)) return
  busyId.value = a.id
  actionError[a.id] = ''
  try {
    // Черновик сервер сохраняет целиком — передаём текущих получателей; опубликованное меняет только статус
    const ids = a.status === 'DRAFT' ? (await allRecipients(a.id)).map(r => r.userId) : []
    await announcementsApi.update(getOrganizationId(), a.id, writeOf(a, ids), 'ARCHIVED')
    await load(0)
  } catch (e) {
    actionError[a.id] = errorText(e)
  } finally {
    busyId.value = null
  }
}

// --- редактор ---
const editorOpen = ref(false)
const editing = ref(null)
const saving = ref(false)
const publishNow = ref(false)
const formError = ref('')
const form = reactive({
  title: '', text: '', category: '', recipientUserIds: [], requiresResponse: false, responseDeadline: '', attachmentFileIds: []
})

const members = ref([])
const membersLoading = ref(false)
const membersError = ref('')
const memberSearch = ref('')

const visibleMembers = computed(() => {
  const q = memberSearch.value.trim().toLowerCase()
  return members.value.filter(m => !q || (m.fullName || '').toLowerCase().includes(q))
})

async function loadMembers() {
  if (members.value.length) return
  membersLoading.value = true
  membersError.value = ''
  try {
    const all = []
    for (let p = 0; p < 50; p++) {
      const res = await organizationsApi.members(getOrganizationId(), { status: 'ACTIVE', page: p, size: 100 })
      all.push(...res.items)
      if (p + 1 >= res.totalPages) break
    }
    members.value = all.sort((x, y) => (x.fullName || '').localeCompare(y.fullName || '', 'ru'))
  } catch (e) {
    membersError.value = errorText(e)
  } finally {
    membersLoading.value = false
  }
}

function selectRole(role) {
  const ids = new Set(form.recipientUserIds)
  for (const m of members.value) if (!role || m.roles.includes(role)) ids.add(m.userId)
  form.recipientUserIds = [...ids]
}

// Instant ↔ значение <input type="datetime-local"> в часовом поясе браузера
function toLocalInput(value) {
  if (!value) return ''
  const d = new Date(value)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function resetForm(a = null) {
  form.title = a?.title || ''
  form.text = a?.text || ''
  form.category = a?.category || ''
  form.recipientUserIds = []
  form.requiresResponse = a?.requiresResponse || false
  form.responseDeadline = toLocalInput(a?.responseDeadline)
  form.attachmentFileIds = a?.attachmentFileIds || []
  formError.value = ''
  publishNow.value = false
  memberSearch.value = ''
}

function openCreate() {
  editing.value = null
  resetForm()
  editorOpen.value = true
  loadMembers()
}

async function openEdit(a) {
  editing.value = a
  resetForm(a)
  editorOpen.value = true
  loadMembers()
  try {
    form.recipientUserIds = (await allRecipients(a.id)).map(r => r.userId)
  } catch (e) {
    formError.value = errorText(e)
  }
}

async function save() {
  if (saving.value) return
  formError.value = ''
  if (!form.title.trim() || !form.text.trim()) {
    formError.value = 'Заполните заголовок и текст.'
    return
  }
  if (!form.recipientUserIds.length) {
    formError.value = 'Выберите хотя бы одного получателя.'
    return
  }
  let deadline = null
  if (form.requiresResponse) {
    if (!form.responseDeadline) {
      formError.value = 'Укажите, до какого времени нужен ответ.'
      return
    }
    const d = new Date(form.responseDeadline)
    if (d <= new Date()) {
      formError.value = 'Срок ответа должен быть в будущем.'
      return
    }
    deadline = d.toISOString()
  }
  const body = {
    title: form.title.trim(),
    text: form.text.trim(),
    category: form.category.trim() || null,
    recipientUserIds: form.recipientUserIds,
    requiresResponse: form.requiresResponse,
    responseDeadline: deadline,
    attachmentFileIds: form.attachmentFileIds
  }
  saving.value = true
  try {
    const org = getOrganizationId()
    const status = publishNow.value ? 'PUBLISHED' : 'DRAFT'
    if (editing.value) {
      await announcementsApi.update(org, editing.value.id, body, status)
    } else {
      const created = await announcementsApi.create(org, body)
      if (publishNow.value) await announcementsApi.update(org, created.id, body, 'PUBLISHED')
    }
    editorOpen.value = false
    await load(0)
  } catch (e) {
    formError.value = errorText(e)
  } finally {
    saving.value = false
  }
}

// --- аудитория ---
const audienceOpen = ref(false)
const audienceFor = ref(null)
const audienceLoading = ref(false)
const audienceError = ref('')
const recipients = ref([])
const responses = ref([])

const readCount = computed(() => recipients.value.filter(r => r.readAt).length)
const answerCount = computed(() => ({
  ACCEPTED: recipients.value.filter(r => r.response === 'ACCEPTED').length,
  DECLINED: recipients.value.filter(r => r.response === 'DECLINED').length
}))

async function openAudience(a) {
  audienceFor.value = a
  audienceOpen.value = true
  audienceLoading.value = true
  audienceError.value = ''
  recipients.value = []
  responses.value = []
  try {
    const [rec, resp] = await Promise.all([
      allRecipients(a.id),
      announcementsApi.responses(getOrganizationId(), a.id, { page: 0, size: 100 })
    ])
    recipients.value = rec
    responses.value = resp.items
  } catch (e) {
    audienceError.value = errorText(e)
  } finally {
    audienceLoading.value = false
  }
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.filters { display: flex; gap: 12px; flex-wrap: wrap; }
.filter-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.filter-btn.active { background: #102522; color: white; border-color: #102522; }

.feed { display: flex; flex-direction: column; gap: 16px; }
.announce-card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  outline: 1px solid #E3EAE8; outline-offset: -1px;
  display: flex; flex-direction: column; gap: 16px;
}
.announce-card.important { outline: 2px solid #B7F34B; outline-offset: -2px; }

.announce-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.author { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.author-date { font-size: 12px; color: #98A6A2; }
.category { font-size: 12px; font-weight: 600; color: #35678E; background: #DDECFB; padding: 3px 10px; border-radius: 99px; }
.badges { display: flex; gap: 8px; flex-wrap: wrap; }
.badge-confirm, .badge-files, .badge-status {
  padding: 4px 10px; border-radius: 99px;
  font-size: 11px; font-weight: 700;
}
.badge-confirm { background: #FFF1D6; color: #8B6914; }
.badge-files { background: #F4F7F8; color: #6D7D79; }
.status-green { background: rgba(46, 139, 87, 0.12); color: #2E8B57; }
.status-yellow { background: rgba(242, 183, 5, 0.16); color: #8B6914; }
.status-red { background: rgba(214, 69, 69, 0.12); color: #D64545; }
.status-blue { background: rgba(59, 130, 246, 0.12); color: #3B82F6; }
.status-gray { background: rgba(136, 136, 136, 0.14); color: #888888; }

.announce-body h3 { font-size: 18px; font-weight: 700; color: #152421; margin-bottom: 8px; }
.announce-body p { font-size: 13px; color: #6D7D79; line-height: 1.6; white-space: pre-line; }

.card-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.btn-primary, .btn-secondary, .btn-ghost {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 14px; border-radius: 8px;
  font-size: 12px; font-weight: 700; cursor: pointer;
}
.btn-primary { background: #102522; border: none; color: white; }
.btn-secondary { background: #F4F7F8; border: 1px solid #E3EAE8; color: #152421; }
.btn-ghost { background: transparent; border: 1px solid #E3EAE8; color: #6D7D79; }
.btn-primary:disabled, .btn-ghost:disabled { opacity: 0.6; cursor: default; }
.btn-more {
  align-self: center; padding: 10px 20px; border-radius: 12px;
  background: white; border: 1px solid #E3EAE8; font-size: 13px; font-weight: 600; color: #152421; cursor: pointer;
}

.field { display: flex; flex-direction: column; gap: 8px; }
.field-label { font-size: 13px; font-weight: 600; color: #152421; display: flex; gap: 8px; align-items: baseline; }
.field-input {
  width: 100%; padding: 12px 14px; border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 14px; color: #152421; background: white; font-family: inherit;
}
.textarea { resize: vertical; min-height: 110px; }
.muted { font-size: 12px; font-weight: 500; color: #98A6A2; }
.quick-select { display: flex; gap: 8px; flex-wrap: wrap; }
.chip {
  padding: 6px 12px; border-radius: 99px; background: #F4F7F8; border: 1px solid #E3EAE8;
  font-size: 12px; font-weight: 600; color: #152421; cursor: pointer;
}
.recipients {
  max-height: 220px; overflow-y: auto; border: 1px solid #E3EAE8; border-radius: 12px; padding: 8px;
  display: flex; flex-direction: column; gap: 2px;
}
.recipient { display: flex; align-items: center; gap: 10px; padding: 6px 8px; border-radius: 8px; cursor: pointer; }
.recipient:hover { background: #F4F7F8; }
.recipient-name { flex: 1; font-size: 13px; color: #152421; }
.recipient-role { font-size: 12px; color: #98A6A2; }
.check-row { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #152421; cursor: pointer; }
.form-error { font-size: 13px; color: #D64545; }

.audience-summary { display: flex; gap: 12px; flex-wrap: wrap; }
.summary-item {
  flex: 1; min-width: 100px; padding: 12px 14px; background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; gap: 2px;
}
.summary-item b { font-size: 20px; color: #152421; }
.summary-item span { font-size: 12px; color: #6D7D79; }
.audience-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.audience-table th { text-align: left; color: #98A6A2; font-weight: 600; padding: 8px; border-bottom: 1px solid #E3EAE8; }
.audience-table td { padding: 10px 8px; border-bottom: 1px solid #F4F7F8; color: #152421; }
.history { display: flex; flex-direction: column; gap: 8px; }
.history-row { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; font-size: 13px; }
.history-name { font-weight: 600; color: #152421; }
.history-comment { color: #6D7D79; }
</style>
