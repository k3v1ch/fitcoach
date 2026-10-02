# Расхождения ТЗ и текущего backend

Обнаружено при чтении контроллеров, DTO и сервисов. Это статический анализ, а не подтверждённые результаты HTTP-прогона.

| Область | ТЗ | Текущий код |
| --- | --- | --- |
| Регистрация | Первый шаг только email; подтверждение может содержать phone | DTO принимает accountType; phone в ConfirmRegistrationRequest отсутствует |
| Организации | Создаются при инициализации; нельзя создать через сайт/API | POST /organizations создаёт организацию и TRAINER-членство создателя |
| Текущий пользователь | /me включает данные доступов к организациям | CurrentUser содержит userId, email, fullName, appRole, expiresAt; доступы выдаёт GET /organizations |
| Создание ресурсов | Статусы определяются описанием API | Большинство POST-контроллеров возвращают 200; POST /organizations — 201 |
| Отчёты | CSV/PDF/XLSX, фильтры по спортсмену/группе/секции/тренеру | Только CSV; контроллер принимает from/to/page/size, дополнительные фильтры не передаёт в сервис |
| Большой отчёт | 422 REPORT_TOO_LARGE | Сервис выбрасывает OrganizationRequestException, handler возвращает 400 INVALID_FIELDS |
| Идемпотентность оплаты | Конфликт тела с уже использованным ключом — 409 | FinanceService.pay возвращает прежний платёж до сравнения тела |
| Конфликты состояния | Многие запреты изменения — 409 | Используется OrganizationRequestException → 400; automatic-e2e проверяет текущий статус |
| Файл документа | Модель нужно сверять со спецификацией | Document содержит вложенный file; ID файла берётся из file.id |

Методы из ТЗ без mapping в текущих контроллерах:

- GET /admin/users
- GET /admin/users/{userId}
- PATCH /admin/users/{userId}
- GET /organizations/{organizationId}/parents
- GET /organizations/{organizationId}/attendance
- GET /organizations/{organizationId}/announcements/{announcementId}/recipients
- GET /organizations/{organizationId}/announcements/{announcementId}/responses
- PATCH /organizations/{organizationId}/documents/{documentId}
- GET /organizations/{organizationId}/charges
- PATCH /organizations/{organizationId}/charges/{chargeId}
- POST /organizations/{organizationId}/payments/{paymentId}/void

Пути выше относительны `/api/v1`. Примеры запросов на эти методы есть в `manual-spec`; ожидаемый успешный статус соответствует ТЗ и на текущем сервере не гарантируется.

Дополнительные mappings текущего backend: GET/POST /organizations, POST /organizations/{organizationId}/members/parents и GET /organizations/{organizationId}/events/{eventId}/participants. Они включены в `manual`.

Регрессионные автоматические тесты адаптированы к текущему backend, чтобы отличать поломку реализованного поведения от незавершённых требований. Расширенные права, конкурентность, SMTP, истечение токенов/сессий и соответствие всем требованиям проверяются отдельно по ручному чек-листу.
