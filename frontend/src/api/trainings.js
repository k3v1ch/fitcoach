import { api } from './index'

export const trainingsApi = {
  list(organizationId, params) {
    return api.get(`/organizations/${organizationId}/trainings`, params)
  },
  // TrainingDetail { trainingId, training, report, attendance } — раздел без права на него приходит null;
  // attendance — отметки и участники без отметки (UNMARKED); родителю и спортсмену — только свои
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
  // Тело — массив AttendanceWrite [{ athleteId, status, reason, comment }], ответ — Attendance[]
  saveAttendance(organizationId, trainingId, items) {
    return api.put(`/organizations/${organizationId}/trainings/${trainingId}/attendance`, items)
  }
}