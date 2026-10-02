import { api } from './index'

export const trainingsApi = {
  list(organizationId, params) {
    return api.get(`/organizations/${organizationId}/trainings`, params)
  },
  get(organizationId, trainingId) {
    return api.get(`/organizations/${organizationId}/trainings/${trainingId}`)
  },
  create(organizationId, data) {
    return api.post(`/organizations/${organizationId}/trainings`, data)
  },
  update(organizationId, trainingId, data) {
    return api.patch(`/organizations/${organizationId}/trainings/${trainingId}`, data)
  },
  cancel(organizationId, trainingId, cancelReason) {
    return api.patch(`/organizations/${organizationId}/trainings/${trainingId}`, {
      status: 'CANCELLED',
      cancelReason
    })
  },
  saveReport(organizationId, trainingId, data) {
    return api.put(`/organizations/${organizationId}/trainings/${trainingId}/report`, data)
  },
  saveAttendance(organizationId, trainingId, items) {
    return api.put(`/organizations/${organizationId}/trainings/${trainingId}/attendance`, { items })
  }
}