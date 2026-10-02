import { ref } from 'vue'
import { authApi } from '../api/auth'
import { organizationsApi } from '../api/organizations'
import { resetCsrf } from '../api/index'

const ORGANIZATION_KEY = 'fitcoach.organizationId'

export const currentUser = ref(null)
export const organizations = ref([])
export const currentOrganization = ref(null)

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
  resetCsrf()
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
