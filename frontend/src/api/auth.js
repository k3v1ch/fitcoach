import { api, fetchCsrf } from './index'

export const authApi = {
  async login(email, password) {
    await fetchCsrf()
    return api.post('/auth/login', { email, password })
  },
  logout() {
    return api.post('/auth/logout')
  },
  me() {
    return api.get('/me')
  },
  // RegistrationRequest на бэкенде: только email и accountType (лишнее поле → 400). Имя задаётся при подтверждении.
  register(email, accountType = 'ATHLETE') {
    return api.post('/auth/register', { email, accountType })
  },
  registerConfirm(token, fullName, password, accountType = 'ATHLETE') {
    return api.post('/auth/register/confirm', { token, fullName, password, accountType })
  },
  requestPasswordReset(email) {
    return api.post('/auth/password-reset/request', { email })
  },
  confirmPasswordReset(token, newPassword) {
    return api.post('/auth/password-reset/confirm', { token, newPassword })
  },
  changePassword(currentPassword, newPassword) {
    return api.put('/auth/password', { currentPassword, newPassword })
  }
}