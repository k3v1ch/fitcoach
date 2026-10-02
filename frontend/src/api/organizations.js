import { api } from './index'

export const organizationsApi = {
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