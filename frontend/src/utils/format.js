// Форматирование данных API для экранов. Даты API: date — 'YYYY-MM-DD', datetime — RFC 3339 со смещением,
// деньги — строка с двумя знаками ('1250.00').

const LOCALE = 'ru-RU'

// 'YYYY-MM-DD' → Date в полночь по местному времени (new Date('2026-10-03') дал бы полночь UTC)
export function parseDate(value) {
  if (!value) return null
  if (value instanceof Date) return value
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  return m ? new Date(+m[1], +m[2] - 1, +m[3]) : new Date(value)
}

export function formatDate(value, { withYear = true } = {}) {
  const d = parseDate(value)
  if (!d || isNaN(d)) return '—'
  return d.toLocaleDateString(LOCALE, withYear
    ? { day: 'numeric', month: 'long', year: 'numeric' }
    : { day: 'numeric', month: 'long' })
}

export function formatDateShort(value) {
  const d = parseDate(value)
  return !d || isNaN(d) ? '—' : d.toLocaleDateString(LOCALE, { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export function formatTime(value) {
  const d = parseDate(value)
  return !d || isNaN(d) ? '—' : d.toLocaleTimeString(LOCALE, { hour: '2-digit', minute: '2-digit' })
}

export function formatDateTime(value) {
  const d = parseDate(value)
  return !d || isNaN(d) ? '—' : `${formatDateShort(d)}, ${formatTime(d)}`
}

// «Пн», «Вт», …
export function formatWeekday(value) {
  const d = parseDate(value)
  if (!d || isNaN(d)) return ''
  const w = d.toLocaleDateString(LOCALE, { weekday: 'short' })
  return w.charAt(0).toUpperCase() + w.slice(1)
}

// '1250.00' → '1 250 ₽'; '1250.50' → '1 250,50 ₽'
export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '—'
  const n = Number(value)
  if (isNaN(n)) return String(value)
  return n.toLocaleString(LOCALE, { minimumFractionDigits: Number.isInteger(n) ? 0 : 2, maximumFractionDigits: 2 }) + ' ₽'
}

// Сумма для API: строка с двумя знаками
export function toMoney(value) {
  const n = Number(String(value).replace(',', '.').replace(/\s/g, ''))
  return isNaN(n) ? null : n.toFixed(2)
}

// Date → 'YYYY-MM-DD' по местному времени (для параметров date)
export function toIsoDate(value = new Date()) {
  const d = parseDate(value)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

// Date → RFC 3339 (для параметров datetime: from/to тренировок)
export function toIsoDateTime(value = new Date()) {
  return parseDate(value).toISOString()
}

// Границы периода для календаря: { from: Date, to: Date } — to не включается
export function periodRange(kind, anchor = new Date()) {
  const d = parseDate(anchor)
  const start = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  if (kind === 'day') return { from: start, to: new Date(start.getFullYear(), start.getMonth(), start.getDate() + 1) }
  if (kind === 'week') {
    const shift = (start.getDay() + 6) % 7 // неделя с понедельника
    const from = new Date(start.getFullYear(), start.getMonth(), start.getDate() - shift)
    return { from, to: new Date(from.getFullYear(), from.getMonth(), from.getDate() + 7) }
  }
  const from = new Date(start.getFullYear(), start.getMonth(), 1)
  return { from, to: new Date(from.getFullYear(), from.getMonth() + 1, 1) }
}

export function addDays(value, days) {
  const d = parseDate(value)
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + days, d.getHours(), d.getMinutes())
}

// Карточка спортсмена → «Иванов Иван Иванович»; участник/пользователь → fullName
export function fullName(person) {
  if (!person) return '—'
  if (person.fullName) return person.fullName
  return [person.lastName, person.firstName, person.middleName].filter(Boolean).join(' ') || '—'
}

export function initials(name) {
  return String(name || '').split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join('') || '?'
}

export function ageYears(birthDate) {
  const b = parseDate(birthDate)
  if (!b) return null
  const now = new Date()
  let age = now.getFullYear() - b.getFullYear()
  if (now < new Date(now.getFullYear(), b.getMonth(), b.getDate())) age--
  return age
}

// Ошибка API → текст для пользователя (поля — отдельно в error.fieldErrors)
export function errorText(error) {
  if (!error) return ''
  if (error.status === 403) return 'Недостаточно прав для этого действия.'
  if (error.status === 404) return 'Запись не найдена или недоступна.'
  if (error.status === 429) return 'Слишком много запросов. Подождите минуту.'
  if (error.status >= 500) return 'Сервер временно недоступен. Попробуйте ещё раз.'
  const fields = (error.fieldErrors || []).map(f => `${f.field}: ${f.message}`).join('; ')
  return [error.message, fields].filter(Boolean).join(' — ') || 'Не удалось выполнить запрос.'
}
