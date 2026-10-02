import { api } from './index'

export const attendanceApi = {
  list(organizationId, params) {
    return api.get(`/organizations/${organizationId}/attendance`, params)
  }
}