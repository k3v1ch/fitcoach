import { api } from './index'

export const eventsApi = {
  list(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/events`, params)
  },
  get(organizationId, eventId, params = {}) {
    return api.get(`/organizations/${organizationId}/events/${eventId}`, params)
  },
  create(organizationId, data) {
    return api.post(`/organizations/${organizationId}/events`, data)
  },
  update(organizationId, eventId, data) {
    return api.patch(`/organizations/${organizationId}/events/${eventId}`, data)
  },
  participants(organizationId, eventId, params = {}) {
    return api.get(`/organizations/${organizationId}/events/${eventId}/participants`, params)
  },
  addParticipants(organizationId, eventId, athleteIds) {
    return api.put(`/organizations/${organizationId}/events/${eventId}/participants`, { athleteIds })
  },
  removeParticipant(organizationId, eventId, athleteId) {
    return api.del(`/organizations/${organizationId}/events/${eventId}/participants/${athleteId}`)
  },
  updateParticipant(organizationId, eventId, athleteId, data) {
    return api.patch(`/organizations/${organizationId}/events/${eventId}/participants/${athleteId}`, data)
  }
}