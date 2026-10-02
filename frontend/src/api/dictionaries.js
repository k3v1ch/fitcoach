import { api } from './index'

export const dictionariesApi = {
  list(organizationId, dictionaryType, params = {}) {
    return api.get(`/organizations/${organizationId}/dictionaries/${dictionaryType}`, params)
  },
  create(organizationId, dictionaryType, data) {
    return api.post(`/organizations/${organizationId}/dictionaries/${dictionaryType}`, data)
  },
  update(organizationId, dictionaryType, itemId, data) {
    return api.patch(`/organizations/${organizationId}/dictionaries/${dictionaryType}/${itemId}`, data)
  }
}