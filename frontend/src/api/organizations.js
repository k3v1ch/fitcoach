import { api } from './index'

export const organizationsApi = {
  // Организации текущего пользователя с ролями и правами (OrganizationAccess[])
  list() {
    return api.get('/organizations')
  },
  // Создатель автоматически получает роль TRAINER с полными правами
  create(data) {
    return api.post('/organizations', data)
  },
  get(organizationId) {
    return api.get(`/organizations/${organizationId}`)
  },
  update(organizationId, data) {
    return api.patch(`/organizations/${organizationId}`, data)
  },
  members(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/members`, params)
  }
}