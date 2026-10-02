import { api, apiFetch } from './index'

export const financeApi = {
  summary(organizationId, from, to, sectionId) {
    return api.get(`/organizations/${organizationId}/finance/summary`, { from, to, sectionId })
  },
  reports(organizationId, reportType, params = {}) {
    return api.get(`/organizations/${organizationId}/reports/${reportType}`, params)
  },
  exportReport(organizationId, reportType, params = {}) {
    const cleaned = { ...params, format: 'CSV' }
    const qs = '?' + new URLSearchParams(cleaned).toString()
    return apiFetch(`/organizations/${organizationId}/reports/${reportType}/export${qs}`)
  },
  types(organizationId) {
    return api.get(`/organizations/${organizationId}/reports/types`)
  }
}