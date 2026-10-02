import { api } from './index'

export const chargesApi = {
  list(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/charges`, params)
  },
  get(organizationId, chargeId) {
    return api.get(`/organizations/${organizationId}/charges/${chargeId}`)
  },
  create(organizationId, data) {
    return api.post(`/organizations/${organizationId}/charges`, data)
  },
  update(organizationId, chargeId, data) {
    return api.patch(`/organizations/${organizationId}/charges/${chargeId}`, data)
  },
  cancel(organizationId, chargeId, cancelReason) {
    return api.patch(`/organizations/${organizationId}/charges/${chargeId}`, {
      status: 'CANCELLED',
      cancelReason
    })
  }
}