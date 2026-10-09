<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Личное дело спортсмена"
        subtitle="Подробные показатели тренировочного процесса"
        :show-back="true"
        :show-search="false"
        @back="$router.push('/trainer/athletes')"
      />

      <div v-if="loading" class="card">
        <StateBlock kind="loading" />
      </div>
      <div v-else-if="loadError" class="card">
        <StateBlock kind="error" :message="loadError" />
      </div>

      <div v-else-if="athlete" class="split-view">
        <aside class="profile-card">
          <div class="avatar-large">{{ initials(fullName(athlete)) }}</div>
          <h2>{{ fullName(athlete) }}</h2>
          <span class="tag">{{ groupTag }}</span>
          <span class="status-badge" :class="`status-${tone(athlete.status)}`">{{ label('athleteStatus', athlete.status) }}</span>

          <div class="divider"></div>

          <div class="meta-list">
            <div class="meta-item">
              <span class="meta-label">Дата рождения</span>
              <span class="meta-value">{{ birthText }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Разряд</span>
              <span class="meta-value">{{ rankText }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Контакты родителей</span>
              <template v-if="athlete.parents?.length">
                <span v-for="p in athlete.parents" :key="p.parentUserId" class="meta-value">{{ parentLine(p) }}</span>
              </template>
              <span v-else class="meta-value muted">Не привязаны</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Мед. допуск</span>
              <span class="meta-value" :class="medical.cls">{{ medical.text }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Дата зачисления</span>
              <span class="meta-value">{{ formatDateShort(athlete.enrolledOn) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Аккаунт спортсмена</span>
              <span class="meta-value" :class="{ muted: !athlete.userId }">{{ athlete.userId ? 'Привязан' : 'Не привязан' }}</span>
            </div>
            <div v-if="athlete.note" class="meta-item">
              <span class="meta-label">Заметка</span>
              <span class="meta-value note">{{ athlete.note }}</span>
            </div>
          </div>

          <template v-if="canWrite">
            <div class="divider"></div>
            <div class="profile-actions">
              <button class="action-btn" @click="openEdit">Изменить</button>
              <button v-if="canLink" class="action-btn" @click="openParents">Родители</button>
              <button v-if="canLink" class="action-btn" @click="openAccount">Аккаунт</button>
              <button
                class="action-btn"
                :class="{ danger: athlete.status === 'ACTIVE' }"
                :disabled="statusSaving"
                @click="toggleArchive"
              >
                {{ athlete.status === 'ACTIVE' ? 'В архив' : 'Вернуть из архива' }}
              </button>
            </div>
            <p v-if="actionError" class="form-error">{{ actionError }}</p>
          </template>
        </aside>

        <div class="right-content">
          <div class="tabs">
            <button
              v-for="tab in TABS"
              :key="tab.key"
              class="tab-btn"
              :class="{ active: activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>

          <!-- История тренировок -->
          <div v-if="activeTab === 'history'" class="card">
            <h3>Тренировки за {{ HISTORY_DAYS }} дней</h3>
            <StateBlock v-if="history.loading" kind="loading" />
            <StateBlock v-else-if="history.error" kind="error" :message="history.error" />
            <StateBlock v-else-if="!history.items.length" kind="empty" message="За этот период тренировок в группах спортсмена не было" />
            <div v-else class="row-list">
              <button
                v-for="t in history.items"
                :key="t.id"
                type="button"
                class="row-item clickable"
                @click="openTraining(t)"
              >
                <span class="row-main">
                  <span class="row-title">{{ t.title }}</span>
                  <span class="row-sub">
                    {{ formatDate(t.startsAt) }} · {{ formatTime(t.startsAt) }}–{{ formatTime(t.endsAt) }}{{ groupNameById[t.groupId] ? ` · ${groupNameById[t.groupId]}` : '' }}
                  </span>
                </span>
                <span class="status-badge" :class="`status-${tone(t.status)}`">{{ label('trainingStatus', t.status) }}</span>
              </button>
            </div>
          </div>

          <!-- Посещаемость -->
          <div v-else-if="activeTab === 'attendance'" class="card">
            <h3>Посещаемость за {{ ATTENDANCE_DAYS }} дней</h3>
            <StateBlock v-if="attendance.loading" kind="loading" />
            <StateBlock v-else-if="attendance.unavailable" kind="empty" message="Журнал посещаемости появится позже" />
            <StateBlock v-else-if="attendance.error" kind="error" :message="attendance.error" />
            <template v-else>
              <div v-if="attendanceMetrics.length" class="metrics-row">
                <div v-for="m in attendanceMetrics" :key="m.label" class="metric-card">
                  <span class="metric-label">{{ m.label }}</span>
                  <span class="metric-value">{{ m.value }}</span>
                  <span v-if="m.note" class="metric-trend neutral">{{ m.note }}</span>
                </div>
              </div>
              <div class="divider"></div>
              <StateBlock v-if="!attendance.items.length" kind="empty" message="Прошедших тренировок за этот период нет" />
              <div v-else class="row-list">
                <div v-for="item in attendance.items" :key="`${item.trainingId}-${item.athleteId}`" class="row-item">
                  <span class="row-main">
                    <span class="row-title">{{ item.trainingTitle }}</span>
                    <span class="row-sub">{{ formatDate(item.startsAt) }} · {{ formatTime(item.startsAt) }}{{ attendanceNote(item) }}</span>
                  </span>
                  <span class="status-badge" :class="`status-${tone(item.status)}`">{{ label('attendanceStatus', item.status) }}</span>
                </div>
              </div>
              <p v-if="attendance.truncated" class="form-hint">Показаны последние {{ attendance.items.length }} записей.</p>
            </template>
          </div>

          <!-- Прогресс и нормативы -->
          <div v-else-if="activeTab === 'progress'" class="card">
            <div class="card-head">
              <h3>Результаты контрольных срезов</h3>
              <button v-if="canWriteProgress" class="action-btn" @click="goProgress">Внести результат</button>
            </div>

            <StateBlock v-if="progress.loading" kind="loading" />
            <StateBlock v-else-if="progress.error" kind="error" :message="progress.error" />
            <template v-else>
              <StateBlock v-if="!metricCards.length" kind="empty" message="Результатов пока нет" />
              <div v-else class="metrics-row">
                <button
                  v-for="m in metricCards"
                  :key="m.key"
                  type="button"
                  class="metric-card clickable"
                  :class="{ selected: chartSeries?.key === m.key }"
                  @click="chartKey = m.key"
                >
                  <span class="metric-label">{{ m.name }}</span>
                  <span class="metric-value">{{ m.value }}</span>
                  <span class="metric-trend" :class="m.best ? 'up' : 'neutral'">{{ m.trend }}</span>
                </button>
              </div>

              <template v-if="chartBars.length">
                <div class="divider"></div>

                <div class="chart-block">
                  <h4>Динамика результатов: {{ chartSeries.name }}</h4>
                  <div class="bars-chart">
                    <div v-for="bar in chartBars" :key="bar.key" class="bar-item">
                      <span class="bar-value">{{ bar.value }}</span>
                      <div class="bar" :style="{ height: bar.height + 'px', background: bar.active ? '#B7F34B' : '#102522' }"></div>
                      <span class="bar-label">{{ bar.label }}</span>
                    </div>
                  </div>
                </div>
              </template>

              <div class="divider"></div>

              <div class="chart-block">
                <h4>Нормативы</h4>
                <StateBlock v-if="!progress.standards.length" kind="empty" message="Нормативы ещё не внесены" />
                <div v-else class="row-list">
                  <div v-for="s in progress.standards" :key="s.id" class="row-item">
                    <span class="row-main">
                      <span class="row-title">{{ s.name }}</span>
                      <span class="row-sub">
                        Норма: {{ s.targetText }} · Факт: {{ s.resultText || '—' }} · {{ formatDateShort(s.assessedOn) }}{{ s.comment ? ` · ${s.comment}` : '' }}
                      </span>
                    </span>
                    <span class="status-badge" :class="`status-${tone(s.status)}`">{{ label('standardStatus', s.status) }}</span>
                  </div>
                </div>
              </div>

              <div class="divider"></div>

              <div class="chart-block">
                <h4>Разряды</h4>
                <StateBlock v-if="!rankRows.length" kind="empty" message="Разряды ещё не присвоены" />
                <div v-else class="row-list">
                  <div v-for="r in rankRows" :key="r.id" class="row-item">
                    <span class="row-main">
                      <span class="row-title">{{ r.name }}</span>
                      <span class="row-sub">
                        Присвоен {{ formatDateShort(r.assignedOn) }}{{ r.validUntil ? ` · действует до ${formatDateShort(r.validUntil)}` : '' }}{{ r.comment ? ` · ${r.comment}` : '' }}
                      </span>
                    </span>
                    <span v-if="rankExpired(r)" class="status-badge status-gray">Срок истёк</span>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- Документы -->
          <div v-else-if="activeTab === 'documents'" class="card">
            <div class="card-head">
              <h3>Документы</h3>
              <button v-if="canUpload" class="action-btn" @click="openUpload">Загрузить документ</button>
            </div>
            <StateBlock v-if="docs.loading" kind="loading" />
            <StateBlock v-else-if="docs.error" kind="error" :message="docs.error" />
            <StateBlock v-else-if="!documentRows.length" kind="empty" message="Документов пока нет" />
            <div v-else class="row-list">
              <div v-for="d in documentRows" :key="d.id" class="row-item">
                <span class="row-main">
                  <span class="row-title">{{ d.title }}</span>
                  <span class="row-sub">{{ label('documentType', d.type) }}{{ docDates(d) }}</span>
                  <span v-if="d.file" class="row-sub">{{ d.file.originalName }} · {{ fileSize(d.file.sizeBytes) }}</span>
                </span>
                <span class="row-side">
                  <span v-if="docExpired(d)" class="status-badge status-red">Срок истёк</span>
                  <a v-if="d.file" class="action-btn" :href="fileUrl(d)" @click.prevent="downloadDocument(d)">
                    {{ downloadingId === d.id ? 'Скачивание…' : 'Скачать' }}
                  </a>
                </span>
              </div>
            </div>
            <p v-if="downloadError" class="form-error">{{ downloadError }}</p>
          </div>

          <!-- Начисления -->
          <div v-else class="card">
            <h3>Начисления</h3>
            <StateBlock v-if="charges.loading" kind="loading" />
            <StateBlock v-else-if="charges.unavailable" kind="empty" message="Начисления появятся позже" />
            <StateBlock v-else-if="charges.error" kind="error" :message="charges.error" />
            <StateBlock v-else-if="!charges.items.length" kind="empty" message="Начислений пока нет" />
            <template v-else>
              <div class="metrics-row">
                <div class="metric-card">
                  <span class="metric-label">Начислено</span>
                  <span class="metric-value">{{ formatMoney(chargeTotals.amount) }}</span>
                </div>
                <div class="metric-card">
                  <span class="metric-label">Оплачено</span>
                  <span class="metric-value">{{ formatMoney(chargeTotals.paid) }}</span>
                </div>
                <div class="metric-card">
                  <span class="metric-label">К оплате</span>
                  <span class="metric-value">{{ formatMoney(chargeTotals.remaining) }}</span>
                  <span v-if="chargeTotals.overdue" class="metric-trend down">Просрочено: {{ chargeTotals.overdue }}</span>
                </div>
              </div>
              <div class="divider"></div>
              <div class="row-list">
                <div v-for="c in charges.items" :key="c.id" class="row-item">
                  <span class="row-main">
                    <span class="row-title">{{ c.title }}</span>
                    <span class="row-sub">{{ chargeLine(c) }}</span>
                  </span>
                  <span class="row-side">
                    <span v-if="c.status === 'CANCELLED'" class="status-badge status-gray">{{ label('chargeStatus', c.status) }}</span>
                    <template v-else>
                      <span v-if="c.overdue" class="status-badge status-red">Просрочено</span>
                      <span class="status-badge" :class="`status-${tone(c.paymentStatus)}`">{{ label('paymentStatus', c.paymentStatus) }}</span>
                    </template>
                  </span>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- Изменение карточки -->
      <BaseModal
        v-model="edit.open"
        title="Изменить карточку"
        :submit-label="edit.saving ? 'Сохранение…' : 'Сохранить'"
        @submit="submitEdit"
      >
        <BaseInput id="card-last-name" v-model="edit.lastName" label="Фамилия" />
        <BaseInput id="card-first-name" v-model="edit.firstName" label="Имя" />
        <BaseInput id="card-middle-name" v-model="edit.middleName" label="Отчество (необязательно)" />
        <BaseInput id="card-birth-date" v-model="edit.birthDate" type="date" label="Дата рождения" />
        <BaseInput id="card-enrolled-on" v-model="edit.enrolledOn" type="date" label="Дата зачисления" />
        <div class="field">
          <label class="field-label" for="card-note">Заметка</label>
          <textarea id="card-note" v-model="edit.note" class="field-control field-textarea" rows="4"></textarea>
        </div>
        <p v-if="edit.error" class="form-error">{{ edit.error }}</p>
      </BaseModal>

      <!-- Родители -->
      <BaseModal
        v-model="parents.open"
        title="Родители спортсмена"
        :submit-label="parents.saving ? 'Сохранение…' : 'Привязать'"
        @submit="submitParent"
      >
        <div class="field">
          <span class="field-label">Привязанные родители</span>
          <p v-if="!athlete?.parents?.length" class="form-hint">Пока никто не привязан.</p>
          <div v-for="p in athlete?.parents || []" :key="p.parentUserId" class="link-row">
            <span class="row-main">
              <span class="row-title">{{ p.fullName || 'Без имени' }}{{ p.relationship ? ` (${p.relationship})` : '' }}</span>
              <span class="row-sub">{{ p.email }}</span>
            </span>
            <button
              type="button"
              class="action-btn danger"
              :disabled="parents.removingId === p.parentUserId"
              @click="removeParent(p)"
            >
              Отвязать
            </button>
          </div>
        </div>
        <div class="field">
          <label class="field-label" for="parent-candidate">Родитель из участников организации</label>
          <StateBlock v-if="parents.loading" kind="loading" />
          <p v-else-if="parents.listError" class="form-error">Список родителей недоступен: {{ parents.listError }}</p>
          <select
            v-else
            id="parent-candidate"
            v-model="parents.parentUserId"
            class="field-control"
            :disabled="!!parents.email.trim()"
          >
            <option value="">{{ parentCandidates.length ? 'Выберите родителя' : 'Нет непривязанных родителей' }}</option>
            <option v-for="m in parentCandidates" :key="m.userId" :value="m.userId">{{ m.fullName || 'Без имени' }}</option>
          </select>
        </div>
        <BaseInput
          id="parent-email"
          v-model="parents.email"
          type="email"
          label="…или email зарегистрированного аккаунта родителя"
          placeholder="name@example.ru"
        />
        <BaseInput id="parent-relationship" v-model="parents.relationship" label="Кем приходится (необязательно)" placeholder="Мать, отец, опекун" />
        <p class="form-hint">Родитель увидит расписание, посещаемость, результаты, документы и начисления ребёнка.</p>
        <p v-if="parents.error" class="form-error">{{ parents.error }}</p>
      </BaseModal>

      <!-- Аккаунт спортсмена -->
      <BaseModal
        v-model="account.open"
        title="Аккаунт спортсмена"
        :submit-label="account.saving ? 'Сохранение…' : (athlete?.userId ? 'Привязать другой' : 'Привязать')"
        @submit="submitAccount"
      >
        <p class="modal-text">
          {{ athlete?.userId
            ? 'Аккаунт привязан: спортсмен видит свою карточку, расписание, посещаемость и начисления.'
            : 'Аккаунт не привязан. Укажите email зарегистрированного и подтверждённого аккаунта спортсмена.' }}
        </p>
        <button
          v-if="athlete?.userId"
          type="button"
          class="action-btn danger self-start"
          :disabled="account.saving"
          @click="unlinkAccount"
        >
          Отвязать аккаунт
        </button>
        <BaseInput id="account-email" v-model="account.email" type="email" label="Email аккаунта спортсмена" placeholder="name@example.ru" />
        <p v-if="account.error" class="form-error">{{ account.error }}</p>
      </BaseModal>

      <!-- Загрузка документа -->
      <BaseModal
        v-model="upload.open"
        title="Загрузить документ"
        :submit-label="upload.saving ? 'Загрузка…' : 'Загрузить'"
        @submit="submitUpload"
      >
        <div class="field">
          <label class="field-label" for="doc-file">Файл: PDF, PNG или JPEG, до 10 МБ</label>
          <input id="doc-file" type="file" class="field-control" accept=".pdf,.png,.jpg,.jpeg" @change="onFile" />
        </div>
        <BaseInput id="doc-title" v-model="upload.title" label="Название" placeholder="Медицинская справка" />
        <div class="field">
          <label class="field-label" for="doc-type">Тип</label>
          <select id="doc-type" v-model="upload.type" class="field-control">
            <option v-for="o in documentTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <BaseInput id="doc-issued" v-model="upload.issuedOn" type="date" label="Дата выдачи (необязательно)" />
        <BaseInput id="doc-valid" v-model="upload.validUntil" type="date" label="Действует до (необязательно)" />
        <p v-if="upload.error" class="form-error">{{ upload.error }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { athletesApi } from '../../api/athletes'
import { groupsApi } from '../../api/groups'
import { organizationsApi } from '../../api/organizations'
import { trainingsApi } from '../../api/trainings'
import { attendanceApi } from '../../api/attendance'
import { documentsApi, filesApi } from '../../api/documents'
import { chargesApi } from '../../api/charges'
import { getOrganizationId, hasPermission, hasRole } from '../../utils/session'
import {
  errorText, formatDate, formatDateShort, formatTime, formatMoney, fullName, initials,
  ageYears, parseDate, toIsoDate, toIsoDateTime, addDays
} from '../../utils/format'
import { label, options, tone } from '../../utils/labels'

const TABS = [
  { key: 'history', label: 'История тренировок' },
  { key: 'attendance', label: 'Посещаемость' },
  { key: 'progress', label: 'Прогресс и нормативы' },
  { key: 'documents', label: 'Документы' },
  { key: 'charges', label: 'Начисления' }
]
const HISTORY_DAYS = 90
const ATTENDANCE_DAYS = 90
const CHART_POINTS = 6
const MAX_PAGES = 10
const MAX_FILE_SIZE = 10 * 1024 * 1024
// Эндпоинты журнала и начислений дописываются на сервере — их отсутствие не ошибка карточки
const UNAVAILABLE = [404, 405, 501]
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const FIELD_LABELS = {
  firstName: 'Имя', lastName: 'Фамилия', middleName: 'Отчество', birthDate: 'Дата рождения',
  enrolledOn: 'Дата зачисления', status: 'Статус', note: 'Заметка', userId: 'Аккаунт',
  parentLinks: 'Родители', email: 'Email', title: 'Название', type: 'Тип', file: 'Файл',
  issuedOn: 'Дата выдачи', validUntil: 'Действует до'
}
const documentTypeOptions = options('documentType')

const route = useRoute()
const router = useRouter()
const athleteId = computed(() => route.params.id)
const activeTab = ref('progress')

// ─────────── Права (запись — только роль тренера с нужным правом) ───────────
const isTrainer = computed(() => hasRole('TRAINER'))
const canWrite = computed(() => isTrainer.value && hasPermission('athletes.write'))
const canLink = computed(() => canWrite.value && hasPermission('members.write'))
const canUpload = computed(() => isTrainer.value && hasPermission('documents.write'))
const canWriteProgress = computed(() => isTrainer.value && hasPermission('progress.write'))

// ─────────── Данные карточки ───────────
const athlete = ref(null)
const loading = ref(true)
const loadError = ref('')

const groupsState = reactive({ loading: false, error: '', items: [] })
const progress = reactive({ loading: false, error: '', results: [], standards: [], ranks: [] })
const docs = reactive({ loading: false, error: '', items: [] })
const history = reactive({ loaded: false, loading: false, error: '', items: [] })
const attendance = reactive({
  loaded: false, loading: false, error: '', unavailable: false, items: [], summary: null, truncated: false
})
const charges = reactive({ loaded: false, loading: false, error: '', unavailable: false, items: [] })
const chartKey = ref('')

// Все страницы списка (size ≤ 100), последовательно
async function fetchPages(request, params = {}) {
  const first = await request({ ...params, page: 0, size: 100 })
  const items = [...(first.items || [])]
  const pages = Math.min(first.totalPages || 1, MAX_PAGES)
  for (let page = 1; page < pages; page++) {
    const next = await request({ ...params, page, size: 100 })
    items.push(...(next.items || []))
  }
  return { first, items, truncated: (first.totalPages || 1) > MAX_PAGES }
}

async function fetchAll(request, params = {}) {
  return (await fetchPages(request, params)).items
}

// Поколение загрузки: ответы для прежнего спортсмена (переход между карточками) отбрасываются
let gen = 0

async function track(state, task) {
  const my = gen
  state.loading = true
  state.error = ''
  if ('unavailable' in state) state.unavailable = false
  try {
    const apply = await task()
    if (my === gen) apply()
  } catch (e) {
    if (my !== gen) return
    if ('unavailable' in state && UNAVAILABLE.includes(e.status)) state.unavailable = true
    else state.error = errorText(e)
  } finally {
    if (my === gen) state.loading = false
  }
}

function resetSections() {
  Object.assign(groupsState, { loading: false, error: '', items: [] })
  Object.assign(progress, { loading: false, error: '', results: [], standards: [], ranks: [] })
  Object.assign(docs, { loading: false, error: '', items: [] })
  Object.assign(history, { loaded: false, loading: false, error: '', items: [] })
  Object.assign(attendance, {
    loaded: false, loading: false, error: '', unavailable: false, items: [], summary: null, truncated: false
  })
  Object.assign(charges, { loaded: false, loading: false, error: '', unavailable: false, items: [] })
  chartKey.value = ''
  actionError.value = ''
}

async function loadAll() {
  const my = ++gen
  loading.value = true
  loadError.value = ''
  athlete.value = null
  resetSections()
  try {
    const data = await athletesApi.get(getOrganizationId(), athleteId.value)
    if (my !== gen) return
    athlete.value = data
  } catch (e) {
    if (my !== gen) return
    loadError.value = errorText(e)
    return
  } finally {
    if (my === gen) loading.value = false
  }
  loadGroups()
  loadProgress()
  loadDocuments()
  loadTab(activeTab.value)
}

function loadGroups() {
  const id = athleteId.value
  return track(groupsState, async () => {
    const list = await fetchAll(params => groupsApi.list(getOrganizationId(), params), { athleteId: id })
    return () => { groupsState.items = list }
  })
}

function loadProgress() {
  const id = athleteId.value
  return track(progress, async () => {
    const org = getOrganizationId()
    const [results, standards, ranks] = await Promise.all([
      fetchAll(params => athletesApi.results(org, id, params)),
      fetchAll(params => athletesApi.standards(org, id, params)),
      fetchAll(params => athletesApi.ranks(org, id, params))
    ])
    return () => Object.assign(progress, { results, standards, ranks })
  })
}

function loadDocuments() {
  const id = athleteId.value
  return track(docs, async () => {
    const list = await fetchAll(params => documentsApi.list(getOrganizationId(), params), { athleteId: id })
    return () => { docs.items = list }
  })
}

function loadHistory() {
  const id = athleteId.value
  const now = new Date()
  history.loaded = true
  return track(history, async () => {
    const list = await fetchAll(params => trainingsApi.list(getOrganizationId(), params), {
      athleteId: id,
      from: toIsoDateTime(addDays(now, -HISTORY_DAYS)),
      to: toIsoDateTime(now)
    })
    return () => { history.items = list.sort((a, b) => new Date(b.startsAt) - new Date(a.startsAt)) }
  })
}

function loadAttendance() {
  const id = athleteId.value
  const today = new Date()
  attendance.loaded = true
  return track(attendance, async () => {
    const res = await fetchPages(params => attendanceApi.list(getOrganizationId(), params), {
      athleteId: id,
      from: toIsoDate(addDays(today, -(ATTENDANCE_DAYS - 1))),
      to: toIsoDate(today)
    })
    return () => Object.assign(attendance, { items: res.items, summary: res.first.summary || null, truncated: res.truncated })
  })
}

function loadCharges() {
  const id = athleteId.value
  charges.loaded = true
  return track(charges, async () => {
    const list = await fetchAll(params => chargesApi.list(getOrganizationId(), params), { athleteId: id })
    return () => { charges.items = list.sort((a, b) => (b.dueOn || '').localeCompare(a.dueOn || '')) }
  })
}

// Вкладки истории, посещаемости и начислений загружаются при первом открытии
function loadTab(tab) {
  if (!athlete.value) return
  if (tab === 'history' && !history.loaded) loadHistory()
  if (tab === 'attendance' && !attendance.loaded) loadAttendance()
  if (tab === 'charges' && !charges.loaded) loadCharges()
}

watch(activeTab, loadTab)
// Переход с одной карточки на другую: компонент тот же, меняется только id
watch(() => route.params.id, id => {
  if (route.name === 'trainer-athlete-card' && id) loadAll()
})
onMounted(loadAll)

// ─────────── Профиль ───────────
const todayKey = () => toIsoDate(new Date())

const groupNameById = computed(() => Object.fromEntries(groupsState.items.map(g => [g.id, g.name])))

const groupTag = computed(() => {
  if (groupsState.loading) return 'Загрузка групп…'
  if (groupsState.error) return 'Группы недоступны'
  const names = groupsState.items.map(g => g.name)
  return names.length ? names.join(', ') : 'Без группы'
})

const birthText = computed(() => {
  const a = athlete.value
  if (!a?.birthDate) return '—'
  const age = ageYears(a.birthDate)
  return `${formatDateShort(a.birthDate)} (${age} ${plural(age, 'год', 'года', 'лет')})`
})

const rankRows = computed(() =>
  [...progress.ranks].sort((a, b) => (b.assignedOn || '').localeCompare(a.assignedOn || '')))

function rankExpired(r) {
  return !!r.validUntil && r.validUntil < todayKey()
}

// Текущий разряд — последний присвоенный из действующих, иначе последний
const currentRank = computed(() => rankRows.value.find(r => !rankExpired(r)) || rankRows.value[0] || null)

const rankText = computed(() => {
  if (progress.loading) return 'Загрузка…'
  if (progress.error) return '—'
  const r = currentRank.value
  if (!r) return 'Не присвоен'
  if (rankExpired(r)) return `${r.name} · срок истёк ${formatDateShort(r.validUntil)}`
  return r.validUntil ? `${r.name} · до ${formatDateShort(r.validUntil)}` : r.name
})

function parentLine(p) {
  const name = `${p.fullName || 'Без имени'}${p.relationship ? ` (${p.relationship})` : ''}`
  return p.email ? `${name} · ${p.email}` : name
}

// Мед. допуск — по загруженным медицинским справкам
const medical = computed(() => {
  if (docs.loading) return { text: 'Загрузка…', cls: '' }
  if (docs.error) return { text: '—', cls: '' }
  const certs = docs.items.filter(d => d.type === 'MEDICAL_CERTIFICATE')
  if (!certs.length) return { text: 'Справка не загружена', cls: 'warn' }
  const now = todayKey()
  const dated = certs.filter(d => d.validUntil).sort((a, b) => b.validUntil.localeCompare(a.validUntil))
  if (dated.length && dated[0].validUntil >= now) {
    return { text: `До ${formatDateShort(dated[0].validUntil)} · действует`, cls: 'ok' }
  }
  const open = certs.find(d => !d.validUntil)
  if (open) return { text: `Справка${open.issuedOn ? ` от ${formatDateShort(open.issuedOn)}` : ''} · срок не указан`, cls: '' }
  return { text: `Истекла ${formatDateShort(dated[0].validUntil)}`, cls: 'bad' }
})

// ─────────── Прогресс ───────────
const formatNumber = (value, digits = 3) =>
  Number(value).toLocaleString('ru-RU', { maximumFractionDigits: digits })

function shortDate(value) {
  const d = parseDate(value)
  if (!d || isNaN(d)) return '—'
  return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`
}

// Ряды по показателю и единице; точки — по возрастанию даты замера (так их отдаёт сервер)
const series = computed(() => {
  const map = new Map()
  for (const r of progress.results) {
    const key = `${r.metricName}\u0000${r.unit}`
    if (!map.has(key)) map.set(key, { key, name: r.metricName, unit: r.unit, points: [] })
    map.get(key).points.push(r)
  }
  const list = [...map.values()]
  // Замеры одного дня — по времени внесения
  list.forEach(s => s.points.sort((a, b) =>
    a.measuredOn.localeCompare(b.measuredOn) || (a.createdAt || '').localeCompare(b.createdAt || '')))
  const lastDate = s => s.points[s.points.length - 1].measuredOn
  return list.sort((a, b) => lastDate(b).localeCompare(lastDate(a)))
})

// Направление «лучше» у показателей разное (время — меньше, повторы — больше), поэтому изменение без оценки
const metricCards = computed(() => series.value.map(s => {
  const latest = s.points[s.points.length - 1]
  const prev = s.points[s.points.length - 2]
  let trend
  if (prev) {
    const diff = Number(latest.value) - Number(prev.value)
    const sign = diff > 0 ? '+' : diff < 0 ? '−' : '±'
    trend = `${sign}${formatNumber(Math.abs(diff))} ${s.unit} к ${shortDate(prev.measuredOn)}`
  } else {
    trend = `Первый замер · ${formatDateShort(latest.measuredOn)}`
  }
  if (latest.isPersonalBest) trend = `Личный рекорд · ${trend}`
  return { key: s.key, name: s.name, value: `${formatNumber(latest.value)} ${s.unit}`, trend, best: latest.isPersonalBest }
}))

const chartSeries = computed(() => series.value.find(s => s.key === chartKey.value) || series.value[0] || null)

const chartBars = computed(() => {
  const s = chartSeries.value
  if (!s) return []
  const points = s.points.slice(-CHART_POINTS)
  const values = points.map(p => Number(p.value))
  const min = Math.min(...values)
  const max = Math.max(...values)
  return points.map((p, i) => ({
    key: p.id,
    value: `${formatNumber(p.value)} ${s.unit}`,
    height: max === min ? 70 : Math.round(30 + ((Number(p.value) - min) / (max - min)) * 70),
    label: shortDate(p.measuredOn),
    active: i === points.length - 1
  }))
})

function goProgress() {
  router.push({ path: '/trainer/progress', query: { athleteId: athlete.value.id } })
}

// ─────────── История и посещаемость ───────────
function openTraining(t) {
  router.push(`/trainer/trainings/${t.id}/report`)
}

const attendanceMetrics = computed(() => {
  const s = attendance.summary
  if (!s) return []
  const marked = s.present + s.sick + s.absent
  const percent = s.attendancePercent === null || s.attendancePercent === undefined
    ? '—'
    : `${formatNumber(s.attendancePercent, 0)}%`
  return [
    { label: 'Посещаемость', value: percent, note: marked ? `был на ${s.present} из ${marked}` : 'отметок пока нет' },
    { label: 'Был', value: String(s.present) },
    { label: 'Болел', value: String(s.sick) },
    { label: 'Не был', value: String(s.absent) },
    ...(s.unmarked ? [{ label: 'Без отметки', value: String(s.unmarked) }] : [])
  ]
})

function attendanceNote(item) {
  const parts = [item.reason, item.comment].filter(Boolean)
  return parts.length ? ` · ${parts.join(' · ')}` : ''
}

// ─────────── Документы ───────────
const documentRows = computed(() =>
  [...docs.items].sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || '')))

function docDates(d) {
  const parts = []
  if (d.issuedOn) parts.push(`выдан ${formatDateShort(d.issuedOn)}`)
  if (d.validUntil) parts.push(`действует до ${formatDateShort(d.validUntil)}`)
  return parts.length ? ` · ${parts.join(' · ')}` : ''
}

function docExpired(d) {
  return !!d.validUntil && d.validUntil < todayKey()
}

function fileSize(bytes) {
  const n = Number(bytes) || 0
  if (n < 1024 * 1024) return `${Math.max(1, Math.round(n / 1024))} КБ`
  return `${(n / 1024 / 1024).toLocaleString('ru-RU', { maximumFractionDigits: 1 })} МБ`
}

function fileUrl(d) {
  return filesApi.contentUrl(getOrganizationId(), d.file.id)
}

// Сервер отдаёт файл под внутренним именем без расширения — сохраняем под исходным именем через blob
const downloadingId = ref(null)
const downloadError = ref('')

async function downloadDocument(d) {
  if (downloadingId.value) return
  downloadingId.value = d.id
  downloadError.value = ''
  try {
    const res = await fetch(fileUrl(d), { credentials: 'include' })
    if (!res.ok) {
      const error = new Error(`Не удалось скачать файл (HTTP ${res.status}).`)
      error.status = res.status
      throw error
    }
    const url = URL.createObjectURL(await res.blob())
    const link = document.createElement('a')
    link.href = url
    link.download = d.file.originalName || d.title
    document.body.appendChild(link)
    link.click()
    link.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    downloadError.value = errorText(e)
  } finally {
    downloadingId.value = null
  }
}

const upload = reactive({ open: false, saving: false, error: '', file: null, title: '', type: 'MEDICAL_CERTIFICATE', issuedOn: '', validUntil: '' })

function openUpload() {
  Object.assign(upload, {
    open: true, saving: false, error: '', file: null, title: '', type: 'MEDICAL_CERTIFICATE', issuedOn: '', validUntil: ''
  })
}

function onFile(event) {
  const file = event.target.files?.[0] || null
  upload.file = file
  if (file && !upload.title.trim()) upload.title = file.name.replace(/\.[^.]+$/, '')
}

async function submitUpload() {
  if (upload.saving) return
  upload.error = ''
  const title = upload.title.trim()
  if (!upload.file) {
    upload.error = 'Выберите файл.'
    return
  }
  if (!/\.(pdf|png|jpe?g)$/i.test(upload.file.name)) {
    upload.error = 'Можно загрузить только PDF, PNG или JPEG.'
    return
  }
  if (upload.file.size > MAX_FILE_SIZE) {
    upload.error = 'Файл больше 10 МБ.'
    return
  }
  if (!title) {
    upload.error = 'Укажите название документа.'
    return
  }
  if (upload.issuedOn && upload.validUntil && upload.validUntil < upload.issuedOn) {
    upload.error = 'Срок действия не может закончиться раньше даты выдачи.'
    return
  }
  upload.saving = true
  try {
    await documentsApi.create(getOrganizationId(), {
      file: upload.file,
      title,
      type: upload.type,
      athleteId: athlete.value.id,
      issuedOn: upload.issuedOn || undefined,
      validUntil: upload.validUntil || undefined
    })
    upload.open = false
    loadDocuments()
  } catch (e) {
    upload.error = formErrorText(e)
  } finally {
    upload.saving = false
  }
}

// ─────────── Начисления ───────────
const chargeTotals = computed(() => {
  const active = charges.items.filter(c => c.status !== 'CANCELLED')
  const sum = key => active.reduce((total, c) => total + (Number(c[key]) || 0), 0)
  return {
    amount: sum('amount'),
    paid: sum('paidAmount'),
    remaining: sum('remainingAmount'),
    overdue: active.filter(c => c.overdue).length
  }
})

function chargeLine(c) {
  const parts = [label('chargeType', c.type), `срок ${formatDateShort(c.dueOn)}`, formatMoney(c.amount)]
  if (Number(c.paidAmount) > 0) parts.push(`оплачено ${formatMoney(c.paidAmount)}`)
  if (c.status === 'CANCELLED' && c.cancelReason) parts.push(`причина: ${c.cancelReason}`)
  return parts.join(' · ')
}

// ─────────── Изменение карточки и статуса ───────────
const actionError = ref('')
const statusSaving = ref(false)
const edit = reactive({
  open: false, saving: false, error: '', lastName: '', firstName: '', middleName: '', birthDate: '', enrolledOn: '', note: ''
})

function openEdit() {
  const a = athlete.value
  Object.assign(edit, {
    open: true,
    saving: false,
    error: '',
    lastName: a.lastName || '',
    firstName: a.firstName || '',
    middleName: a.middleName || '',
    birthDate: a.birthDate || '',
    enrolledOn: a.enrolledOn || '',
    note: a.note || ''
  })
}

async function submitEdit() {
  if (edit.saving) return
  edit.error = ''
  const lastName = edit.lastName.trim()
  const firstName = edit.firstName.trim()
  if (!lastName || !firstName) {
    edit.error = 'Укажите фамилию и имя.'
    return
  }
  if (!edit.birthDate || !edit.enrolledOn) {
    edit.error = 'Укажите дату рождения и дату зачисления.'
    return
  }
  if (edit.birthDate > todayKey()) {
    edit.error = 'Дата рождения не может быть в будущем.'
    return
  }
  if (edit.enrolledOn < edit.birthDate) {
    edit.error = 'Дата зачисления не может быть раньше даты рождения.'
    return
  }
  edit.saving = true
  try {
    athlete.value = await athletesApi.update(getOrganizationId(), athlete.value.id, {
      firstName,
      lastName,
      middleName: edit.middleName.trim() || null,
      birthDate: edit.birthDate,
      enrolledOn: edit.enrolledOn,
      note: edit.note.trim() || null
    })
    edit.open = false
  } catch (e) {
    edit.error = formErrorText(e)
  } finally {
    edit.saving = false
  }
}

async function toggleArchive() {
  const a = athlete.value
  const archive = a.status === 'ACTIVE'
  const question = archive
    ? `Перенести «${fullName(a)}» в архив? Посещения, документы и платежи сохранятся.`
    : `Вернуть «${fullName(a)}» в число активных спортсменов?`
  if (!confirm(question)) return
  statusSaving.value = true
  actionError.value = ''
  try {
    athlete.value = await athletesApi.update(getOrganizationId(), a.id, { status: archive ? 'ARCHIVED' : 'ACTIVE' })
  } catch (e) {
    actionError.value = errorText(e)
  } finally {
    statusSaving.value = false
  }
}

// ─────────── Родители: parentLinks заменяет список целиком ───────────
const parents = reactive({
  open: false, saving: false, error: '', loading: false, listError: '',
  candidates: [], parentUserId: '', email: '', relationship: '', removingId: null
})

const parentCandidates = computed(() => {
  const linked = new Set((athlete.value?.parents || []).map(p => p.parentUserId))
  return parents.candidates.filter(m => !linked.has(m.userId))
})

function currentLinks(exceptUserId = null) {
  return (athlete.value?.parents || [])
    .filter(p => p.parentUserId !== exceptUserId)
    .map(p => ({ parentUserId: p.parentUserId, relationship: p.relationship || null }))
}

async function openParents() {
  Object.assign(parents, {
    open: true, saving: false, error: '', listError: '', parentUserId: '', email: '', relationship: '', removingId: null
  })
  parents.loading = true
  try {
    const list = await fetchAll(params => organizationsApi.members(getOrganizationId(), params), { role: 'PARENT', status: 'ACTIVE' })
    parents.candidates = list.sort((a, b) => (a.fullName || '').localeCompare(b.fullName || '', 'ru'))
  } catch (e) {
    parents.candidates = []
    parents.listError = errorText(e)
  } finally {
    parents.loading = false
  }
}

async function submitParent() {
  if (parents.saving) return
  parents.error = ''
  const email = parents.email.trim()
  if (!email && !parents.parentUserId) {
    parents.error = 'Выберите родителя из списка или укажите email его аккаунта.'
    return
  }
  if (email && !EMAIL_RE.test(email)) {
    parents.error = 'Проверьте email родителя.'
    return
  }
  parents.saving = true
  try {
    const org = getOrganizationId()
    // По email аккаунт получает роль PARENT; связь создаёт только PATCH карточки
    const parentUserId = email ? (await organizationsApi.addParent(org, email)).userId : parents.parentUserId
    if ((athlete.value.parents || []).some(p => p.parentUserId === parentUserId)) {
      parents.error = 'Этот родитель уже привязан к спортсмену.'
      return
    }
    athlete.value = await athletesApi.update(org, athlete.value.id, {
      parentLinks: [...currentLinks(), { parentUserId, relationship: parents.relationship.trim() || null }]
    })
    parents.open = false
  } catch (e) {
    parents.error = formErrorText(e)
  } finally {
    parents.saving = false
  }
}

async function removeParent(p) {
  if (!confirm(`Отвязать ${p.fullName || 'родителя'} от спортсмена? Он перестанет видеть его данные.`)) return
  parents.removingId = p.parentUserId
  parents.error = ''
  try {
    athlete.value = await athletesApi.update(getOrganizationId(), athlete.value.id, {
      parentLinks: currentLinks(p.parentUserId)
    })
  } catch (e) {
    parents.error = errorText(e)
  } finally {
    parents.removingId = null
  }
}

// ─────────── Аккаунт спортсмена (userId карточки) ───────────
const account = reactive({ open: false, saving: false, error: '', email: '' })

function openAccount() {
  Object.assign(account, { open: true, saving: false, error: '', email: '' })
}

// Одному аккаунту — одна карточка в организации: проверяем до PATCH
async function cardOfUser(org, userId) {
  const list = await fetchAll(params => athletesApi.list(org, params))
  return list.find(a => a.userId === userId) || null
}

async function submitAccount() {
  if (account.saving) return
  account.error = ''
  const email = account.email.trim()
  if (!email || !EMAIL_RE.test(email)) {
    account.error = 'Укажите email зарегистрированного аккаунта спортсмена.'
    return
  }
  account.saving = true
  try {
    const org = getOrganizationId()
    const member = await organizationsApi.addAthlete(org, email)
    if (member.userId !== athlete.value.userId) {
      const owner = await cardOfUser(org, member.userId)
      if (owner && owner.id !== athlete.value.id) {
        account.error = `Этот аккаунт уже привязан к карточке «${fullName(owner)}».`
        return
      }
      athlete.value = await athletesApi.update(org, athlete.value.id, { userId: member.userId })
    }
    account.open = false
  } catch (e) {
    account.error = formErrorText(e)
  } finally {
    account.saving = false
  }
}

async function unlinkAccount() {
  if (!confirm('Отвязать аккаунт от карточки? Спортсмен перестанет видеть свою карточку, расписание и начисления.')) return
  account.saving = true
  account.error = ''
  try {
    athlete.value = await athletesApi.update(getOrganizationId(), athlete.value.id, { userId: null })
    account.open = false
  } catch (e) {
    account.error = errorText(e)
  } finally {
    account.saving = false
  }
}

// ─────────── Общее ───────────
function plural(n, one, few, many) {
  const mod10 = n % 10
  const mod100 = n % 100
  if (mod10 === 1 && mod100 !== 11) return one
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few
  return many
}

// Ошибка формы: имена полей из fieldErrors — по-русски
function formErrorText(e) {
  const fieldErrors = (e?.fieldErrors || []).map(f => ({ ...f, field: FIELD_LABELS[f.field] || f.field }))
  return errorText({ status: e?.status, message: e?.message, fieldErrors })
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace {
  flex: 1; padding: 28px 32px 32px;
  display: flex; flex-direction: column; gap: 24px;
  overflow-y: auto;
}

.split-view {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 24px;
  align-items: start;
}
@media (max-width: 1024px) {
  .split-view { grid-template-columns: 1fr; }
}

.profile-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column;
  align-items: center; gap: 16px;
}
.avatar-large {
  width: 100px; height: 100px; background: #E9F7D5;
  border-radius: 999px; display: flex;
  justify-content: center; align-items: center;
  font-size: 32px; font-weight: 700; color: #2D5B24;
}
.profile-card h2 { font-size: 20px; font-weight: 700; color: #152421; text-align: center; }
.tag {
  padding: 4px 12px; background: #E9F7D5;
  border-radius: 999px; font-size: 12px;
  font-weight: 700; color: #2E8B57;
  text-align: center;
}
.divider { width: 100%; height: 1px; background: #E3EAE8; }

.meta-list { width: 100%; display: flex; flex-direction: column; gap: 12px; }
.meta-item { display: flex; flex-direction: column; gap: 4px; }
.meta-label {
  font-size: 11px; font-weight: 700; color: #98A6A2;
  text-transform: uppercase;
}
.meta-value { font-size: 14px; font-weight: 600; color: #152421; overflow-wrap: anywhere; }
.meta-value.muted { color: #98A6A2; font-weight: 500; }
.meta-value.ok { color: #2E8B57; }
.meta-value.warn { color: #B07F00; }
.meta-value.bad { color: #D64545; }
.meta-value.note { font-weight: 500; white-space: pre-line; }

.profile-actions { width: 100%; display: flex; flex-wrap: wrap; gap: 8px; }

.right-content { display: flex; flex-direction: column; gap: 20px; min-width: 0; }

.tabs { display: flex; gap: 12px; flex-wrap: wrap; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }

.card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }

.metrics-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}
.metric-card {
  background: #F4F7F8; border-radius: 12px;
  padding: 16px; display: flex; flex-direction: column; gap: 8px;
}
.metric-card.clickable {
  border: 2px solid transparent; text-align: left;
  font-family: inherit; cursor: pointer;
}
.metric-card.selected { border-color: #102522; }
.metric-label {
  font-size: 12px; font-weight: 700; color: #98A6A2;
  text-transform: uppercase;
}
.metric-value { font-size: 22px; font-weight: 700; color: #152421; }
.metric-trend { font-size: 13px; font-weight: 600; }
.metric-trend.up { color: #2E8B57; }
.metric-trend.neutral { color: #6D7D79; }
.metric-trend.down { color: #D64545; }

.chart-block { display: flex; flex-direction: column; gap: 12px; }
.chart-block h4 { font-size: 14px; font-weight: 700; color: #152421; }

.bars-chart {
  display: flex; justify-content: center;
  align-items: flex-end; gap: 32px;
  height: 180px; padding: 20px;
  background: #F4F7F8; border-radius: 12px;
  overflow-x: auto;
}
.bar-item {
  display: flex; flex-direction: column;
  align-items: center; gap: 8px;
}
.bar-value { font-size: 12px; font-weight: 700; color: #152421; white-space: nowrap; }
.bar {
  width: 40px; border-radius: 6px 6px 0 0;
  transition: height 0.3s;
}
.bar-label { font-size: 11px; color: #6D7D79; }

/* Списки вкладок */
.row-list { display: flex; flex-direction: column; }
.row-item {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
  width: 100%; padding: 12px 0;
  background: none; border-bottom: 1px solid #E3EAE8;
  text-align: left; font-family: inherit; color: inherit;
}
.row-item:last-child { border-bottom: none; }
.row-item.clickable { cursor: pointer; }
.row-item.clickable:hover .row-title { color: #2E8B57; }
.row-main { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.row-title { font-size: 14px; font-weight: 700; color: #152421; }
.row-sub { font-size: 12px; color: #6D7D79; overflow-wrap: anywhere; }
.row-side { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 8px; flex-shrink: 0; }
.link-row {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
  padding: 8px 0; border-bottom: 1px solid #E3EAE8;
}

.status-badge {
  display: inline-block; padding: 4px 12px;
  border-radius: 999px; font-size: 12px; font-weight: 700;
  white-space: nowrap;
}
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF8E6; color: #B07F00; }
.status-red { background: #FCE2E5; color: #D64545; }
.status-blue { background: #DDECFB; color: #3B82F6; }
.status-gray { background: #EEF1F0; color: #888888; }

.action-btn {
  display: inline-flex; align-items: center; justify-content: center;
  padding: 8px 14px; background: white;
  border: 1px solid #E3EAE8; border-radius: 10px;
  font-size: 13px; font-weight: 600; color: #152421;
  text-decoration: none; white-space: nowrap; cursor: pointer;
}
.action-btn:hover { border-color: #102522; }
.action-btn.danger { color: #D64545; }
.action-btn:disabled { opacity: 0.5; cursor: default; }
.self-start { align-self: flex-start; }

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
.modal-text { font-size: 14px; color: #152421; line-height: 1.5; }
.form-hint { font-size: 12px; color: #6D7D79; line-height: 1.5; }
.form-error { font-size: 13px; color: #D64545; }
</style>
