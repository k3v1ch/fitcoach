<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader
        title="Детали тренировки"
        subtitle="Информация, план и полезные материалы к занятию"
        :show-search="false"
        show-back
        @back="goBack"
      >
        <template #actions>
          <div v-if="groupChip" class="coach-card">
            <span>{{ groupChip }}</span>
            <span class="online-dot"></span>
          </div>
        </template>
      </PageHeader>

      <StateBlock v-if="pageState" :kind="pageState.kind" :message="pageState.message" />

      <div v-else class="content-grid">
        <!-- Left column: Session summary -->
        <div class="left-column">
          <div class="card">
            <div class="session-header">
              <div>
                <h2>{{ heading }}</h2>
                <p class="session-meta">{{ sessionMeta }}</p>
                <p v-if="training.status === 'CANCELLED'" class="session-meta">
                  Тренировка отменена{{ training.cancelReason ? `: ${training.cancelReason}` : '' }}
                </p>
              </div>
              <span class="status-badge" :class="`status-${badge.tone}`" :title="badge.hint">{{ badge.text }}</span>
            </div>

            <div class="divider"></div>

            <h3 class="section-title">План тренировки</h3>
            <StateBlock v-if="!plan.length" kind="empty" message="Тренер ещё не добавил план этой тренировки" />
            <div v-else class="training-blocks">
              <div v-for="block in plan" :key="block.id" class="training-block">
                <div class="block-header">
                  <span class="block-title">{{ block.title }}</span>
                  <span class="block-hr">{{ block.hr }}</span>
                </div>
                <p v-if="block.desc" class="block-desc">{{ block.desc }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Right column: Coach feedback -->
        <aside class="right-column">
          <div class="card">
            <h3>Комментарий тренера</h3>
            <p v-if="feedback" class="feedback-text">{{ feedback }}</p>
            <StateBlock v-else kind="empty" message="Тренер не оставил комментария к этой тренировке" />

            <template v-if="closedReport">
              <div class="divider"></div>
              <h3 class="section-title">Итоги тренировки</h3>
              <p class="block-desc"><strong>Тема:</strong> {{ closedReport.topic }}</p>
              <p class="block-desc">{{ closedReport.actualContent }}</p>
              <p v-if="closedReport.comment" class="feedback-text">«{{ closedReport.comment }}»</p>
            </template>

            <div v-if="coachNames.length" class="coach-info">
              <div class="coach-avatar">{{ initials(coachNames[0]) }}</div>
              <span class="coach-name">{{ coachNames.join(', ') }}</span>
            </div>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { trainingsApi } from '../../api/trainings'
import { groupsApi } from '../../api/groups'
import { dictionariesApi } from '../../api/dictionaries'
import { organizationsApi } from '../../api/organizations'
import {
  myAthletes, selectedAthleteId, loadMyAthletes, getOrganizationId, hasPermission
} from '../../utils/session'
import { formatDate, formatTime, initials, errorText } from '../../utils/format'
import { label, tone } from '../../utils/labels'

const NO_CARD = 'Карточка спортсмена ещё не создана тренером'
const ATTENDANCE_TEXT = { PRESENT: 'Посещение отмечено', SICK: 'Пропуск по болезни', ABSENT: 'Отмечен пропуск' }

const route = useRoute()
const router = useRouter()

// Назад — туда, откуда пришли (обзор или расписание); при прямом переходе по ссылке — в расписание
const goBack = () => {
  if (window.history.state?.back) router.back()
  else router.push('/athlete/schedule')
}

// ─────────── Своя карточка ───────────
const athletesReady = ref(false)
const athletesError = ref(null)

// ─────────── Тренировка: GET /trainings/{id} → TrainingDetail { trainingId, training, report, attendance } ───────────
const detail = ref(null)
const loading = ref(true)
const error = ref(null)

const training = computed(() => detail.value?.training || null)

const pageState = computed(() => {
  if (!athletesReady.value) return { kind: 'loading', message: '' }
  if (athletesError.value) return { kind: 'error', message: errorText(athletesError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CARD }
  if (loading.value) return { kind: 'loading', message: '' }
  if (error.value) return { kind: 'error', message: errorText(error.value) }
  if (!training.value) return { kind: 'error', message: 'Расписание недоступно: нет права на просмотр тренировок.' }
  return null
})

let loadSeq = 0
async function load() {
  const seq = ++loadSeq
  const id = route.params.id
  error.value = null
  group.value = null
  coachNames.value = []
  if (!id) {
    detail.value = null
    loading.value = false
    return
  }
  loading.value = true
  try {
    const data = await trainingsApi.get(getOrganizationId(), id)
    if (seq !== loadSeq) return
    detail.value = data
    if (data?.training) loadReference(data.training, seq)
  } catch (e) {
    if (seq !== loadSeq) return
    detail.value = null
    error.value = e
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// ─────────── Подписи: группа, площадка, тип занятия, тренеры ───────────
const group = ref(null)
const venues = ref([])
const trainingTypes = ref([])
const coachNames = ref([])

async function loadReference(t, seq) {
  const org = getOrganizationId()
  // Имена тренеров отдаёт только GET /members (право members.read) — у спортсмена его обычно нет
  const canSeeCoaches = hasPermission('members.read') && (t.coachIds || []).length > 0
  const [groupRes, venueRes, typeRes, coachRes] = await Promise.allSettled([
    t.groupId ? groupsApi.get(org, t.groupId) : Promise.resolve(null),
    venues.value.length ? Promise.resolve(venues.value) : fetchAll(p => dictionariesApi.list(org, 'venues', p)),
    trainingTypes.value.length ? Promise.resolve(trainingTypes.value) : fetchAll(p => dictionariesApi.list(org, 'training-types', p)),
    canSeeCoaches ? fetchAll(p => organizationsApi.members(org, p), { role: 'TRAINER' }) : Promise.resolve([])
  ])
  if (seq !== loadSeq) return
  group.value = groupRes.status === 'fulfilled' ? groupRes.value?.group || null : null
  if (venueRes.status === 'fulfilled') venues.value = venueRes.value
  if (typeRes.status === 'fulfilled') trainingTypes.value = typeRes.value
  if (coachRes.status === 'fulfilled') {
    const byId = new Map(coachRes.value.map(m => [m.userId, m.fullName]))
    coachNames.value = (t.coachIds || []).map(id => byId.get(id)).filter(Boolean)
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

watch(() => route.params.id, () => {
  if (athletesReady.value && !athletesError.value) load()
})

// ─────────── Шапка карточки ───────────
const groupChip = computed(() => (group.value?.name ? `Группа: ${group.value.name}` : ''))

const heading = computed(() => {
  const t = training.value
  const type = trainingTypes.value.find(item => item.id === t?.typeId)?.name
  return [t?.title, type].filter(Boolean).join(' · ')
})

const sessionMeta = computed(() => {
  const t = training.value
  if (!t) return ''
  const start = new Date(t.startsAt)
  const end = new Date(t.endsAt)
  const weekday = start.toLocaleDateString('ru-RU', { weekday: 'long' })
  const date = formatDate(start, { withYear: start.getFullYear() !== new Date().getFullYear() })
  const venue = venues.value.find(v => v.id === t.venueId)
  const place = venue ? [venue.name, venue.address].filter(Boolean).join(', ') : ''
  return [
    `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)}, ${date}`,
    `${formatTime(start)} - ${formatTime(end)}`,
    place
  ].filter(Boolean).join(' · ')
})

// Своя отметка: родителю и спортсмену сервер отдаёт только свои строки посещаемости
const ownMark = computed(() => {
  const rows = detail.value?.attendance || []
  return rows.find(r => r.athleteId === selectedAthleteId.value) || (rows.length === 1 ? rows[0] : null)
})

const badge = computed(() => {
  const t = training.value
  if (!t) return { text: '', tone: 'gray', hint: '' }
  if (t.status === 'CANCELLED') return { text: 'Тренировка отменена', tone: 'red', hint: t.cancelReason || '' }
  const mark = ownMark.value
  if (mark && mark.status !== 'UNMARKED') {
    return {
      text: ATTENDANCE_TEXT[mark.status] || label('attendanceStatus', mark.status),
      tone: tone(mark.status),
      hint: [mark.reason, mark.comment].filter(Boolean).join(' · ')
    }
  }
  if (new Date(t.endsAt) > new Date() && t.status === 'PLANNED') {
    return { text: label('trainingStatus', t.status), tone: tone(t.status), hint: '' }
  }
  return { text: mark ? 'Посещение не отмечено' : label('trainingStatus', t.status), tone: 'gray', hint: '' }
})

// ─────────── План: этапы по порядку, время каждого — от начала тренировки ───────────
const plan = computed(() => {
  const t = training.value
  if (!t) return []
  let offset = 0
  return (t.plan || []).map((stage, i) => {
    const from = addMinutes(t.startsAt, offset)
    offset += stage.durationMinutes || 0
    const to = addMinutes(t.startsAt, offset)
    return {
      id: i,
      title: `${i + 1}. ${stage.title} (${stage.durationMinutes} мин)`,
      hr: `${formatTime(from)} – ${formatTime(to)}`,
      desc: stage.description || ''
    }
  })
})

const feedback = computed(() => {
  const comment = training.value?.comment?.trim()
  return comment ? `«${comment}»` : ''
})

// Отчёт тренера показываем спортсмену только закрытым
const closedReport = computed(() => {
  const report = detail.value?.report
  return report && report.status === 'CLOSED' ? report : null
})

// ─────────── Помощники ───────────
function addMinutes(value, minutes) {
  return new Date(new Date(value).getTime() + minutes * 60000)
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
  background: #A44450; border-radius: 50%;
}
.coach-card {
  height: 44px; padding: 0 16px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #6D7D79;
}
.online-dot {
  width: 6px; height: 6px;
  background: #B7F34B; border-radius: 50%;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}
@media (max-width: 1024px) { .content-grid { grid-template-columns: 1fr; } }

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }

.session-header {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;
}
.session-header h2 { font-size: 22px; font-weight: 700; color: #152421; }
.session-meta { font-size: 14px; color: #6D7D79; margin-top: 4px; }
.status-badge {
  padding: 6px 12px; background: #E9F7D5; color: #2D5B24;
  font-size: 12px; font-weight: 700; border-radius: 999px;
  white-space: nowrap;
}
.status-badge.status-green { background: #E9F7D5; color: #2E8B57; }
.status-badge.status-yellow { background: #FFF1D6; color: #A36A16; }
.status-badge.status-red { background: #FCE2E5; color: #D64545; }
.status-badge.status-blue { background: #DDECFB; color: #35678E; }
.status-badge.status-gray { background: #EEF1F0; color: #888888; }
.divider { height: 1px; background: #E3EAE8; }
.section-title { font-size: 16px; font-weight: 700; color: #152421; }
.training-blocks { display: flex; flex-direction: column; gap: 16px; }
.training-block {
  padding: 16px; background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; gap: 8px;
}
.block-header {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;
}
.block-title { font-size: 14px; font-weight: 700; color: #152421; }
.block-hr { font-size: 12px; color: #6D7D79; white-space: nowrap; }
.block-desc { font-size: 13px; color: #6D7D79; line-height: 1.5; white-space: pre-line; }

.feedback-text {
  font-size: 14px; color: #6D7D79; line-height: 1.5;
  font-style: italic;
  white-space: pre-line;
}
.coach-info {
  display: flex; align-items: center; gap: 8px;
}
.coach-avatar {
  width: 24px; height: 24px;
  background: #E9F7D5; color: #2D5B24;
  font-size: 10px; font-weight: 700; border-radius: 999px;
  display: flex; justify-content: center; align-items: center;
  flex-shrink: 0;
}
.coach-name { font-size: 12px; font-weight: 600; color: #6D7D79; }
</style>
