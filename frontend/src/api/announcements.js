import { api } from './index'

export const announcementsApi = {
  list(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/announcements`, params)
  },
  get(organizationId, announcementId) {
    return api.get(`/organizations/${organizationId}/announcements/${announcementId}`)
  },
  create(organizationId, data) {
    return api.post(`/organizations/${organizationId}/announcements`, data)
  },
  update(organizationId, announcementId, data) {
    return api.patch(`/organizations/${organizationId}/announcements/${announcementId}`, data)
  },
  markRead(organizationId, announcementId) {
    return api.put(`/organizations/${organizationId}/announcements/${announcementId}/read`)
  },
  recipients(organizationId, announcementId, params = {}) {
    return api.get(`/organizations/${organizationId}/announcements/${announcementId}/recipients`, params)
  },
  responses(organizationId, announcementId, params = {}) {
    return api.get(`/organizations/${organizationId}/announcements/${announcementId}/responses`, params)
  },
  respond(organizationId, announcementId, response, comment = null) {
    return api.post(`/organizations/${organizationId}/announcements/${announcementId}/responses`, { response, comment })
  }
}