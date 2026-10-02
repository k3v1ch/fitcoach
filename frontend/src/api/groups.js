import { api } from './index'

export const groupsApi = {
  list(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/groups`, params)
  },
  get(organizationId, groupId, params = {}) {
    return api.get(`/organizations/${organizationId}/groups/${groupId}`, params)
  },
  create(organizationId, data) {
    return api.post(`/organizations/${organizationId}/groups`, data)
  },
  update(organizationId, groupId, data) {
    return api.patch(`/organizations/${organizationId}/groups/${groupId}`, data)
  },
  addAthlete(organizationId, groupId, athleteId, joinedOn) {
    return api.post(`/organizations/${organizationId}/groups/${groupId}/athletes`, { athleteId, joinedOn })
  },
  removeAthlete(organizationId, groupId, athleteId, leftOn) {
    return api.patch(`/organizations/${organizationId}/groups/${groupId}/athletes/${athleteId}`, { leftOn })
  }
}