import { api, apiUrl } from './index'

export const documentsApi = {
  list(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/documents`, params)
  },
  get(organizationId, documentId) {
    return api.get(`/organizations/${organizationId}/documents/${documentId}`)
  },
  // Документ создаётся вместе с файлом: multipart (file, title, type, athleteId?, issuedOn?, validUntil?)
  create(organizationId, { file, title, type, athleteId, issuedOn, validUntil }) {
    const form = new FormData()
    form.append('file', file)
    form.append('title', title)
    form.append('type', type)
    if (athleteId) form.append('athleteId', athleteId)
    if (issuedOn) form.append('issuedOn', issuedOn)
    if (validUntil) form.append('validUntil', validUntil)
    return api.postForm(`/organizations/${organizationId}/documents`, form)
  },
  update(organizationId, documentId, data) {
    return api.patch(`/organizations/${organizationId}/documents/${documentId}`, data)
  }
}

export const filesApi = {
  upload(organizationId, file) {
    const form = new FormData()
    form.append('file', file)
    return api.postForm(`/organizations/${organizationId}/files`, form)
  },
  remove(organizationId, fileId) {
    return api.del(`/organizations/${organizationId}/files/${fileId}`)
  },
  // Ссылка для скачивания (Content-Disposition: attachment)
  contentUrl(organizationId, fileId) {
    return apiUrl(`/organizations/${organizationId}/files/${fileId}/content`)
  }
}
