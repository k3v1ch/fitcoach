<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader
        title="Задания и прогресс"
        subtitle="Задания на ближайшие тренировки, разряд и персональные рекорды"
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

      <div v-else class="content-split">
        <!-- Rank card -->
        <div class="rank-card">
          <div class="rank-icon">
            <BaseIcon name="award" :size="32" color="#102522" />
          </div>
          <div class="rank-info">
            <div class="rank-title">{{ rankTitle }}</div>
            <div class="rank-subtitle">
              <span v-if="rankSubtitle">{{ rankSubtitle }} · </span>
              <template v-if="standardsTotal">
                Нормативы: выполнено <strong>{{ standardsMet }} из {{ standardsTotal }}</strong>
              </template>
              <template v-else>{{ standardsNote }}</template>
            </div>
            <div class="progress-bar" :title="standardsTotal ? `Выполнено нормативов: ${standardsMet} из ${standardsTotal}` : ''">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
          </div>
        </div>

        <!-- Tasks card: этапы плана ближайших тренировок -->
        <div class="records-card">
          <h3>Задания на ближайшие тренировки</h3>
          <StateBlock v-if="tasksLoading" kind="loading" />
          <StateBlock v-else-if="tasksError" kind="error" :message="errorText(tasksError)" />
          <StateBlock
            v-else-if="!taskTrainings.length"
            kind="empty"
            :message="`В ближайшие ${TASK_DAYS} дней тренировок не запланировано`"
          />
          <div v-else class="tasks-list">
            <div v-for="training in taskTrainings" :key="training.id" class="task-group">
              <router-link :to="`/athlete/schedule/${training.id}`" class="task-head">
                <span class="record-name">{{ training.title }}</span>
                <span class="record-date">{{ training.when }}</span>
              </router-link>
              <div v-if="training.stages.length" class="records-list">
                <div v-for="stage in training.stages" :key="stage.id" class="record-item">
                  <div class="record-info">
                    <div class="record-name">{{ stage.title }}</div>
                    <div v-if="stage.desc" class="record-date">{{ stage.desc }}</div>
                  </div>
                  <div class="record-value">{{ stage.duration }}</div>
                </div>
              </div>
              <div v-else class="record-date">Тренер ещё не добавил план этой тренировки</div>
            </div>
          </div>
        </div>

        <!-- Records card -->
        <div class="records-card">
          <h3>Личные рекорды спортсмена</h3>
          <StateBlock v-if="resultsLoading" kind="loading" />
          <StateBlock v-else-if="resultsError" kind="error" :message="errorText(resultsError)" />
          <StateBlock
            v-else-if="!records.length"
            kind="empty"
            message="Личных рекордов пока нет — их отмечает тренер, когда вносит результаты"
          />
          <div v-else class="records-list">
            <div v-for="record in records" :key="record.id" class="record-item">
              <div class="record-info">
                <div class="record-name">{{ record.name }}</div>
                <div class="record-date">{{ record.date }}</div>
              </div>
              <div class="record-value">{{ record.value }}</div>
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
import { athletesApi } from '../../api/athletes'
import { trainingsApi } from '../../api/trainings'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { myAthletes, selectedAthleteId, loadMyAthletes, getOrganizationId } from '../../utils/session'
import { formatDate, formatTime, toIsoDate, toIsoDateTime, addDays, errorText } from '../../utils/format'

const NO_CARD = 'Карточка спортсмена ещё не создана тренером'
// Задания — этапы плана ближайших запланированных тренировок (отдельного API заданий нет)
const TASK_DAYS = 30
const TASK_TRAININGS = 5

// ─────────── Своя карточка ───────────
const athletesReady = ref(false)
const athletesError = ref(null)

const pageState = computed(() => {
  if (!athletesReady.value) return { kind: 'loading', message: '' }
  if (athletesError.value) return { kind: 'error', message: errorText(athletesError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CARD }
  return null
})

// ─────────── Данные ───────────
const ranks = ref([])
const standards = ref([])
const progressLoading = ref(false)
const ranksError = ref(null)
const standardsError = ref(null)

const upcoming = ref([])
const tasksLoading = ref(false)
const tasksError = ref(null)

const results = ref([])
const resultsLoading = ref(false)
const resultsError = ref(null)

const groups = ref([])
const sportTypes = ref([])
const progressPercent = ref(0)

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  ranks.value = []
  standards.value = []
  upcoming.value = []
  results.value = []
  groups.value = []
  ranksError.value = null
  standardsError.value = null
  tasksError.value = null
  resultsError.value = null
  progressPercent.value = 0
  if (!athletesReady.value || !athleteId) {
    progressLoading.value = false
    tasksLoading.value = false
    resultsLoading.value = false
    return
  }
  progressLoading.value = true
  tasksLoading.value = true
  resultsLoading.value = true
  let org
  try {
    org = getOrganizationId()
  } catch (e) {
    ranksError.value = standardsError.value = tasksError.value = resultsError.value = e
    progressLoading.value = tasksLoading.value = resultsLoading.value = false
    return
  }
  const now = new Date()
  const [rankRes, standardRes, trainingRes, resultRes, groupRes] = await Promise.allSettled([
    // GET /athletes/{id}/ranks, /standards, /results — свои разряды, нормативы и результаты
    fetchAll(p => athletesApi.ranks(org, athleteId, p)),
    fetchAll(p => athletesApi.standards(org, athleteId, p)),
    // GET /trainings: ближайшие запланированные занятия групп спортсмена (сервер сортирует по началу)
    trainingsApi.list(org, {
      athleteId,
      from: toIsoDateTime(now),
      to: toIsoDateTime(addDays(now, TASK_DAYS)),
      status: 'PLANNED',
      page: 0,
      size: TASK_TRAININGS
    }),
    fetchAll(p => athletesApi.results(org, athleteId, p)),
    fetchAll(p => groupsApi.list(org, p), { athleteId, status: 'ACTIVE' })
  ])
  if (seq !== loadSeq) return
  if (rankRes.status === 'fulfilled') ranks.value = rankRes.value
  else ranksError.value = rankRes.reason
  if (standardRes.status === 'fulfilled') standards.value = standardRes.value
  else standardsError.value = standardRes.reason
  if (trainingRes.status === 'fulfilled') upcoming.value = trainingRes.value?.items || []
  else tasksError.value = trainingRes.reason
  if (resultRes.status === 'fulfilled') results.value = resultRes.value
  else resultsError.value = resultRes.reason
  if (groupRes.status === 'fulfilled') groups.value = groupRes.value
  progressLoading.value = false
  tasksLoading.value = false
  resultsLoading.value = false
  // Полоса заполняется с анимацией после загрузки нормативов
  setTimeout(() => {
    if (seq === loadSeq) progressPercent.value = standardsPercent.value
  }, 150)
}

async function loadSportTypes() {
  try {
    sportTypes.value = await fetchAll(p => dictionariesApi.list(getOrganizationId(), 'sport-types', p))
  } catch (_) {
    sportTypes.value = []
  }
}

onMounted(async () => {
  loadSportTypes()
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

// ─────────── Разряд и нормативы ───────────
// Действующий разряд — присвоенный последним
const currentRank = computed(() => [...ranks.value]
  .sort((a, b) => String(b.assignedOn || '').localeCompare(String(a.assignedOn || '')))[0] || null)

const rankTitle = computed(() => {
  if (progressLoading.value) return 'Загрузка…'
  if (ranksError.value) return 'Не удалось загрузить разряд'
  return currentRank.value?.name || 'Разряд пока не присвоен'
})

const rankSubtitle = computed(() => {
  const rank = currentRank.value
  if (progressLoading.value) return ''
  if (ranksError.value) return errorText(ranksError.value)
  if (!rank) return ''
  const sport = sportTypes.value.find(s => s.id === rank.sportTypeId)?.name
  const expired = rank.validUntil && rank.validUntil < toIsoDate(new Date())
  return [
    sport ? `Специализация: ${sport}` : null,
    rank.assignedOn ? `присвоен ${formatDate(rank.assignedOn)}` : null,
    rank.validUntil ? `${expired ? 'срок истёк' : 'действует до'} ${formatDate(rank.validUntil)}` : null
  ].filter(Boolean).join(' · ')
})

const standardsTotal = computed(() => standards.value.length)
const standardsMet = computed(() => standards.value.filter(s => s.status === 'MET').length)
const standardsPercent = computed(() => (standardsTotal.value ? Math.round(standardsMet.value / standardsTotal.value * 100) : 0))

const standardsNote = computed(() => {
  if (progressLoading.value) return ''
  if (standardsError.value) return 'Нормативы: не удалось загрузить'
  return 'Нормативы ещё не оценивались'
})

// ─────────── Задания: этапы плана ближайших тренировок ───────────
const taskTrainings = computed(() => upcoming.value.map(t => ({
  id: t.id,
  title: t.title,
  when: whenText(t.startsAt, t.endsAt),
  stages: (t.plan || []).map((stage, i) => ({
    id: i,
    title: `${i + 1}. ${stage.title}`,
    desc: stage.description || '',
    duration: `${stage.durationMinutes} мин`
  }))
})))

function whenText(startsAt, endsAt) {
  const start = new Date(startsAt)
  const now = new Date()
  const key = toIsoDate(start)
  const day = key === toIsoDate(now)
    ? 'Сегодня'
    : key === toIsoDate(addDays(now, 1)) ? 'Завтра' : formatDate(start, { withYear: false })
  return `${day}, ${formatTime(start)} - ${formatTime(endsAt)}`
}

// ─────────── Личные рекорды: результаты с отметкой isPersonalBest, по одному на показатель ───────────
const records = computed(() => {
  const best = new Map()
  for (const r of results.value) {
    if (!r.isPersonalBest) continue
    const key = `${String(r.metricName || '').trim().toLowerCase()}|${r.unit || ''}`
    const prev = best.get(key)
    if (!prev || String(r.measuredOn) > String(prev.measuredOn)) best.set(key, r)
  }
  return [...best.values()]
    .sort((a, b) => String(b.measuredOn || '').localeCompare(String(a.measuredOn || '')))
    .map(r => ({
      id: r.id,
      name: r.metricName,
      date: `Дата рекорда: ${formatDate(r.measuredOn)}`,
      value: [formatNumber(r.value), r.unit].filter(Boolean).join(' ')
    }))
})

function formatNumber(value) {
  const n = Number(value)
  return value === null || value === undefined || isNaN(n)
    ? String(value ?? '—')
    : n.toLocaleString('ru-RU', { maximumFractionDigits: 3 })
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

.content-split {
  display: flex; flex-direction: column; gap: 24px;
}

/* Rank card */
.rank-card {
  padding: 24px;
  background: #102522;
  border-radius: 20px;
  display: flex; align-items: center; gap: 16px;
}
.rank-icon {
  width: 56px; height: 56px;
  background: #B7F34B;
  border-radius: 16px;
  display: flex; justify-content: center; align-items: center;
  flex-shrink: 0;
}
.rank-info { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.rank-title { color: white; font-size: 18px; font-weight: 700; }
.rank-subtitle { color: #98A6A2; font-size: 13px; }
.rank-subtitle strong { color: #B7F34B; font-weight: 700; }
.progress-bar {
  height: 6px; background: #19332F;
  border-radius: 999px; overflow: hidden;
  margin-top: 4px;
}
.progress-fill {
  height: 100%;
  background: #B7F34B;
  border-radius: 999px;
  transition: width 1s ease-out;
}

/* Records card */
.records-card {
  padding: 24px; background: white;
  border: 1px solid #E3EAE8; border-radius: 20px;
  display: flex; flex-direction: column; gap: 16px;
}
.records-card h3 {
  font-size: 18px; font-weight: 700; color: #152421;
}
.records-list { display: flex; flex-direction: column; gap: 12px; }
.record-item {
  padding-bottom: 12px;
  border-bottom: 1px solid #E3EAE8;
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
}
.record-item:last-child { border-bottom: none; padding-bottom: 0; }
.record-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.record-name { font-size: 13px; font-weight: 600; color: #152421; }
.record-date { font-size: 11px; color: #98A6A2; white-space: pre-line; }
.record-value { font-size: 15px; font-weight: 700; color: #2D5B24; flex-shrink: 0; white-space: nowrap; }

/* Задания по тренировкам */
.tasks-list { display: flex; flex-direction: column; gap: 16px; }
.task-group {
  padding: 16px; background: #F4F7F8; border-radius: 14px;
  display: flex; flex-direction: column; gap: 12px;
}
.task-head {
  display: flex; justify-content: space-between; align-items: baseline; gap: 12px;
  text-decoration: none;
}
.task-head .record-name { font-size: 14px; font-weight: 700; }
.task-head:hover .record-name { color: #35678E; }
</style>
