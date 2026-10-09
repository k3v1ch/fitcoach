<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Прогресс спортсменов"
        subtitle="Динамика результатов и выполнения нормативов"
        v-model="searchQuery"
        search-placeholder="Поиск спортсмена..."
      >
        <template #actions>
          <BaseButton v-if="canWrite" :disabled="!selectedAthlete" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Внести результат
          </BaseButton>
        </template>
      </PageHeader>

      <!-- Выбор группы и спортсмена -->
      <div class="selector-bar">
        <div class="selector">
          <span>Выбор группы:</span>
          <select v-model="selectedGroup" aria-label="Группа">
            <option value="">Все группы</option>
            <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
        </div>
        <div class="selector">
          <span>Спортсмен:</span>
          <select v-model="selectedAthlete" aria-label="Спортсмен" :disabled="!athleteOptions.length">
            <option v-if="!athleteOptions.length" value="">{{ athletesLoading ? 'Загрузка…' : 'Нет спортсменов' }}</option>
            <option v-for="a in athleteOptions" :key="a.id" :value="a.id">
              {{ fullName(a) }}{{ a.status === 'ARCHIVED' ? ' (в архиве)' : '' }}
            </option>
          </select>
        </div>
        <span v-if="selectorError" class="selector-error">{{ selectorError }}</span>
      </div>

      <div v-if="pageState" class="card">
        <StateBlock :kind="pageState.kind" :message="pageState.message" />
      </div>

      <template v-else>
        <!-- Карточки метрик -->
        <div class="metrics-strip">
          <StatCard label="Нормативы" :value="standardsCard.value" :trend="standardsCard.trend" :subtext="standardsCard.subtext" />
          <StatCard label="Динамика результатов" :value="dynamicsCard.value" :subtext="dynamicsCard.subtext" />
          <StatCard label="Лучший результат" :value="bestCard.value" :trend="bestCard.trend" :subtext="bestCard.subtext" />
        </div>

        <!-- График + Нормативы -->
        <div class="content-grid">
          <div class="card chart-card">
            <div class="card-head">
              <h3>График результатов</h3>
              <select v-if="series.length" v-model="chartKey" class="metric-select" aria-label="Показатель на графике">
                <option v-for="s in series" :key="s.key" :value="s.key">{{ s.name }}, {{ s.unit }}</option>
              </select>
            </div>
            <StateBlock v-if="!chartBars.length" kind="empty" message="Результатов пока нет" />
            <div v-else class="bars-chart">
              <div v-for="bar in chartBars" :key="bar.key" class="bar-item" :title="bar.title">
                <span class="bar-value">{{ bar.value }}</span>
                <div class="bar" :style="{ height: bar.height + 'px' }"></div>
                <span class="bar-label">{{ bar.label }}</span>
              </div>
            </div>
          </div>

          <div class="card norms-card">
            <h3>Нормативы</h3>
            <StateBlock v-if="!standardRows.length" kind="empty" message="Нормативы ещё не внесены" />
            <div v-else class="table-scroll">
              <table class="norms-table">
                <thead>
                  <tr>
                    <th>ПОКАЗАТЕЛЬ</th>
                    <th>НОРМА</th>
                    <th>ФАКТ</th>
                    <th>СТАТУС</th>
                    <th>ДАТА</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="norm in standardRows" :key="norm.id" :title="norm.comment || ''">
                    <td>{{ norm.name }}</td>
                    <td>{{ norm.targetText }}</td>
                    <td>{{ norm.resultText || '—' }}</td>
                    <td :class="standardClass(norm.status)">{{ label('standardStatus', norm.status) }}</td>
                    <td>{{ formatDateShort(norm.assessedOn) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Результаты + разряды -->
        <div class="content-grid secondary">
          <div class="card">
            <h3>Результаты</h3>
            <StateBlock
              v-if="!resultRows.length"
              kind="empty"
              :message="canWrite ? 'Результатов пока нет — внесите первый' : 'Результатов пока нет'"
            />
            <div v-else class="table-scroll">
              <table class="norms-table">
                <thead>
                  <tr>
                    <th>ДАТА</th>
                    <th>ПОКАЗАТЕЛЬ</th>
                    <th>РЕЗУЛЬТАТ</th>
                    <th>КОММЕНТАРИЙ</th>
                    <th v-if="canWrite"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="r in resultRows" :key="r.id">
                    <td>{{ formatDateShort(r.measuredOn) }}</td>
                    <td>{{ r.metricName }}</td>
                    <td>
                      {{ formatNumber(r.value) }} {{ r.unit }}
                      <span v-if="r.isPersonalBest" class="pb-badge">рекорд</span>
                    </td>
                    <td class="muted">{{ r.comment || '—' }}</td>
                    <td v-if="canWrite" class="row-actions">
                      <button class="icon-btn" title="Исправить" aria-label="Исправить результат" @click="openEdit(r)">
                        <BaseIcon name="edit" :size="14" color="#152421" />
                      </button>
                      <button
                        class="icon-btn"
                        title="Удалить"
                        aria-label="Удалить результат"
                        :disabled="deletingId === r.id"
                        @click="removeResult(r)"
                      >
                        <BaseIcon name="close" :size="12" color="#D64545" />
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-if="deleteError" class="form-error">{{ deleteError }}</p>
          </div>

          <div class="card">
            <h3>Разряды</h3>
            <StateBlock v-if="!rankRows.length" kind="empty" message="Разряды ещё не присвоены" />
            <div v-else class="table-scroll">
              <table class="norms-table">
                <thead>
                  <tr>
                    <th>РАЗРЯД</th>
                    <th>ВИД СПОРТА</th>
                    <th>ПРИСВОЕН</th>
                    <th>ДЕЙСТВУЕТ ДО</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="rank in rankRows" :key="rank.id" :title="rank.comment || ''">
                    <td>{{ rank.name }}</td>
                    <td>{{ sportTypeName(rank.sportTypeId) }}</td>
                    <td>{{ formatDateShort(rank.assignedOn) }}</td>
                    <td :class="{ red: rankExpired(rank) }">{{ rank.validUntil ? formatDateShort(rank.validUntil) : '—' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>

      <!-- Внесение и исправление результата -->
      <BaseModal
        v-model="editor.open"
        :title="editor.id ? 'Исправить результат' : 'Новый результат'"
        :submit-label="editor.saving ? 'Сохранение…' : 'Сохранить'"
        @submit="submitResult"
      >
        <p class="modal-text">{{ editor.athleteName }}</p>
        <div class="field">
          <label class="field-label" for="result-metric">Показатель</label>
          <input
            id="result-metric"
            v-model="editor.metricName"
            class="field-control"
            list="result-metric-names"
            placeholder="Например: бег 60 м"
          />
          <datalist id="result-metric-names">
            <option v-for="name in metricNames" :key="name" :value="name" />
          </datalist>
        </div>
        <div class="field-row">
          <div class="field">
            <label class="field-label" for="result-value">Значение</label>
            <input id="result-value" v-model="editor.value" class="field-control" inputmode="decimal" placeholder="Например: 9.8" />
          </div>
          <div class="field">
            <label class="field-label" for="result-unit">Единица</label>
            <input id="result-unit" v-model="editor.unit" class="field-control" list="result-units" placeholder="с, м, раз" />
            <datalist id="result-units">
              <option v-for="unit in unitOptions" :key="unit" :value="unit" />
            </datalist>
          </div>
        </div>
        <BaseInput id="result-date" v-model="editor.measuredOn" type="date" label="Дата замера" />
        <div class="field">
          <label class="field-label" for="result-comment">Комментарий (необязательно)</label>
          <textarea id="result-comment" v-model="editor.comment" class="field-control field-textarea" rows="3"></textarea>
        </div>
        <label class="check-row">
          <input v-model="editor.isPersonalBest" type="checkbox" />
          Личный рекорд
        </label>
        <p v-if="editor.error" class="form-error">{{ editor.error }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import StatCard from '../../components/layout/StatCard.vue'
import { athletesApi } from '../../api/athletes'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { getOrganizationId, hasPermission, hasRole } from '../../utils/session'
import { errorText, formatDateShort, fullName, parseDate, toIsoDate } from '../../utils/format'
import { label } from '../../utils/labels'

const CHART_POINTS = 8
const MAX_PAGES = 20
const COMMON_UNITS = ['с', 'мин', 'м', 'см', 'кг', 'раз']
const NUMBER_RE = /^-?\d+(\.\d+)?$/
const FIELD_LABELS = {
  metricName: 'Показатель', value: 'Значение', unit: 'Единица', measuredOn: 'Дата замера',
  comment: 'Комментарий', isPersonalBest: 'Личный рекорд'
}

const route = useRoute()
// Результаты вносит только тренер с progress.write (сервер проверяет то же)
const canWrite = computed(() => hasRole('TRAINER') && hasPermission('progress.write'))

// Все страницы списка (size ≤ 100), последовательно
async function fetchAll(request, params = {}) {
  const first = await request({ ...params, page: 0, size: 100 })
  const items = [...(first.items || [])]
  const pages = Math.min(first.totalPages || 1, MAX_PAGES)
  for (let page = 1; page < pages; page++) {
    const next = await request({ ...params, page, size: 100 })
    items.push(...(next.items || []))
  }
  return items
}

// ─────────── Группа и спортсмен ───────────
const groups = ref([])
const groupsError = ref('')
const selectedGroup = ref('')
const roster = ref(null) // id спортсменов выбранной группы; null — все
const rosterLoading = ref(false)
const rosterError = ref('')

const searchQuery = ref('')
const athletes = ref([])
const athletesLoading = ref(true)
const athletesError = ref('')
const queryAthleteId = () => (typeof route.query.athleteId === 'string' ? route.query.athleteId : '')
const selectedAthlete = ref(queryAthleteId())

const sportTypes = ref([])

let athletesSeq = 0
async function loadAthletes() {
  const seq = ++athletesSeq
  athletesLoading.value = true
  athletesError.value = ''
  try {
    const list = await fetchAll(params => athletesApi.list(getOrganizationId(), params), {
      q: searchQuery.value.trim() || undefined
    })
    if (seq !== athletesSeq) return
    athletes.value = list
  } catch (e) {
    if (seq !== athletesSeq) return
    athletes.value = []
    athletesError.value = errorText(e)
  } finally {
    if (seq === athletesSeq) athletesLoading.value = false
  }
}

async function loadGroups() {
  try {
    const list = await fetchAll(params => groupsApi.list(getOrganizationId(), params), { status: 'ACTIVE' })
    groups.value = list.sort((a, b) => (a.name || '').localeCompare(b.name || '', 'ru'))
  } catch (e) {
    groups.value = []
    groupsError.value = `Группы: ${errorText(e)}`
  }
}

async function loadSportTypes() {
  try {
    sportTypes.value = await fetchAll(params => dictionariesApi.list(getOrganizationId(), 'sport-types', params))
  } catch (_) {
    sportTypes.value = [] // справочник не обязателен: вид спорта покажем прочерком
  }
}

// В Athlete нет групп: состав группы — из GroupDetail
let rosterSeq = 0
watch(selectedGroup, async id => {
  const seq = ++rosterSeq
  rosterError.value = ''
  if (!id) {
    roster.value = null
    rosterLoading.value = false
    return
  }
  rosterLoading.value = true
  try {
    const detail = await groupsApi.get(getOrganizationId(), id)
    if (seq !== rosterSeq) return
    const list = Array.isArray(detail?.athletes) ? detail.athletes : (detail?.athletes?.items || [])
    roster.value = new Set(list.map(a => a.athleteId))
  } catch (e) {
    if (seq !== rosterSeq) return
    roster.value = new Set()
    rosterError.value = `Состав группы: ${errorText(e)}`
  } finally {
    if (seq === rosterSeq) rosterLoading.value = false
  }
})

const athleteOptions = computed(() => {
  const list = roster.value ? athletes.value.filter(a => roster.value.has(a.id)) : athletes.value
  return [...list].sort((a, b) => {
    if (a.status !== b.status) return a.status === 'ACTIVE' ? -1 : 1
    return fullName(a).localeCompare(fullName(b), 'ru')
  })
})

// Выбранный спортсмен выпал из списка (поиск, группа) — берём первого
watch(athleteOptions, list => {
  if (!list.some(a => a.id === selectedAthlete.value)) selectedAthlete.value = list[0]?.id || ''
})

const selectorError = computed(() => [groupsError.value, rosterError.value].filter(Boolean).join(' · '))

let searchTimer = null
watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadAthletes, 300)
})

// Переход из карточки спортсмена: /trainer/progress?athleteId=…
watch(() => route.query.athleteId, () => {
  const id = queryAthleteId()
  if (id && id !== selectedAthlete.value) {
    selectedGroup.value = ''
    selectedAthlete.value = id
  }
})

onMounted(() => {
  loadAthletes()
  loadGroups()
  loadSportTypes()
})
onBeforeUnmount(() => clearTimeout(searchTimer))

// ─────────── Результаты, нормативы, разряды спортсмена ───────────
const data = reactive({ loading: false, error: '', results: [], standards: [], ranks: [] })

let dataSeq = 0
async function loadProgress(silent = false) {
  const seq = ++dataSeq
  const id = selectedAthlete.value
  data.error = ''
  if (!id) {
    Object.assign(data, { loading: false, results: [], standards: [], ranks: [] })
    return
  }
  if (!silent) data.loading = true
  try {
    const org = getOrganizationId()
    const [results, standards, ranks] = await Promise.all([
      fetchAll(params => athletesApi.results(org, id, params)),
      fetchAll(params => athletesApi.standards(org, id, params)),
      fetchAll(params => athletesApi.ranks(org, id, params))
    ])
    if (seq !== dataSeq) return
    Object.assign(data, { results, standards, ranks })
  } catch (e) {
    if (seq !== dataSeq) return
    Object.assign(data, { results: [], standards: [], ranks: [] })
    data.error = errorText(e)
  } finally {
    if (seq === dataSeq) data.loading = false
  }
}

watch(selectedAthlete, () => loadProgress(), { immediate: true })

const pageState = computed(() => {
  if (athletesError.value) return { kind: 'error', message: athletesError.value }
  if (!athleteOptions.value.length) {
    if (athletesLoading.value || rosterLoading.value) return { kind: 'loading', message: '' }
    const filtered = searchQuery.value.trim() || selectedGroup.value
    return {
      kind: 'empty',
      message: filtered
        ? 'Спортсмены не найдены — измените поиск или группу'
        : 'Спортсменов пока нет — добавьте их в разделе «Спортсмены»'
    }
  }
  if (data.loading) return { kind: 'loading', message: '' }
  if (data.error) return { kind: 'error', message: data.error }
  return null
})

// ─────────── Вычисляемые ───────────
const formatNumber = (value, digits = 3) =>
  Number(value).toLocaleString('ru-RU', { maximumFractionDigits: digits })

function shortDate(value) {
  const d = parseDate(value)
  if (!d || isNaN(d)) return '—'
  return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`
}

const todayKey = () => toIsoDate(new Date())

// Ряды по показателю и единице; точки — по возрастанию даты замера (так их отдаёт сервер)
const series = computed(() => {
  const map = new Map()
  for (const r of data.results) {
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

const chartKey = ref('')
const chartSeries = computed(() => series.value.find(s => s.key === chartKey.value) || series.value[0] || null)
watch(series, list => {
  if (!list.some(s => s.key === chartKey.value)) chartKey.value = list[0]?.key || ''
})

const chartBars = computed(() => {
  const s = chartSeries.value
  if (!s) return []
  const points = s.points.slice(-CHART_POINTS)
  const values = points.map(p => Number(p.value))
  const min = Math.min(...values)
  const max = Math.max(...values)
  return points.map(p => ({
    key: p.id,
    value: formatNumber(p.value),
    height: max === min ? 64 : Math.round(20 + ((Number(p.value) - min) / (max - min)) * 90),
    label: shortDate(p.measuredOn),
    title: `${formatDateShort(p.measuredOn)}: ${formatNumber(p.value)} ${s.unit}${p.comment ? ` — ${p.comment}` : ''}`
  }))
})

const resultRows = computed(() => [...data.results].sort((a, b) =>
  b.measuredOn.localeCompare(a.measuredOn) || (b.createdAt || '').localeCompare(a.createdAt || '')))

const standardRows = computed(() => [...data.standards].sort((a, b) => (b.assessedOn || '').localeCompare(a.assessedOn || '')))

const rankRows = computed(() => [...data.ranks].sort((a, b) => (b.assignedOn || '').localeCompare(a.assignedOn || '')))

function rankExpired(rank) {
  return !!rank.validUntil && rank.validUntil < todayKey()
}

function sportTypeName(id) {
  return sportTypes.value.find(t => t.id === id)?.name || '—'
}

function standardClass(status) {
  return status === 'MET' ? 'green' : status === 'NOT_MET' ? 'red' : ''
}

// Нормативы вносит организация (через API не редактируются) — здесь только итог
const standardsCard = computed(() => {
  const list = data.standards
  if (!list.length) return { value: '—', trend: '', subtext: 'Нормативы ещё не внесены' }
  const met = list.filter(s => s.status === 'MET').length
  const notMet = list.filter(s => s.status === 'NOT_MET').length
  const lastDate = list.map(s => s.assessedOn).filter(Boolean).sort().pop()
  const parts = []
  if (notMet) parts.push(`не выполнено: ${notMet}`)
  if (lastDate) parts.push(`оценка ${formatDateShort(lastDate)}`)
  return { value: `${met} из ${list.length}`, trend: 'выполнено', subtext: parts.length ? `· ${parts.join(' · ')}` : ' ' }
})

// Изменение выбранного на графике показателя от первого замера к последнему, без оценки «лучше/хуже»
const dynamicsCard = computed(() => {
  const s = chartSeries.value
  if (!s) return { value: '—', subtext: 'Результатов пока нет' }
  if (s.points.length < 2) return { value: '—', subtext: `${s.name}: нужно хотя бы два замера` }
  const first = s.points[0]
  const last = s.points[s.points.length - 1]
  const a = Number(first.value)
  const b = Number(last.value)
  const diff = b - a
  const sign = diff > 0 ? '+' : diff < 0 ? '−' : ''
  const value = a !== 0
    ? `${sign}${formatNumber(Math.abs(diff / a) * 100, 1)}%`
    : `${sign}${formatNumber(Math.abs(diff))} ${s.unit}`
  return {
    value,
    subtext: `${s.name}: ${formatNumber(a)} → ${formatNumber(b)} ${s.unit} · первый замер ${formatDateShort(first.measuredOn)}`
  }
})

// Личный рекорд тренер отмечает вручную (isPersonalBest)
const bestCard = computed(() => {
  const records = data.results.filter(r => r.isPersonalBest)
  if (!records.length) return { value: '—', trend: '', subtext: 'Рекорды пока не отмечены' }
  // Последний по дате замера, при равной дате — последний внесённый
  const stamp = r => `${r.measuredOn}|${r.createdAt || ''}`
  const best = records.reduce((x, y) => (stamp(y) >= stamp(x) ? y : x))
  return {
    value: best.metricName,
    trend: 'Личный рекорд',
    subtext: `${formatNumber(best.value)} ${best.unit} · ${formatDateShort(best.measuredOn)}`
  }
})

const metricNames = computed(() => [...new Set(data.results.map(r => r.metricName))].sort((a, b) => a.localeCompare(b, 'ru')))
const unitOptions = computed(() => [...new Set([...data.results.map(r => r.unit), ...COMMON_UNITS])])

// ─────────── Внесение, исправление, удаление ───────────
const editor = reactive({
  open: false, saving: false, error: '', id: null, athleteId: '', athleteName: '',
  metricName: '', value: '', unit: '', measuredOn: '', comment: '', isPersonalBest: false
})

function athleteNameById(id) {
  const a = athletes.value.find(x => x.id === id)
  return a ? fullName(a) : ''
}

function openCreate() {
  if (!selectedAthlete.value) return
  Object.assign(editor, {
    open: true, saving: false, error: '', id: null,
    athleteId: selectedAthlete.value, athleteName: athleteNameById(selectedAthlete.value),
    metricName: '', value: '', unit: '', measuredOn: todayKey(), comment: '', isPersonalBest: false
  })
}

function openEdit(r) {
  Object.assign(editor, {
    open: true, saving: false, error: '', id: r.id,
    athleteId: r.athleteId, athleteName: athleteNameById(r.athleteId),
    metricName: r.metricName, value: String(r.value), unit: r.unit, measuredOn: r.measuredOn,
    comment: r.comment || '', isPersonalBest: !!r.isPersonalBest
  })
}

async function submitResult() {
  if (editor.saving) return
  editor.error = ''
  const metricName = editor.metricName.trim()
  const unit = editor.unit.trim()
  const raw = String(editor.value).trim().replace(/\s/g, '').replace(',', '.')
  if (!metricName) {
    editor.error = 'Укажите показатель.'
    return
  }
  if (!NUMBER_RE.test(raw)) {
    editor.error = 'Значение — число, например 9.8 или 24.'
    return
  }
  if (!unit) {
    editor.error = 'Укажите единицу измерения.'
    return
  }
  if (!editor.measuredOn) {
    editor.error = 'Укажите дату замера.'
    return
  }
  if (editor.measuredOn > todayKey()) {
    editor.error = 'Дата замера не может быть в будущем.'
    return
  }
  const body = {
    metricName,
    value: Number(raw),
    unit,
    measuredOn: editor.measuredOn,
    comment: editor.comment.trim() || null,
    isPersonalBest: editor.isPersonalBest
  }
  editor.saving = true
  try {
    const org = getOrganizationId()
    if (editor.id) await athletesApi.updateResult(org, editor.athleteId, editor.id, body)
    else await athletesApi.addResult(org, editor.athleteId, body)
    editor.open = false
    if (editor.athleteId === selectedAthlete.value) await loadProgress(true)
  } catch (e) {
    editor.error = formErrorText(e)
  } finally {
    editor.saving = false
  }
}

const deletingId = ref(null)
const deleteError = ref('')

async function removeResult(r) {
  if (!confirm(`Удалить результат «${r.metricName}» от ${formatDateShort(r.measuredOn)}? Действие нельзя отменить.`)) return
  deletingId.value = r.id
  deleteError.value = ''
  try {
    await athletesApi.deleteResult(getOrganizationId(), r.athleteId, r.id)
    await loadProgress(true)
  } catch (e) {
    deleteError.value = errorText(e)
  } finally {
    deletingId.value = null
  }
}

// Ошибка формы: имена полей из fieldErrors — по-русски
function formErrorText(e) {
  const fieldErrors = (e?.fieldErrors || []).map(f => ({ ...f, field: FIELD_LABELS[f.field] || f.field }))
  return errorText({ status: e?.status, message: e?.message, fieldErrors })
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.selector-bar {
  display: flex; gap: 24px; flex-wrap: wrap;
  background: white; padding: 16px; border-radius: 12px;
  border: 1px solid #E3EAE8;
}
.selector { display: flex; align-items: center; gap: 8px; }
.selector span { font-size: 14px; color: #6D7D79; }
.selector select {
  padding: 8px 12px; border: 1px solid #E3EAE8;
  border-radius: 8px; background: #F4F7F8;
  font-size: 14px; font-weight: 600; color: #152421;
  cursor: pointer; outline: none;
}
.selector select:disabled { cursor: default; opacity: 0.7; }
.selector-error { align-self: center; font-size: 13px; color: #D64545; }

.metrics-strip {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.content-grid {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.content-grid.secondary { grid-template-columns: 3fr 2fr; }
@media (max-width: 1024px) {
  .content-grid, .content-grid.secondary { grid-template-columns: 1fr; }
}

.card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
  min-width: 0;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.metric-select {
  padding: 6px 10px; border: 1px solid #E3EAE8;
  border-radius: 8px; background: #F4F7F8;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer; outline: none; max-width: 100%;
}

.bars-chart {
  display: flex; justify-content: space-between;
  align-items: flex-end; gap: 12px;
  height: 200px; padding: 16px;
  background: #F4F7F8; border-radius: 12px;
}
.bar-item {
  display: flex; flex-direction: column;
  align-items: center; gap: 8px;
  flex: 1; min-width: 0;
}
.bar-value { font-size: 11px; font-weight: 700; color: #152421; white-space: nowrap; }
.bar {
  width: 12px; background: #102522;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
}
.bar-label { font-size: 11px; color: #6D7D79; }

.table-scroll { overflow-x: auto; }
.norms-table {
  width: 100%; border-collapse: collapse;
}
.norms-table th {
  text-align: left; font-size: 12px; font-weight: 700;
  color: #6D7D79; text-transform: uppercase;
  padding-bottom: 8px;
}
.norms-table td {
  padding: 8px 0; font-size: 14px; color: #152421;
  border-bottom: 1px solid #E3EAE8;
}
.norms-table th:not(:last-child), .norms-table td:not(:last-child) { padding-right: 12px; }
.norms-table .green { color: #2E8B57; font-weight: 700; }
.norms-table .red { color: #D64545; font-weight: 700; }
.norms-table .muted { color: #6D7D79; }

.pb-badge {
  display: inline-block; margin-left: 6px; padding: 2px 8px;
  border-radius: 999px; background: #E9F7D5; color: #2E8B57;
  font-size: 11px; font-weight: 700;
}
.row-actions { white-space: nowrap; text-align: right; }
.icon-btn {
  width: 28px; height: 28px; margin-left: 4px;
  display: inline-flex; justify-content: center; align-items: center;
  background: #F4F7F8; border: 1px solid #E3EAE8; border-radius: 8px;
  cursor: pointer;
}
.icon-btn:disabled { opacity: 0.5; cursor: default; }

/* Форма в модальном окне — в стиле BaseInput */
.field { display: flex; flex-direction: column; gap: 6px; width: 100%; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field-label { font-size: 12px; color: var(--color-gray-text); font-weight: 500; }
.field-control {
  width: 100%; min-height: 40px; padding: 8px 12px;
  border: 1px solid var(--color-gray-border); border-radius: 8px;
  font-size: 14px; font-family: inherit; color: var(--color-dark);
  background: var(--color-white); outline: none;
}
.field-control:focus { border-color: var(--color-primary); }
.field-textarea { resize: vertical; }
.check-row { display: flex; align-items: center; gap: 10px; font-size: 14px; color: #152421; cursor: pointer; }
.modal-text { font-size: 14px; font-weight: 600; color: #152421; }
.form-error { font-size: 13px; color: #D64545; }
</style>
