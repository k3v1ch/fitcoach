import { ref, computed } from 'vue'
import { authApi } from '../api/auth'
import { organizationsApi } from '../api/organizations'
import { athletesApi } from '../api/athletes'
import { resetCsrf } from '../api/index'

const ORGANIZATION_KEY = 'fitcoach.organizationId'
const ATHLETE_KEY = 'fitcoach.athleteId'

export const currentUser = ref(null)
export const organizations = ref([])
export const currentOrganization = ref(null)

// Кабинеты родителя и спортсмена: свои карточки спортсменов и выбранный ребёнок
export const myAthletes = ref([])
export const selectedAthleteId = ref(readKey(ATHLETE_KEY))
export const selectedAthlete = computed(() => myAthletes.value.find(a => a.id === selectedAthleteId.value) || null)

function readKey(key) {
  try { return localStorage.getItem(key) } catch (_) { return null }
}

function readSavedOrganizationId() {
  try { return localStorage.getItem(ORGANIZATION_KEY) } catch (_) { return null }
}

function saveOrganizationId(id) {
  try {
    if (id) localStorage.setItem(ORGANIZATION_KEY, id)
    else localStorage.removeItem(ORGANIZATION_KEY)
  } catch (_) { /* приватный режим браузера */ }
}

// /me возвращает только аккаунт; организации, роли и права отдаёт GET /organizations.
export async function loadSession() {
  const [user, list] = await Promise.all([authApi.me(), organizationsApi.list()])
  currentUser.value = user
  organizations.value = list || []
  const savedId = readSavedOrganizationId()
  currentOrganization.value =
    organizations.value.find(o => o.organizationId === savedId) || organizations.value[0] || null
  saveOrganizationId(currentOrganization.value?.organizationId)
  return user
}

// Для навигации: сессия уже загружена или восстанавливается по cookie после перезагрузки страницы.
export async function ensureSession() {
  if (currentUser.value) return true
  try {
    await loadSession()
    return true
  } catch (_) {
    return false
  }
}

export function clearSession() {
  currentUser.value = null
  organizations.value = []
  currentOrganization.value = null
  myAthletes.value = []
  selectAthlete(null)
  resetCsrf()
}

// Свои карточки: дети родителя (по parentLinks) и карточка спортсмена (по userId).
// Сервер уже ограничивает список для родителя и спортсмена; фильтр нужен, если у пользователя есть и роль тренера.
export async function loadMyAthletes() {
  const page = await athletesApi.list(getOrganizationId(), { size: 100 })
  const userId = currentUser.value?.userId
  myAthletes.value = (page.items || []).filter(a =>
    a.userId === userId || (a.parents || []).some(p => p.parentUserId === userId))
  if (!myAthletes.value.some(a => a.id === selectedAthleteId.value)) {
    selectAthlete(myAthletes.value[0]?.id || null)
  }
  return myAthletes.value
}

export function selectAthlete(id) {
  selectedAthleteId.value = id
  try {
    if (id) localStorage.setItem(ATHLETE_KEY, id)
    else localStorage.removeItem(ATHLETE_KEY)
  } catch (_) { /* приватный режим браузера */ }
}

// Переключение организации: роли, права и все данные экранов другие — поэтому стартовая страница
// новой роли открывается с полной перезагрузкой
export function switchOrganization(id) {
  const org = organizations.value.find(o => o.organizationId === id)
  if (!org || org.organizationId === currentOrganization.value?.organizationId) return
  saveOrganizationId(org.organizationId)
  currentOrganization.value = org
  myAthletes.value = []
  selectAthlete(null)
  window.location.assign(homePath())
}

export async function logout() {
  try {
    await authApi.logout()
  } catch (e) {
    // 401: серверная сессия уже закончилась — выход по сути выполнен
    if (e.status !== 401) throw e
  } finally {
    clearSession()
  }
}

// Стартовая страница по роли в текущей организации.
export function homePath() {
  const roles = currentOrganization.value?.roles || []
  if (roles.includes('TRAINER') || roles.includes('AGENCY')) return '/trainer/dashboard'
  if (roles.includes('PARENT')) return '/parent/dashboard'
  if (roles.includes('ATHLETE')) return '/athlete/dashboard'
  return '/onboarding'
}

export function getOrganizationId() {
  if (!currentOrganization.value) {
    throw new Error('Нет активной организации')
  }
  return currentOrganization.value.organizationId
}

export function hasRole(role) {
  return currentOrganization.value?.roles?.includes(role)
}

export function hasPermission(code) {
  return currentOrganization.value?.permissions?.includes(code)
}
