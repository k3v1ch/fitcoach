import { api } from './index'

export const athletesApi = {
  list(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/athletes`, params)
  },
  get(organizationId, athleteId) {
    return api.get(`/organizations/${organizationId}/athletes/${athleteId}`)
  },
  create(organizationId, data) {
    return api.post(`/organizations/${organizationId}/athletes`, data)
  },
  update(organizationId, athleteId, data) {
    return api.patch(`/organizations/${organizationId}/athletes/${athleteId}`, data)
  },
  results(organizationId, athleteId, params = {}) {
    return api.get(`/organizations/${organizationId}/athletes/${athleteId}/results`, params)
  },
  addResult(organizationId, athleteId, data) {
    return api.post(`/organizations/${organizationId}/athletes/${athleteId}/results`, data)
  },
  updateResult(organizationId, athleteId, resultId, data) {
    return api.patch(`/organizations/${organizationId}/athletes/${athleteId}/results/${resultId}`, data)
  },
  deleteResult(organizationId, athleteId, resultId) {
    return api.del(`/organizations/${organizationId}/athletes/${athleteId}/results/${resultId}`)
  },
  standards(organizationId, athleteId, params = {}) {
    return api.get(`/organizations/${organizationId}/athletes/${athleteId}/standards`, params)
  },
  ranks(organizationId, athleteId, params = {}) {
    return api.get(`/organizations/${organizationId}/athletes/${athleteId}/ranks`, params)
  }
}