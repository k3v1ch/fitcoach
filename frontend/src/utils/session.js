import { ref } from 'vue'
import { authApi } from '../api/auth'

export const currentUser = ref(null)
export const currentOrganization = ref(null)

export async function loadSession() {
  const user = await authApi.me()
  currentUser.value = user
  currentOrganization.value = user.organizations?.[0] || null
  return user
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