import { api } from './index'

export const sectionsApi = {
  list(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/sections`, params)
  },
  create(organizationId, data) {
    return api.post(`/organizations/${organizationId}/sections`, data)
  },
  update(organizationId, sectionId, data) {
    return api.patch(`/organizations/${organizationId}/sections/${sectionId}`, data)
  }
}