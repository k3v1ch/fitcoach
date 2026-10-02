const API_BASE = import.meta.env.VITE_API_URL || '/api/v1'

let csrfToken = null
let csrfHeaderName = 'X-CSRF-TOKEN'

export async function fetchCsrf() {
  const res = await fetch(`${API_BASE}/auth/csrf`, { credentials: 'include' })
  if (!res.ok) throw new Error('Не удалось получить CSRF-токен')
  const data = await res.json()
  csrfToken = data.token
  csrfHeaderName = data.headerName
  return data
}

export function resetCsrf() {
  csrfToken = null
}

// Вызывается, когда сервер ответил 401 UNAUTHENTICATED: серверной сессии больше нет
// (выкладка бэкенда, 8 часов с входа, смена пароля на другом устройстве).
let onUnauthenticated = null
export function setUnauthenticatedHandler(handler) {
  onUnauthenticated = handler
}

export async function apiFetch(path, options = {}, retried = false) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = { ...(options.headers || {}) }
  const isMutation = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)

  if (isMutation) {
    if (!csrfToken) await fetchCsrf()
    headers[csrfHeaderName] = csrfToken
  }

  const isFormData = options.body instanceof FormData
  if (!isFormData && options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...options, method, headers, credentials: 'include'
  })

  // Ошибка CSRF приходит как обычный 403: один раз берём свежий токен и повторяем запрос.
  if (res.status === 403 && isMutation && !retried) {
    resetCsrf()
    return apiFetch(path, options, true)
  }

  if (!res.ok) {
    let err = { code: 'UNKNOWN', message: `HTTP ${res.status}` }
    try { err = await res.json() } catch (_) {}
    if (res.status === 401 && err.code === 'UNAUTHENTICATED' && onUnauthenticated) onUnauthenticated()
    const error = new Error(err.message || `HTTP ${res.status}`)
    error.code = err.code
    error.status = res.status
    error.fieldErrors = err.fieldErrors || []
    error.requestId = err.requestId
    throw error
  }

  if (res.status === 204) return null
  const ct = res.headers.get('content-type') || ''
  if (ct.includes('application/json')) return res.json()
  return res.blob()
}

export const api = {
  get: (path, params) => {
    let qs = ''
    if (params) {
      const cleaned = {}
      Object.keys(params).forEach(k => {
        if (params[k] !== undefined && params[k] !== null && params[k] !== '') {
          cleaned[k] = params[k]
        }
      })
      qs = '?' + new URLSearchParams(cleaned).toString()
    }
    return apiFetch(`${path}${qs}`)
  },
  post: (path, body, extraHeaders = {}) =>
    apiFetch(path, { method: 'POST', body: JSON.stringify(body), headers: extraHeaders }),
  postForm: (path, formData) => apiFetch(path, { method: 'POST', body: formData }),
  patch: (path, body) => apiFetch(path, { method: 'PATCH', body: JSON.stringify(body) }),
  put: (path, body) => apiFetch(path, { method: 'PUT', body: JSON.stringify(body) }),
  del: (path) => apiFetch(path, { method: 'DELETE' })
}