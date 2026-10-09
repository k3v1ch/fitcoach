import { api } from './index'

// Уведомления текущего пользователя в организации:
// NotificationPage { items: [{ id, type, title, text, entityType, entityId, createdAt, readAt }], …, unreadCount }
export const notificationsApi = {
  list(organizationId, params = {}) {
    return api.get(`/organizations/${organizationId}/notifications`, params)
  },
  read(organizationId, notificationId) {
    return api.put(`/organizations/${organizationId}/notifications/${notificationId}/read`)
  },
  readAll(organizationId) {
    return api.put(`/organizations/${organizationId}/notifications/read-all`)
  }
}
