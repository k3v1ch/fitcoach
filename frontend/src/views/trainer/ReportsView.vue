<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Отчётность"
        subtitle="Формирование и выгрузка аналитических отчетов"
        :show-bell="true"
        v-model="searchQuery"
        search-placeholder="Поиск отчёта..."
      />

      <div class="period-bar">
        <span class="period-label">Период</span>
        <input v-model="period.from" type="date" class="period-input" aria-label="Начало периода" :max="period.to || undefined" />
        <span class="period-sep">—</span>
        <input v-model="period.to" type="date" class="period-input" aria-label="Конец периода" :min="period.from || undefined" />
        <span v-if="periodError" class="period-error">{{ periodError }}</span>
      </div>

      <div v-if="typesLoading" class="state-card">
        <StateBlock kind="loading" />
      </div>
      <div v-else-if="typesError" class="state-card">
        <StateBlock kind="error" :message="typesError" />
      </div>
      <div v-else-if="!visibleReports.length" class="state-card">
        <StateBlock kind="empty" :message="searchQuery.trim() ? `Ничего не найдено по запросу «${searchQuery.trim()}»` : 'Отчёты недоступны для вашей роли'" />
      </div>

      <div v-else class="reports-grid">
        <div v-for="report in visibleReports" :key="report.code" class="report-card">
          <div class="report-header">
            <div class="report-icon">
              <BaseIcon name="file-text" :size="18" color="#2E8B57" />
            </div>
            <div class="report-meta">
              <h3>{{ report.title }}</h3>
              <span class="report-code">{{ report.code }}</span>
            </div>
          </div>
          <p class="report-desc">{{ report.description }}</p>
          <div v-if="report.type" class="report-form">
            <select v-model="formats[report.type]" class="format-select" :aria-label="`Формат отчёта «${report.title}»`">
              <option value="VIEW">Таблица на экране</option>
              <option v-for="f in exportFormats(report)" :key="f" :value="f">Файл {{ f }}</option>
            </select>
            <button class="build-btn" :disabled="!!busy[report.type]" @click="handleBuild(report)">
              {{ busy[report.type] ? 'Формируем…' : 'Сформировать' }}
            </button>
          </div>
          <p v-else class="report-soon">Отчёт появится позже</p>
          <p v-if="report.type && cardErrors[report.type]" class="report-error">{{ cardErrors[report.type] }}</p>
        </div>
      </div>

      <!-- Сформированный отчёт -->
      <section v-if="result.type" ref="resultEl" class="report-result">
        <div class="result-header">
          <div class="result-title">
            <h2>{{ result.title }}</h2>
            <p class="result-meta">{{ resultMeta }}</p>
          </div>
          <div class="result-actions">
            <button v-if="canExport(result.type)" class="build-btn" :disabled="!!busy[result.type]" @click="download(result.type)">
              Скачать CSV
            </button>
            <button class="close-btn" aria-label="Закрыть отчёт" @click="closeResult">
              <BaseIcon name="close" :size="14" color="#6D7D79" />
            </button>
          </div>
        </div>

        <StateBlock v-if="result.loading" kind="loading" />
        <StateBlock v-else-if="result.error" kind="error" :message="result.error" />
        <StateBlock v-else-if="!result.rows.length" kind="empty" message="За выбранный период данных нет" />
        <div v-else class="result-table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th v-for="column in result.columns" :key="column.key">{{ column.label }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in result.rows" :key="index">
                <td v-for="column in result.columns" :key="column.key">{{ column.format(row[column.key], row) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!result.loading && !result.error && result.totalPages > 1" class="pager">
          <button class="pager-btn" :disabled="result.page === 0" @click="loadResult(result.type, result.page - 1)">← Назад</button>
          <span class="pager-text">Страница {{ result.page + 1 }} из {{ result.totalPages }}</span>
          <button class="pager-btn" :disabled="result.page + 1 >= result.totalPages" @click="loadResult(result.type, result.page + 1)">Вперёд →</button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { financeApi } from '../../api/finance'
import { athletesApi } from '../../api/athletes'
import { trainingsApi } from '../../api/trainings'
import { getOrganizationId, hasPermission } from '../../utils/session'
import {
  errorText, formatDate, formatDateShort, formatDateTime, formatTime, formatMoney, toIsoDate, toIsoDateTime, periodRange, addDays, parseDate, fullName
} from '../../utils/format'
import { label } from '../../utils/labels'

const PAGE_SIZE = 50

// Карточки макета: тип отчёта API (GET /reports/types) или «появится позже», если такого отчёта в API нет
const CATALOG = [
  { code: 'REP-ATT', type: 'ATTENDANCE', title: 'Посещаемость', description: 'Отметки посещения тренировок за период: был, болел, не был — с причинами пропусков.' },
  { code: 'REP-PROG', type: 'PROGRESS', title: 'Прогресс', description: 'Результаты спортсменов за период: показатели, значения и личные рекорды.' },
  { code: 'REP-FIN', type: 'CHARGES', title: 'Финансовый отчёт', description: 'Начисления со сроком оплаты в периоде: суммы, оплаченная часть и статусы.' },
  { code: 'REP-TRAIN', type: 'TRAININGS', title: 'Журнал тренировок', description: 'Реестр тренировок за период: время проведения, статус и состояние отчёта тренера.' },
  { code: 'REP-GROUP', type: null, title: 'Сводка по группам', description: 'Общая статистика укомплектованности групп и спортивной занятости тренеров.' },
  { code: 'REP-NORM', type: null, title: 'Нормативы', description: 'Оценка результатов сдачи контрольных переводных нормативов по возрастам.' }
]

// ─────────── Доступные отчёты ───────────
const searchQuery = ref('')
const types = ref([])
const typesLoading = ref(true)
const typesError = ref('')

async function loadTypes() {
  typesLoading.value = true
  typesError.value = ''
  try {
    types.value = (await financeApi.types(getOrganizationId())) || []
    for (const t of types.value) if (!formats[t.type]) formats[t.type] = 'VIEW'
  } catch (e) {
    types.value = []
    typesError.value = errorText(e)
  } finally {
    typesLoading.value = false
  }
}

const reports = computed(() => {
  const byType = new Map(types.value.map(t => [t.type, t]))
  const known = CATALOG
    .filter(item => !item.type || byType.has(item.type))
    .map(item => (item.type ? { ...item, formats: byType.get(item.type).formats || [] } : item))
  // Тип, о котором экран ещё не знает, — карточка с названием из API
  const extra = types.value
    .filter(t => !CATALOG.some(item => item.type === t.type))
    .map(t => ({ code: t.type, type: t.type, title: t.name || label('reportType', t.type), description: 'Табличный отчёт за выбранный период.', formats: t.formats || [] }))
  return [...known, ...extra]
})

const visibleReports = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return reports.value
  return reports.value.filter(r => `${r.title} ${r.description} ${r.code}`.toLowerCase().includes(q))
})

// Выгрузка файла — с правом reports.export; формат из ответа API (сейчас только CSV)
const canExport = (type) => hasPermission('reports.export') &&
  (reports.value.find(r => r.type === type)?.formats || []).includes('CSV')
const exportFormats = (report) => (hasPermission('reports.export') ? report.formats : [])

const formats = reactive({})
const busy = reactive({})
const cardErrors = reactive({})

// ─────────── Период (date, включительно) ───────────
const month = periodRange('month')
const period = reactive({ from: toIsoDate(month.from), to: toIsoDate(addDays(month.to, -1)) })

const periodError = computed(() => {
  if (!period.from || !period.to) return 'Укажите обе даты периода.'
  if (period.from > period.to) return 'Начало периода позже конца.'
  return ''
})

// ─────────── Формирование ───────────
async function handleBuild(report) {
  cardErrors[report.type] = ''
  if (periodError.value) {
    cardErrors[report.type] = periodError.value
    return
  }
  if ((formats[report.type] || 'VIEW') === 'VIEW') {
    await loadResult(report.type, 0)
    await nextTick()
    resultEl.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } else {
    await download(report.type)
  }
}

const resultEl = ref(null)
const result = reactive(emptyResult())

function emptyResult() {
  return {
    type: null, title: '', from: '', to: '', loading: false, error: '',
    columns: [], rows: [], page: 0, totalPages: 0, totalElements: 0, generatedAt: null
  }
}

let resultSeq = 0
async function loadResult(type, page) {
  const seq = ++resultSeq
  const report = reports.value.find(r => r.type === type)
  Object.assign(result, {
    type, title: report?.title || label('reportType', type), from: period.from, to: period.to,
    loading: true, error: '', page
  })
  busy[type] = true
  try {
    const [table] = await Promise.all([
      financeApi.reports(getOrganizationId(), type, { from: period.from, to: period.to, page, size: PAGE_SIZE }),
      loadNames(type, period.from, period.to)
    ])
    if (seq !== resultSeq) return
    Object.assign(result, {
      columns: buildColumns(type, table.columns || []),
      rows: table.rows || [],
      page: table.page ?? page,
      totalPages: table.totalPages || 0,
      totalElements: table.totalElements || 0,
      generatedAt: table.generatedAt
    })
  } catch (e) {
    if (seq === resultSeq) {
      Object.assign(result, { columns: [], rows: [], totalPages: 0, totalElements: 0, generatedAt: null })
      result.error = errorText(e)
    }
  } finally {
    if (seq === resultSeq) result.loading = false
    busy[type] = false
  }
}

const resultMeta = computed(() => {
  const parts = [`${formatDateShort(result.from)} — ${formatDateShort(result.to)}`]
  if (!result.loading && !result.error) {
    parts.push(`${result.totalElements} ${plural(result.totalElements, 'строка', 'строки', 'строк')}`)
    if (result.generatedAt) parts.push(`сформирован ${formatDateTime(result.generatedAt)}`)
  }
  return parts.join(' · ')
})

function closeResult() {
  resultSeq++
  Object.assign(result, emptyResult())
}

// CSV (GET …/export → Blob): с BOM, чтобы Excel открыл кириллицу
async function download(type) {
  cardErrors[type] = ''
  if (periodError.value) {
    cardErrors[type] = periodError.value
    return
  }
  busy[type] = true
  try {
    const blob = await financeApi.exportReport(getOrganizationId(), type, { from: period.from, to: period.to })
    if (!(blob instanceof Blob) || blob.size === 0) {
      cardErrors[type] = 'За выбранный период данных нет — файл не сформирован.'
      return
    }
    const file = new Blob(['﻿', blob], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(file)
    const link = document.createElement('a')
    link.href = url
    link.download = `${type.toLowerCase()}_${period.from}_${period.to}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    cardErrors[type] = e?.message?.includes('REPORT_TOO_LARGE')
      ? 'Слишком большой отчёт — сузьте период.'
      : errorText(e)
  } finally {
    busy[type] = false
  }
}

// ─────────── Колонки: подписи и форматирование значений строк ReportTable ───────────
const athleteNames = ref(new Map())
const athletesLoaded = ref(false)
const trainingTitles = ref(new Map())
let trainingsRange = ''

// Имена спортсменов и названия тренировок вместо идентификаторов
async function loadNames(type, from, to) {
  const jobs = []
  if (['ATTENDANCE', 'PROGRESS', 'CHARGES'].includes(type) && !athletesLoaded.value) {
    jobs.push(fetchAll(params => athletesApi.list(getOrganizationId(), params))
      .then(items => {
        athleteNames.value = new Map(items.map(a => [a.id, fullName(a)]))
        athletesLoaded.value = true
      })
      .catch(() => {}))
  }
  const range = `${from}|${to}`
  if (type === 'ATTENDANCE' && trainingsRange !== range) {
    // Отчёт режет период по дате начала в часовом поясе сервера — берём запас в сутки с каждой стороны
    jobs.push(fetchAll(params => trainingsApi.list(getOrganizationId(), params), {
      from: toIsoDateTime(addDays(parseDate(from), -1)),
      to: toIsoDateTime(addDays(parseDate(to), 2))
    })
      .then(items => {
        trainingTitles.value = new Map(items.map(t => [t.id, t.title]))
        trainingsRange = range
      })
      .catch(() => {}))
  }
  await Promise.all(jobs)
}

const text = (v) => (v === null || v === undefined || v === '' ? '—' : String(v))
const dateTime = (v) => (v ? formatDateTime(v) : '—')
const timeOnly = (v) => (v ? formatTime(v) : '—')
const dateOnly = (v) => (v ? formatDate(v) : '—')
const money = (v) => formatMoney(v)
const athleteName = (v) => (v ? athleteNames.value.get(v) || 'Спортсмен не найден' : '—')
const number = (v) => (typeof v === 'number' ? v.toLocaleString('ru-RU') : text(v))

const COLUMNS = {
  ATTENDANCE: [
    { key: 'starts_at', label: 'Дата и время', format: dateTime },
    { key: 'training_id', label: 'Тренировка', format: (v) => trainingTitles.value.get(v) || '—' },
    { key: 'athlete_id', label: 'Спортсмен', format: athleteName },
    { key: 'attendance_status', label: 'Отметка', format: (v) => label('attendanceStatus', v) },
    { key: 'reason', label: 'Причина', format: text }
  ],
  TRAININGS: [
    { key: 'starts_at', label: 'Начало', format: dateTime },
    { key: 'ends_at', label: 'Окончание', format: timeOnly },
    { key: 'title', label: 'Тренировка', format: text },
    { key: 'status', label: 'Статус', format: (v) => label('trainingStatus', v) },
    { key: 'report_status', label: 'Отчёт тренера', format: (v) => (v ? label('reportStatus', v) : 'Нет') }
  ],
  PROGRESS: [
    { key: 'measured_on', label: 'Дата', format: dateOnly },
    { key: 'athlete_id', label: 'Спортсмен', format: athleteName },
    { key: 'metric_name', label: 'Показатель', format: text },
    { key: 'value', label: 'Результат', format: (v, row) => [number(v), row.unit].filter(x => x && x !== '—').join(' ') || '—' },
    { key: 'is_personal_best', label: 'Личный рекорд', format: (v) => (v ? 'Да' : '—') },
    { key: 'comment', label: 'Комментарий', format: text }
  ],
  CHARGES: [
    { key: 'due_on', label: 'Срок оплаты', format: dateOnly },
    { key: 'athlete_id', label: 'Спортсмен', format: athleteName },
    { key: 'title', label: 'Начисление', format: text },
    { key: 'amount', label: 'Сумма', format: money },
    { key: 'paid_amount', label: 'Оплачено', format: money },
    { key: 'status', label: 'Статус', format: (v) => label('chargeStatus', v) }
  ]
}
// Служебные колонки (идентификаторы и единицы, уже показанные рядом со значением)
const HIDDEN = new Set(['id', 'charge_id', 'training_id', 'athlete_id', 'unit'])

function buildColumns(type, apiColumns) {
  const configured = COLUMNS[type] || []
  const keys = new Set(configured.map(c => c.key))
  // Новые колонки, которых экран не знает, показываем как есть
  const extra = apiColumns
    .filter(key => !keys.has(key) && !HIDDEN.has(key))
    .map(key => ({ key, label: key, format: text }))
  return [...configured, ...extra]
}

// ─────────── Жизненный цикл ───────────
// Смена периода при открытом отчёте — пересобрать его с первой страницы
let periodTimer = null
watch(() => [period.from, period.to], () => {
  clearTimeout(periodTimer)
  if (!result.type || periodError.value) return
  periodTimer = setTimeout(() => loadResult(result.type, 0), 300)
})
onBeforeUnmount(() => clearTimeout(periodTimer))

onMounted(loadTypes)

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
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 24px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.period-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.period-label { font-size: 13px; font-weight: 700; color: #152421; }
.period-input {
  padding: 8px 12px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 13px; font-weight: 600; color: #152421;
  font-family: inherit; outline: none;
}
.period-sep { color: #98A6A2; }
.period-error { font-size: 12px; color: #D64545; }

.state-card {
  background: white; border: 1px dashed #E3EAE8;
  border-radius: 16px;
}

.reports-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.report-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 16px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.report-header { display: flex; align-items: center; gap: 12px; }
.report-icon {
  padding: 8px; background: #E9F7D5;
  border-radius: 8px; display: flex;
  justify-content: center; align-items: center;
}
.report-meta h3 { font-size: 16px; font-weight: 700; color: #152421; }
.report-code { font-size: 11px; color: #98A6A2; }
.report-desc { font-size: 13px; color: #6D7D79; line-height: 1.4; }

.report-form {
  display: flex; gap: 8px; margin-top: auto;
}
.format-select {
  flex: 1; padding: 8px 12px;
  background: #F4F7F8; border: none;
  border-radius: 8px; font-size: 12px;
  font-weight: 600; color: #152421;
  cursor: pointer; outline: none;
}
.build-btn {
  padding: 8px 16px; background: #102522;
  border: none; border-radius: 8px;
  color: #C4F000; font-size: 12px;
  font-weight: 700; cursor: pointer;
}
.build-btn:hover { opacity: 0.9; }
.build-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.report-soon {
  margin-top: auto; padding: 8px 12px;
  background: #F4F7F8; border-radius: 8px;
  font-size: 12px; font-weight: 600; color: #98A6A2; text-align: center;
}
.report-error { font-size: 12px; color: #D64545; }

/* Сформированный отчёт */
.report-result {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 16px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.result-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; }
.result-title h2 { font-size: 18px; font-weight: 700; color: #152421; }
.result-meta { font-size: 12px; color: #6D7D79; margin-top: 4px; }
.result-actions { display: flex; align-items: center; gap: 8px; }
.close-btn {
  width: 32px; height: 32px; background: #F4F7F8;
  border: none; border-radius: 16px;
  display: flex; justify-content: center; align-items: center; cursor: pointer;
}
.result-table-wrap { overflow-x: auto; }
.result-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.result-table th {
  text-align: left; padding: 8px 12px;
  font-size: 11px; font-weight: 700; color: #6D7D79; text-transform: uppercase;
  border-bottom: 1px solid #E3EAE8; white-space: nowrap;
}
.result-table td { padding: 10px 12px; color: #152421; border-bottom: 1px solid #F0F4F4; }
.pager { display: flex; justify-content: flex-end; align-items: center; gap: 12px; }
.pager-btn {
  padding: 6px 12px; background: #F4F7F8;
  border: none; border-radius: 8px;
  font-size: 12px; font-weight: 600; color: #152421; cursor: pointer;
}
.pager-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.pager-text { font-size: 12px; color: #6D7D79; }
</style>
