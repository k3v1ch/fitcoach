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
  },
  // Добавить активированный аккаунт родителя в организацию по email (роль PARENT)
  addParent(organizationId, email) {
    return api.post(`/organizations/${organizationId}/members/parents`, { email })
  },
  // Родители и их дети: Page<{ userId, fullName, email, athletes: [{ id, fullName }] }>
  parents(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/parents`, params)
  }
}