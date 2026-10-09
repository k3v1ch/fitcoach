<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        title="Спортивный прогресс"
        subtitle="Оценка показателей, комментарии тренера и разряды"
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

      <div v-if="pageState" class="card">
        <StateBlock :kind="pageState.kind" :message="pageState.message" />
      </div>

      <template v-else>
        <section class="top-section">
          <!-- Карточка разряда -->
          <div class="rank-card">
            <div class="rank-header">
              <span class="rank-label">ТЕКУЩИЙ РАЗРЯД</span>
              <BaseIcon name="award" :size="24" color="#B7F34B" />
            </div>
            <div class="rank-value">{{ currentRank ? currentRank.name : 'Разряд не присвоен' }}</div>
            <div class="rank-note">{{ rankNote }}</div>
          </div>

          <!-- Быстрые метрики -->
          <div class="metrics">
            <MetricCard
              :label="bestLabel"
              :value="bestValue"
              :note="bestNote"
              icon="award"
              color="#E9F7D5"
              icon-color="#2E8B57"
            />
            <MetricCard
              label="Выполнение нормативов"
              :value="standardsValue"
              :note="standardsNote"
              icon="check"
              color="#DDECFB"
              icon-color="#35678E"
            />
          </div>
        </section>

        <section class="content-grid">
          <div class="card chart-card">
            <div class="chart-head">
              <h3>{{ chartTitle }}</h3>
              <select v-if="metrics.length > 1" v-model="selectedMetric" class="metric-select" aria-label="Показатель">
                <option v-for="m in metrics" :key="m.key" :value="m.key">{{ m.metricName }} ({{ m.unit }})</option>
              </select>
            </div>
            <StateBlock v-if="!chart" kind="empty" message="Результатов пока нет — тренер внесёт их после замеров" />
            <div v-else class="chart">
              <div class="y-axis">
                <span v-for="(tick, i) in chart.axis" :key="i">{{ tick }}</span>
              </div>
              <div class="chart-area">
                <div v-for="line in 3" :key="line" class="grid-line"></div>
                <div class="bars">
                  <div v-for="bar in chart.bars" :key="bar.key" class="bar-item" :title="bar.title">
                    <div class="bar" :style="{ height: bar.height + '%' }"></div>
                    <span class="bar-label">{{ bar.label }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="card feedback-card">
            <h3>Отзывы тренера</h3>
            <StateBlock v-if="!feedbacks.length" kind="empty" message="Комментариев тренера пока нет" />
            <div v-else class="feedback-list">
              <div v-for="fb in feedbacks" :key="fb.id" class="feedback-item">
                <div class="feedback-header">
                  <span class="feedback-date">{{ fb.date }}</span>
                  <span class="feedback-author">{{ fb.author }}</span>
                </div>
                <p>{{ fb.text }}</p>
              </div>
            </div>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import MetricCard from '../../components/layout/MetricCard.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { athletesApi } from '../../api/athletes'
import { announcementsApi } from '../../api/announcements'
import {
  myAthletes, selectedAthleteId, selectedAthlete, loadMyAthletes, selectAthlete, getOrganizationId
} from '../../utils/session'
import { parseDate, formatDate, toIsoDate, fullName, initials, errorText } from '../../utils/format'
import { label } from '../../utils/labels'

const NO_CHILD = 'Ребёнок ещё не привязан — обратитесь к тренеру'
const CHART_POINTS = 8   // столбиков на графике — последние замеры показателя
const FEEDBACK_LIMIT = 5

function plural(n, one, few, many) {
  const m10 = n % 10
  const m100 = n % 100
  if (m10 === 1 && m100 !== 11) return one
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return few
  return many
}

const formatNumber = (value, digits = 3) =>
  Number(value).toLocaleString('ru-RU', { maximumFractionDigits: digits })
const last = list => list[list.length - 1]
const pad = n => String(n).padStart(2, '0')
const shortDate = value => {
  const d = parseDate(value)
  return d && !isNaN(d) ? `${pad(d.getDate())}.${pad(d.getMonth() + 1)}` : '—'
}

// ─────────── загрузка ───────────

const childrenReady = ref(false)
const childrenError = ref(null)

const results = ref([])    // Result[] по возрастанию measuredOn
const standards = ref([])  // AthleteStandard[]
const ranks = ref([])      // AthleteRank[]
const loading = ref(false)
const error = ref(null)
const selectedMetric = ref('')
const hasUnread = ref(false)

// Результаты приходят по возрастанию measuredOn: берём первую страницу, а если их больше — две последние
async function loadRecentResults(org, athleteId) {
  const first = await athletesApi.results(org, athleteId, { size: 100 })
  const pages = first?.totalPages || 0
  if (pages <= 1) return first?.items || []
  const [prevPage, lastPage] = await Promise.all([
    pages - 2 === 0 ? Promise.resolve(first) : athletesApi.results(org, athleteId, { size: 100, page: pages - 2 }),
    athletesApi.results(org, athleteId, { size: 100, page: pages - 1 })
  ])
  return [...(prevPage?.items || []), ...(lastPage?.items || [])]
}

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  results.value = []
  standards.value = []
  ranks.value = []
  error.value = null
  if (!athleteId) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const org = getOrganizationId()
    const [resultList, standardPage, rankPage] = await Promise.all([
      loadRecentResults(org, athleteId),
      athletesApi.standards(org, athleteId, { size: 100 }),
      athletesApi.ranks(org, athleteId, { size: 100 })
    ])
    if (seq !== loadSeq) return
    results.value = resultList
    standards.value = standardPage?.items || []
    ranks.value = rankPage?.items || []
  } catch (e) {
    if (seq === loadSeq) error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

async function loadUnread() {
  try {
    const res = await announcementsApi.list(getOrganizationId(), { status: 'PUBLISHED', unread: true, size: 1 })
    hasUnread.value = (res?.totalElements || 0) > 0
  } catch (_) {
    hasUnread.value = false // отметка о новых объявлениях не обязательна для экрана
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
  return null
})

// ─────────── разряд ───────────

// Текущий — последний присвоенный из действующих (validUntil не указан или не прошёл); иначе последний
const currentRank = computed(() => {
  const list = [...ranks.value].sort((a, b) => (a.assignedOn || '').localeCompare(b.assignedOn || ''))
  if (!list.length) return null
  const todayKey = toIsoDate(new Date())
  const valid = list.filter(r => !r.validUntil || r.validUntil >= todayKey)
  return last(valid.length ? valid : list)
})

const rankNote = computed(() => {
  const r = currentRank.value
  if (!r) return 'Разряды вносит организация — после присвоения разряд появится здесь.'
  const parts = [`Присвоен ${formatDate(r.assignedOn)}`]
  if (r.validUntil) {
    parts.push(r.validUntil < toIsoDate(new Date())
      ? `срок действия истёк ${formatDate(r.validUntil)}`
      : `действует до ${formatDate(r.validUntil)}`)
  }
  return r.comment ? `${parts.join(', ')} · ${r.comment}` : parts.join(', ')
})

// ─────────── показатели и график ───────────

// Показатель — пара metricName + unit; первым идёт тот, у которого больше замеров
const metrics = computed(() => {
  const map = new Map()
  for (const r of results.value) {
    const key = `${r.metricName}\u0001${r.unit}`
    if (!map.has(key)) map.set(key, { key, metricName: r.metricName, unit: r.unit, items: [] })
    map.get(key).items.push(r)
  }
  const list = [...map.values()]
  for (const m of list) {
    m.items.sort((a, b) => a.measuredOn.localeCompare(b.measuredOn) || (a.createdAt || '').localeCompare(b.createdAt || ''))
  }
  return list.sort((a, b) => b.items.length - a.items.length
    || last(b.items).measuredOn.localeCompare(last(a.items).measuredOn))
})

const currentMetric = computed(() =>
  metrics.value.find(m => m.key === selectedMetric.value) || metrics.value[0] || null)

watch(metrics, list => {
  if (!list.some(m => m.key === selectedMetric.value)) selectedMetric.value = list[0]?.key || ''
})

const chartTitle = computed(() => {
  const m = currentMetric.value
  return m ? `Динамика результатов (${m.metricName}, ${m.unit})` : 'Динамика результатов'
})

// Столбики — последние замеры; высота — положение значения между минимумом и максимумом (10–85 %)
const chart = computed(() => {
  const m = currentMetric.value
  if (!m) return null
  const points = m.items.slice(-CHART_POINTS)
  const values = points.map(r => Number(r.value))
  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min
  return {
    // ось: максимум, середина, минимум; при одинаковых значениях — одно значение на средней линии
    axis: span ? [max, (max + min) / 2, min].map(v => formatNumber(v, 2)) : ['', formatNumber(max, 2), ''],
    bars: points.map((r, i) => ({
      key: r.id,
      label: shortDate(r.measuredOn),
      height: span ? Math.round(10 + ((values[i] - min) / span) * 75) : 50,
      title: `${formatDate(r.measuredOn)}: ${formatNumber(r.value)} ${r.unit}${r.isPersonalBest ? ' · личный рекорд' : ''}`
    }))
  }
})

// Личный рекорд отмечает тренер (isPersonalBest); если отметки нет — показываем последний замер
const best = computed(() => {
  const m = currentMetric.value
  if (!m) return null
  const records = m.items.filter(r => r.isPersonalBest)
  return records.length ? { result: last(records), record: true } : { result: last(m.items), record: false }
})

const bestLabel = computed(() => {
  const b = best.value
  if (!b) return 'Личный рекорд'
  return `${b.record ? 'Личный рекорд' : 'Последний результат'} (${b.result.metricName})`
})
const bestValue = computed(() => (best.value ? `${formatNumber(best.value.result.value)} ${best.value.result.unit}` : '—'))
const bestNote = computed(() => (best.value ? `Зафиксирован ${formatDate(best.value.result.measuredOn)}` : 'Результатов пока нет'))

// ─────────── нормативы ───────────

const standardsStats = computed(() => ({
  total: standards.value.length,
  met: standards.value.filter(s => s.status === 'MET').length,
  notAssessed: standards.value.filter(s => s.status === 'NOT_ASSESSED').length
}))

const standardsValue = computed(() => {
  const { total, met } = standardsStats.value
  return total ? `${Math.round((met / total) * 100)}%` : '—'
})

const standardsNote = computed(() => {
  const { total, met, notAssessed } = standardsStats.value
  if (!total) return 'Нормативы пока не внесены'
  const base = `Выполнено ${met} из ${total} ${plural(total, 'норматива', 'нормативов', 'нормативов')}`
  return notAssessed ? `${base} · не оценивались: ${notAssessed}` : base
})

// ─────────── комментарии тренера: к результатам и нормативам ───────────

const feedbacks = computed(() => {
  const fromResults = results.value
    .filter(r => r.comment && r.comment.trim())
    .map(r => ({ id: `r-${r.id}`, on: r.measuredOn, author: `${r.metricName}: ${formatNumber(r.value)} ${r.unit}`, text: r.comment }))
  const fromStandards = standards.value
    .filter(s => s.comment && s.comment.trim())
    .map(s => ({ id: `s-${s.id}`, on: s.assessedOn, author: `${s.name}: ${label('standardStatus', s.status)}`, text: s.comment }))
  return [...fromResults, ...fromStandards]
    .sort((a, b) => (b.on || '').localeCompare(a.on || ''))
    .slice(0, FEEDBACK_LIMIT)
    .map(f => ({ ...f, date: formatDate(f.on, { withYear: false }) }))
})
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

.top-section { display: flex; gap: 24px; flex-wrap: wrap; }
.rank-card {
  width: 360px; padding: 24px;
  background: #102522; border-radius: 20px;
  display: flex; flex-direction: column; gap: 16px;
}
.rank-header { display: flex; justify-content: space-between; align-items: center; }
.rank-label { color: #B7F34B; font-size: 13px; font-weight: 700; }
.rank-value { color: white; font-size: 24px; font-weight: 800; }
.rank-note { color: #98A6A2; font-size: 12px; line-height: 1.5; }

.metrics { flex: 1; display: grid; grid-template-columns: 1fr 1fr; gap: 16px; min-width: 320px; }

.content-grid { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
}
.card h3 { font-size: 16px; font-weight: 700; color: #152421; }

.chart-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.metric-select {
  padding: 6px 10px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 13px; color: #152421; cursor: pointer;
}

.chart { display: flex; gap: 12px; height: 200px; }
.y-axis {
  display: flex; flex-direction: column; justify-content: space-between;
  font-size: 11px; color: #6D7D79; padding: 0 4px;
}
.chart-area { flex: 1; position: relative; }
.grid-line {
  position: absolute; left: 0; right: 0;
  height: 1px; background: #E3EAE8;
}
.grid-line:nth-child(1) { top: 0; }
.grid-line:nth-child(2) { top: 50%; }
.grid-line:nth-child(3) { top: 100%; }

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

.bars {
  position: absolute; bottom: 0; left: 0; right: 0;
  display: flex; justify-content: space-around;
  align-items: flex-end; height: 100%;
}
.bar-item {
  display: flex; flex-direction: column;
  align-items: center; gap: 8px; width: 40px;
  height: 100%; justify-content: flex-end;
}
.bar {
  width: 24px; background: #102522;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
}
.bar-label { font-size: 11px; color: #6D7D79; }

.feedback-list { display: flex; flex-direction: column; gap: 16px; }
.feedback-item {
  padding: 12px; background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; gap: 8px;
}
.feedback-header { display: flex; justify-content: space-between; gap: 8px; }
.feedback-date { font-size: 11px; color: #6D7D79; font-weight: 600; }
.feedback-author { font-size: 11px; color: #2E8B57; font-weight: 700; text-align: right; }
.feedback-item p { font-size: 13px; color: #152421; line-height: 1.4; }
</style>
