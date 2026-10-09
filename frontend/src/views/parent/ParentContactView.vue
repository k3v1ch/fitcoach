<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        title="Связь с секцией"
        subtitle="Контакты организации и группы, в которых занимается ребёнок"
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

      <div class="content-grid">
        <!-- ЛЕВАЯ КОЛОНКА -->
        <div class="left-column">
          <!-- Карточка: Контакты секции (GET /organizations/{id}) -->
          <div class="card">
            <h3>Контакты секции</h3>
            <StateBlock v-if="orgLoading" kind="loading" />
            <StateBlock v-else-if="orgError" kind="error" :message="orgError" />
            <div v-else class="info-list">
              <div class="info-item">
                <BaseIcon name="briefcase" :size="16" color="#6D7D79" />
                <div class="info-text">
                  <div class="info-label">ОРГАНИЗАЦИЯ</div>
                  <div class="info-value">{{ organization?.name || '—' }}</div>
                </div>
              </div>
              <div class="info-item">
                <BaseIcon name="map-pin" :size="16" color="#6D7D79" />
                <div class="info-text">
                  <div class="info-label">АДРЕС</div>
                  <div class="info-value">{{ organization?.address || 'Не указан' }}</div>
                </div>
              </div>
              <div v-if="organization?.description" class="info-item">
                <BaseIcon name="file-text" :size="16" color="#6D7D79" />
                <div class="info-text">
                  <div class="info-label">О СЕКЦИИ</div>
                  <div class="info-value info-value--text">{{ organization.description }}</div>
                </div>
              </div>
              <div class="info-item">
                <BaseIcon name="phone" :size="16" color="#6D7D79" />
                <div class="info-text">
                  <div class="info-label">ТЕЛЕФОН И ВРЕМЯ РАБОТЫ</div>
                  <div class="info-value info-value--muted">Раздел появится позже</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Карточка: Тренерский состав — группы ребёнка и число тренеров (GET /groups?athleteId=…) -->
          <div class="card">
            <h3>Тренерский состав</h3>
            <StateBlock v-if="groupsState" :kind="groupsState.kind" :message="groupsState.message" />
            <div v-else class="coaches-list">
              <div v-for="(group, i) in groupItems" :key="group.id" class="coach-item">
                <div class="coach-avatar" :style="AVATAR_STYLES[i % AVATAR_STYLES.length]">{{ group.initials }}</div>
                <div class="coach-details">
                  <div class="coach-name">{{ group.name }}</div>
                  <div class="coach-role">{{ group.role }}</div>
                </div>
                <div class="coach-phone">{{ group.coaches }}</div>
              </div>
            </div>
            <p class="soon-text">Имена и телефоны тренеров появятся позже.</p>
          </div>
        </div>

        <!-- ПРАВАЯ КОЛОНКА -->
        <div class="right-column">
          <div class="card">
            <h3>Написать сообщение</h3>
            <p class="soon-text">
              Раздел появится позже: отправить сообщение тренеру или администрации из кабинета пока нельзя.
              Новости и запросы согласия от секции приходят в раздел
              <router-link to="/parent/announcements" class="inline-link">«Объявления»</router-link>.
            </p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StateBlock from '../../components/ui/StateBlock.vue'
import { organizationsApi } from '../../api/organizations'
import { groupsApi } from '../../api/groups'
import { sectionsApi } from '../../api/sections'
import { announcementsApi } from '../../api/announcements'
import {
  myAthletes, selectedAthleteId, selectedAthlete, loadMyAthletes, selectAthlete, getOrganizationId
} from '../../utils/session'
import { fullName, initials, errorText } from '../../utils/format'

const NO_CHILD = 'Ребёнок ещё не привязан — обратитесь к тренеру'
const AVATAR_STYLES = [
  { background: '#E9F7D5', color: '#2E8B57' },
  { background: '#DDECFB', color: '#35678E' },
  { background: '#FFF1D6', color: '#8B6914' }
]

function plural(n, one, few, many) {
  const m10 = n % 10
  const m100 = n % 100
  if (m10 === 1 && m100 !== 11) return one
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return few
  return many
}

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

// ─────────── организация ───────────

const organization = ref(null)
const orgLoading = ref(true)
const orgError = ref('')

async function loadOrganization() {
  orgLoading.value = true
  orgError.value = ''
  try {
    organization.value = await organizationsApi.get(getOrganizationId())
  } catch (e) {
    organization.value = null
    orgError.value = errorText(e)
  } finally {
    orgLoading.value = false
  }
}

// ─────────── группы ребёнка ───────────
// В карточке спортсмена групп нет, поэтому берём текущие группы через фильтр athleteId.
// У группы есть только coachIds: имена тренеров родителю недоступны (нет members.read).

const childrenReady = ref(false)
const childrenError = ref(null)
const groups = ref([])
const sections = ref([])
const groupsLoading = ref(false)
const groupsError = ref(null)

let loadSeq = 0
async function loadGroups() {
  const seq = ++loadSeq
  const athleteId = selectedAthleteId.value
  groups.value = []
  groupsError.value = null
  if (!athleteId) {
    groupsLoading.value = false
    return
  }
  groupsLoading.value = true
  try {
    const list = await fetchAll(p => groupsApi.list(getOrganizationId(), { athleteId, status: 'ACTIVE', ...p }))
    if (seq !== loadSeq) return
    groups.value = list
  } catch (e) {
    if (seq === loadSeq) groupsError.value = e
  } finally {
    if (seq === loadSeq) groupsLoading.value = false
  }
}

// Названия секций для подписей групп; без них список групп всё равно показывается
async function loadSections() {
  try {
    sections.value = await fetchAll(p => sectionsApi.list(getOrganizationId(), p))
  } catch (_) {
    sections.value = []
  }
}

const hasUnread = ref(false)
async function loadUnread() {
  try {
    const res = await announcementsApi.list(getOrganizationId(), { status: 'PUBLISHED', unread: true, size: 1 })
    hasUnread.value = (res?.totalElements || 0) > 0
  } catch (_) {
    hasUnread.value = false // отметка в шапке не критична
  }
}

onMounted(async () => {
  loadOrganization()
  loadSections()
  loadUnread()
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (e) {
    childrenError.value = e
  } finally {
    childrenReady.value = true
  }
  if (!childrenError.value) loadGroups()
})

watch(selectedAthleteId, () => {
  if (childrenReady.value && !childrenError.value) loadGroups()
})

const groupsState = computed(() => {
  if (!childrenReady.value || groupsLoading.value) return { kind: 'loading', message: '' }
  if (childrenError.value) return { kind: 'error', message: errorText(childrenError.value) }
  if (!selectedAthleteId.value) return { kind: 'empty', message: NO_CHILD }
  if (groupsError.value) return { kind: 'error', message: errorText(groupsError.value) }
  if (!groups.value.length) return { kind: 'empty', message: 'Ребёнок пока не зачислен ни в одну группу' }
  return null
})

const sectionById = computed(() => new Map(sections.value.map(s => [s.id, s])))

const groupItems = computed(() => groups.value.map(g => {
  const n = (g.coachIds || []).length
  return {
    id: g.id,
    name: g.name,
    initials: initials(g.name),
    role: [sectionById.value.get(g.sectionId)?.name, g.description].filter(Boolean).join(' · ') || 'Группа',
    coaches: n ? `${n} ${plural(n, 'тренер', 'тренера', 'тренеров')}` : 'Тренер не назначен'
  }
}))
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

/* Селектор ребенка */
.child-selector {
  position: relative;
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
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

/* Сетка */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
@media (max-width: 1024px) { .content-grid { grid-template-columns: 1fr; } }
.left-column, .right-column { display: flex; flex-direction: column; gap: 24px; }

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 16px;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }

/* Левая колонка */
.info-list { display: flex; flex-direction: column; gap: 16px; }
.info-item { display: flex; gap: 12px; align-items: flex-start; }
.info-text { display: flex; flex-direction: column; gap: 2px; }
.info-label { font-size: 12px; font-weight: 600; color: #98A6A2; text-transform: uppercase; }
.info-value { font-size: 14px; font-weight: 500; color: #152421; }
.info-value--text { font-size: 13px; line-height: 1.5; white-space: pre-line; }
.info-value--muted { color: #98A6A2; }

.coaches-list { display: flex; flex-direction: column; gap: 16px; }
.coach-item { display: flex; align-items: center; gap: 12px; }
.coach-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 13px; flex-shrink: 0;
}
.coach-details { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.coach-name { font-size: 14px; font-weight: 600; color: #152421; }
.coach-role { font-size: 12px; color: #6D7D79; }
.coach-phone { font-size: 13px; font-weight: 500; color: #152421; white-space: nowrap; }

.soon-text { font-size: 13px; color: #6D7D79; line-height: 1.5; }
.inline-link { color: #2E8B57; font-weight: 600; text-decoration: none; }
.inline-link:hover { text-decoration: underline; }
</style>
