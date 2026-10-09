<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Финансы"
        subtitle="Управление финансовыми реестрами и платежами"
        v-model="searchQuery"
        search-placeholder="Поиск реестра..."
      >
        <template #actions>
          <BaseButton v-if="types.length" @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать реестр
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <div class="table-card">
        <div class="table-title">
          <h3>Реестры финансовых операций</h3>
          <span class="total">Всего записей: {{ filteredRegistries.length }}</span>
        </div>

        <StateBlock v-if="typesLoading" kind="loading" />
        <StateBlock v-else-if="typesError" kind="error" :message="typesError" />
        <StateBlock
          v-else-if="!types.length"
          kind="empty"
          message="Реестры недоступны: нужны права на просмотр отчётов и начислений"
        />
        <template v-else>
          <StateBlock v-if="downloadError" kind="error" :message="downloadError" />

          <div class="table-header">
            <div class="col num">№</div>
            <div class="col date">Дата</div>
            <div class="col type">Тип реестра</div>
            <div class="col period">Период выгрузки</div>
            <div class="col count">Записей</div>
            <div class="col status">Статус</div>
            <div class="col actions">Действия</div>
          </div>

          <div v-for="(reg, idx) in filteredRegistries" :key="reg.key" class="table-row">
            <div class="col num">{{ idx + 1 }}</div>
            <div class="col date">{{ reg.generatedAt ? formatDateShort(reg.generatedAt) : '—' }}</div>
            <div class="col type">{{ typeName(reg.type) }}</div>
            <div class="col period">{{ reg.periodLabel }}</div>
            <div class="col count">{{ reg.state === 'ready' ? reg.count : '—' }}</div>
            <div class="col status">
              <span class="status-badge" :class="statusClass(reg)" :title="reg.error">{{ statusLabel(reg) }}</span>
            </div>
            <div class="col actions">
              <button
                class="download-btn"
                :disabled="!canDownload(reg)"
                :title="downloadHint(reg)"
                @click="downloadRegistry(reg)"
              >{{ reg.downloading ? 'Скачивание…' : 'Скачать' }}</button>
            </div>
          </div>

          <div v-if="filteredRegistries.length === 0" class="empty-state">
            Реестров не найдено
          </div>
        </template>
      </div>

      <BaseModal v-model="showModal" title="Создать реестр" submit-label="Сформировать" @submit="handleSubmit">
        <div class="field">
          <label class="field-label" for="registry-type">Тип реестра</label>
          <select id="registry-type" v-model="form.type" class="field-control">
            <option v-for="t in types" :key="t.type" :value="t.type">{{ typeName(t.type) }}</option>
          </select>
        </div>
        <div class="field">
          <label class="field-label" for="registry-period">Период выгрузки</label>
          <select id="registry-period" v-model="form.period" class="field-control">
            <option v-for="m in monthOptions" :key="m.value" :value="m.value">{{ m.label }}</option>
            <option value="custom">Произвольный период</option>
          </select>
        </div>
        <div class="row-2">
          <BaseInput id="registry-from" v-model="form.dateFrom" type="date" label="Дата с" />
          <BaseInput id="registry-to" v-model="form.dateTo" type="date" label="По" />
        </div>
        <p class="form-hint">В реестр попадают начисления со сроком оплаты в выбранном периоде; файл скачивается в формате CSV.</p>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import Sidebar from '../../../components/layout/Sidebar.vue'
import PageHeader from '../../../components/layout/PageHeader.vue'
import FinanceTabs from '../../../components/layout/FinanceTabs.vue'
import BaseButton from '../../../components/ui/BaseButton.vue'
import BaseIcon from '../../../components/ui/BaseIcon.vue'
import BaseModal from '../../../components/ui/BaseModal.vue'
import BaseInput from '../../../components/ui/BaseInput.vue'
import StateBlock from '../../../components/ui/StateBlock.vue'
import { financeApi } from '../../../api/finance'
import { getOrganizationId, hasPermission } from '../../../utils/session'
import { errorText, formatDateShort, toIsoDate } from '../../../utils/format'
import { label } from '../../../utils/labels'

// Реестр = выгрузка отчёта GET /reports/{type}/export за период; на сервере реестры не хранятся.
// Финансовые типы отчётов — только начисления (ATTENDANCE/TRAININGS/PROGRESS живут в «Отчётах»).
const REGISTRY_TYPES = ['CHARGES']
const DEFAULT_MONTHS = 6
const MAX_ROWS = 10000 // больше — сервер отвечает REPORT_TOO_LARGE
const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь']

const searchQuery = ref('')
const canExport = computed(() => hasPermission('reports.export'))

// Месяцы для выбора периода: текущий и 11 предыдущих
const monthOptions = Array.from({ length: 12 }, (_, i) => monthPeriod(-i))

// ─────────── Каталог отчётов: какие типы доступны пользователю ───────────
const types = ref([])
const typesLoading = ref(true)
const typesError = ref('')

async function loadTypes() {
  try {
    const res = await financeApi.types(getOrganizationId())
    const list = Array.isArray(res) ? res : (res?.items || [])
    types.value = list.filter(t => REGISTRY_TYPES.includes(t.type) && (t.formats || []).includes('CSV'))
  } catch (e) {
    typesError.value = errorText(e)
  } finally {
    typesLoading.value = false
  }
}

function typeName(type) {
  return label('reportType', type)
}

// ─────────── Реестры: по умолчанию — начисления за последние месяцы, плюс созданные вручную ───────────
const registries = ref([])

function makeRegistry(type, period) {
  return reactive({
    key: `${type}:${period.from}:${period.to}`,
    type,
    from: period.from,
    to: period.to,
    periodLabel: period.label,
    state: 'loading',
    count: null,
    generatedAt: null,
    error: '',
    downloading: false
  })
}

// Число строк и время формирования — из просмотра отчёта (одна строка на страницу)
async function loadCount(reg) {
  reg.state = 'loading'
  reg.error = ''
  try {
    const table = await financeApi.reports(getOrganizationId(), reg.type, { from: reg.from, to: reg.to, page: 0, size: 1 })
    reg.count = table.totalElements ?? 0
    reg.generatedAt = table.generatedAt || null
    reg.state = 'ready'
  } catch (e) {
    reg.state = 'error'
    reg.error = errorText(e)
  }
}

function addRegistry(type, period) {
  const key = `${type}:${period.from}:${period.to}`
  const existing = registries.value.find(r => r.key === key)
  if (existing) {
    registries.value = [existing, ...registries.value.filter(r => r !== existing)]
    loadCount(existing)
    return
  }
  const reg = makeRegistry(type, period)
  registries.value = [reg, ...registries.value]
  loadCount(reg)
}

function statusLabel(reg) {
  if (reg.state === 'loading') return 'Формируется'
  if (reg.state === 'error') return 'Ошибка'
  if (reg.count === 0) return 'Нет записей'
  if (reg.count > MAX_ROWS) return 'Слишком большой'
  return 'Готов'
}

function statusClass(reg) {
  if (reg.state === 'loading') return 'status-yellow'
  if (reg.state === 'error' || reg.count > MAX_ROWS) return 'status-red'
  if (reg.count === 0) return 'status-gray'
  return 'status-green'
}

const filteredRegistries = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return registries.value
  return registries.value.filter(r =>
    typeName(r.type).toLowerCase().includes(q) ||
    r.periodLabel.toLowerCase().includes(q) ||
    statusLabel(r).toLowerCase().includes(q)
  )
})

// ─────────── Скачивание: Blob из GET /reports/{type}/export?format=CSV ───────────
const downloadError = ref('')

function canDownload(reg) {
  return canExport.value && reg.state === 'ready' && reg.count > 0 && reg.count <= MAX_ROWS && !reg.downloading
}

function downloadHint(reg) {
  if (!canExport.value) return 'Нет права на выгрузку отчётов'
  if (reg.state === 'ready' && reg.count === 0) return 'За период нет записей'
  if (reg.state === 'ready' && reg.count > MAX_ROWS) return 'Больше 10 000 строк — сузьте период'
  if (reg.state === 'error') return reg.error
  return ''
}

async function downloadRegistry(reg) {
  if (!canDownload(reg)) return
  reg.downloading = true
  downloadError.value = ''
  try {
    const blob = await financeApi.exportReport(getOrganizationId(), reg.type, { from: reg.from, to: reg.to })
    // BOM — чтобы Excel открыл UTF-8 с кириллицей без «кракозябр»
    saveFile(new Blob(['﻿', blob], { type: 'text/csv;charset=utf-8' }),
      `${reg.type.toLowerCase()}_${reg.from}_${reg.to}.csv`)
  } catch (e) {
    downloadError.value = `Не удалось скачать реестр: ${errorText(e)}`
  } finally {
    reg.downloading = false
  }
}

function saveFile(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

// ─────────── Создание реестра ───────────
const showModal = ref(false)
const formError = ref('')
const form = reactive({ type: '', period: '', dateFrom: '', dateTo: '' })

function openCreate() {
  const current = monthOptions[0]
  Object.assign(form, { type: types.value[0]?.type || '', period: current.value, dateFrom: current.from, dateTo: current.to })
  formError.value = ''
  showModal.value = true
}

// Выбор месяца подставляет даты; ручная правка дат переключает на произвольный период
watch(() => form.period, value => {
  const month = monthOptions.find(m => m.value === value)
  if (month) {
    form.dateFrom = month.from
    form.dateTo = month.to
  }
})
watch(() => [form.dateFrom, form.dateTo], ([from, to]) => {
  const month = monthOptions.find(m => m.value === form.period)
  if (month && (month.from !== from || month.to !== to)) form.period = 'custom'
})

function handleSubmit() {
  if (!form.type) { formError.value = 'Выберите тип реестра.'; return }
  if (!form.dateFrom || !form.dateTo) { formError.value = 'Укажите даты начала и окончания периода.'; return }
  if (form.dateFrom > form.dateTo) { formError.value = 'Дата начала позже даты окончания.'; return }
  const month = monthOptions.find(m => m.from === form.dateFrom && m.to === form.dateTo)
  addRegistry(form.type, month || {
    from: form.dateFrom,
    to: form.dateTo,
    label: `${formatDateShort(form.dateFrom)} — ${formatDateShort(form.dateTo)}`
  })
  showModal.value = false
}

onMounted(async () => {
  await loadTypes()
  if (!types.value.length) return
  registries.value = monthOptions.slice(0, DEFAULT_MONTHS).map(m => makeRegistry(types.value[0].type, m))
  registries.value.forEach(loadCount)
})

// ─────────── Помощники ───────────
// Месяц со сдвигом от текущего: { value, label: 'Сентябрь 2026', from, to }
function monthPeriod(offset) {
  const now = new Date()
  const first = new Date(now.getFullYear(), now.getMonth() + offset, 1)
  const last = new Date(first.getFullYear(), first.getMonth() + 1, 0)
  return {
    value: toIsoDate(first).slice(0, 7),
    label: `${MONTHS[first.getMonth()]} ${first.getFullYear()}`,
    from: toIsoDate(first),
    to: toIsoDate(last)
  }
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.table-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 16px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
  overflow-x: auto;
}
.table-title { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.table-title h3 { font-size: 16px; font-weight: 700; color: #152421; }
.table-title .total { font-size: 12px; color: #6D7D79; }
.table-header, .table-row { display: flex; align-items: center; gap: 12px; min-width: 1000px; }
.table-header {
  background: #F4F7F8; padding: 12px 16px;
  border-radius: 8px;
  font-size: 12px; font-weight: 700; color: #6D7D79; text-transform: uppercase;
}
.table-row { padding: 16px; border-bottom: 1px solid #E3EAE8; font-size: 13px; }
.col.num { width: 60px; font-weight: 600; color: #152421; }
.col.date { width: 120px; }
.col.type { width: 180px; font-weight: 500; }
.col.period { flex: 1; color: #6D7D79; }
.col.count { width: 100px; text-align: center; }
.col.status { width: 140px; display: flex; justify-content: center; }
.col.actions { width: 120px; display: flex; justify-content: flex-end; }
.status-badge { padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 600; }
.status-green { background: #EAF7EE; color: #2E8B57; }
.status-yellow { background: #FFF6ED; color: #D68C45; }
.status-red { background: #FDF2F2; color: #D64545; }
.status-gray { background: #EEF1F0; color: #888888; }
.download-btn { color: #2E8B57; background: none; border: none; font-size: 13px; font-weight: 600; cursor: pointer; }
.download-btn:hover { text-decoration: underline; }
.download-btn:disabled { color: #98A6A2; cursor: default; text-decoration: none; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.field-control { padding: 10px 12px; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 14px; background: white; cursor: pointer; outline: none; }
.form-hint { font-size: 12px; color: #6D7D79; }
.form-error { font-size: 13px; color: #D64545; }
</style>
