import { api } from './index'

export const dashboardApi = {
  get(organizationId, view, { athleteId, from, to, ...params } = {}) {
    return api.get(`/organizations/${organizationId}/dashboard`, {
      view, athleteId, from, to, ...params
    })
  }
}