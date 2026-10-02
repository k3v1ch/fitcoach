import { api, apiFetch } from './index'

export const paymentsApi = {
  list(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/payments`, params)
  },
  create(organizationId, data) {
    const key = (crypto.randomUUID && crypto.randomUUID()) || `k-${Date.now()}`
    return apiFetch(`/organizations/${organizationId}/payments`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { 'Idempotency-Key': key }
    })
  },
  void(organizationId, paymentId, reason) {
    return api.post(`/organizations/${organizationId}/payments/${paymentId}/void`, { reason })
  }
}