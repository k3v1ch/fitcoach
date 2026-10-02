import { api, fetchCsrf, resetCsrf } from './index'

export const authApi = {
  async login(email, password) {
    await fetchCsrf()
    const user = await api.post('/auth/login', { email, password })
    // Вход меняет сессию и её CSRF-токен; в заголовке ответа он «сырой», а сервер принимает
    // только маскированный из GET /auth/csrf — следующий запрос на запись возьмёт его заново.
    resetCsrf()
    return user
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