// Русские подписи и цвета для значений перечислений API (контракт: backend/sports-organization-api.md).
// Использование: label('trainingStatus', t.status) → «Запланирована»; tone(t.status) → 'blue' (класс status-blue).

export const LABELS = {
  athleteStatus: { ACTIVE: 'Активен', ARCHIVED: 'В архиве' },
  recordStatus: { ACTIVE: 'Активна', ARCHIVED: 'В архиве' }, // секции, группы, элементы справочников
  memberStatus: { ACTIVE: 'Активен', BLOCKED: 'Заблокирован' },
  role: { TRAINER: 'Тренер', PARENT: 'Родитель', ATHLETE: 'Спортсмен', AGENCY: 'Представитель ведомства' },
  trainingStatus: { PLANNED: 'Запланирована', COMPLETED: 'Проведена', CANCELLED: 'Отменена' },
  reportStatus: { DRAFT: 'Черновик', CLOSED: 'Закрыт' },
  attendanceStatus: { UNMARKED: 'Не отмечен', PRESENT: 'Был', SICK: 'Болел', ABSENT: 'Не был' },
  eventType: { CAMP: 'Сбор', COMPETITION: 'Соревнование', MEDICAL_EXAM: 'Медосмотр', FUNDRAISER: 'Денежный сбор' },
  eventStatus: { DRAFT: 'Черновик', PUBLISHED: 'Опубликовано', CANCELLED: 'Отменено', COMPLETED: 'Завершено' },
  participantResponse: { PENDING: 'Ждём ответа', ACCEPTED: 'Участвует', DECLINED: 'Отказался' },
  announcementStatus: { DRAFT: 'Черновик', PUBLISHED: 'Опубликовано', ARCHIVED: 'В архиве' },
  announcementResponse: { ACCEPTED: 'Согласен', DECLINED: 'Не согласен' },
  documentType: { MEDICAL_CERTIFICATE: 'Медицинская справка', CONSENT: 'Согласие', OTHER: 'Документ' },
  chargeType: { SUBSCRIPTION: 'Абонемент', TRAINING: 'Разовое занятие', EVENT: 'Мероприятие или сбор' },
  chargeStatus: { ACTIVE: 'Действует', CANCELLED: 'Отменено' },
  paymentStatus: { UNPAID: 'Не оплачено', PARTIALLY_PAID: 'Оплачено частично', PAID: 'Оплачено' },
  paymentRecordStatus: { ACTIVE: 'Проведён', VOIDED: 'Аннулирован' },
  paymentMethod: { SBP: 'СБП', TRANSFER: 'Перевод', CASH: 'Наличные', OTHER: 'Другое' },
  standardStatus: { NOT_ASSESSED: 'Не оценивался', MET: 'Выполнен', NOT_MET: 'Не выполнен' },
  reportType: { ATTENDANCE: 'Посещаемость', TRAININGS: 'Тренировки', PROGRESS: 'Результаты', CHARGES: 'Начисления' },
  dictionaryType: { 'sport-types': 'Виды спорта', 'training-types': 'Типы тренировок', venues: 'Площадки' }
}

export function label(kind, value) {
  if (value === null || value === undefined || value === '') return '—'
  return LABELS[kind]?.[value] ?? String(value)
}

// Варианты для <select>: options('paymentMethod') → [{ value: 'SBP', label: 'СБП' }, …]
export function options(kind) {
  return Object.entries(LABELS[kind] || {}).map(([value, text]) => ({ value, label: text }))
}

// Цвет бейджа статуса: green | yellow | red | blue | gray. В экранах — класс `status-${tone(value)}`.
const TONES = {
  ACTIVE: 'green', PRESENT: 'green', COMPLETED: 'green', PAID: 'green', ACCEPTED: 'green', MET: 'green', CLOSED: 'green',
  PLANNED: 'blue', PUBLISHED: 'blue',
  PARTIALLY_PAID: 'yellow', PENDING: 'yellow', DRAFT: 'yellow', SICK: 'yellow',
  CANCELLED: 'red', ABSENT: 'red', UNPAID: 'red', DECLINED: 'red', NOT_MET: 'red', BLOCKED: 'red',
  UNMARKED: 'gray', NOT_ASSESSED: 'gray', VOIDED: 'gray', ARCHIVED: 'gray'
}

export function tone(value) {
  return TONES[value] || 'gray'
}
