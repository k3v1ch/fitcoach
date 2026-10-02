# Sports Organization API

Backend for the sports organization specification. The API uses Java 21, Spring Boot, PostgreSQL, Flyway, and cookie-based sessions with CSRF protection.

## Implementation Stages

1. **Foundation**: Spring Boot project, session-backed CSRF endpoint, PostgreSQL configuration, and identity/organization schema. Implemented.
2. **Authentication**: registration, email confirmation, login/logout, `/me`, password reset/change, and absolute session expiry are implemented.
3. **Organization access**: role-permission validation, organization profiles, and the member directory are implemented; full organization-scope enforcement for later modules remains.
4. **People and groups**: athlete cards, parent links, sections, groups, and membership history are implemented.
5. **Training and progress**: schedule, training reports, attendance, results, standards, and ranks are implemented.
6. **Events and communication**: events, participant responses, announcements, consent responses, documents, and private file storage are implemented.
7. **Finance and dictionaries**: charges, concurrent-safe manual payments, summaries, and editable dictionaries are implemented.
8. **Reporting and completion**: dashboard, activity storage, report catalog, table views, CSV exports, and OpenAPI documentation are implemented; seed data and end-to-end/security tests remain next.

## Current Slice

`GET /api/v1/auth/csrf` returns `{ "token": "...", "headerName": "X-CSRF-TOKEN" }` and creates a server-side session. The token must accompany state-changing requests.

`POST /api/v1/auth/register` accepts an email, full name, and optional phone, then returns `202`. Pending accounts receive a one-time activation link by SMTP. `POST /api/v1/auth/register/confirm` accepts that token and a 15-128 character password, sets an Argon2id hash, and activates the account. Tokens are stored only as SHA-256 hashes, expire after 30 minutes, and can be used once. Registration requests are rate-limited by email and source IP. Mail delivery happens after the database transaction commits; SMTP failure does not expose token data in the API or logs.

`POST /api/v1/auth/login` authenticates against PostgreSQL, rotates the session ID, creates an absolute eight-hour session expiry, and returns the current user. A fresh CSRF token is returned in the `X-CSRF-TOKEN` response header. `POST /api/v1/auth/logout` invalidates the session; `GET /api/v1/me` returns the profile and active memberships.

`POST /api/v1/auth/password-reset/request` always returns `202` with a neutral message. Active verified accounts receive a one-time 30-minute reset link. `POST /api/v1/auth/password-reset/confirm` changes the password and expires registered sessions. `PUT /api/v1/auth/password` verifies the current password, changes it, and likewise expires sessions. Passwords must contain 15-128 characters and are encoded with Argon2id.

The shared error response covers request validation, malformed JSON, invalid tokens, authentication failures, and rate limits.

`GET /api/v1/organizations/{organizationId}` returns the organization only to an active member. `PATCH` requires the `TRAINER` role and `organization.write`; the timezone must be a valid IANA zone. `GET /api/v1/organizations/{organizationId}/members` requires `members.read`, supports role/status/name filters and pagination, and returns no contact fields. Missing organizations and cross-organization access are represented as `404`.

`GET /api/v1/organizations/{organizationId}/athletes` supports name/status filters and pagination. Trainers and other members with `athletes.read` see the organization scope; parents and athletes are restricted to their linked or own cards. `POST` and `PATCH` require a trainer with `athletes.write`. Athlete accounts must have an active `ATHLETE` membership, and replacing `parentLinks` additionally requires `members.write` with active `PARENT` memberships. `GET` by id applies the same scope checks. Migration `V3__athletes_and_parent_links.sql` creates the cards and parent-link tables.

`GET/POST/PATCH /api/v1/organizations/{organizationId}/sections` and `GET/POST/PATCH /api/v1/organizations/{organizationId}/groups` implement section and group management. Groups validate active trainer memberships, keep coach assignments, expose active athlete counts, and support name/status/section/coach/athlete filters. Group details return current or historical composition; athlete enrollment and completion are transactional, preserve history, reject duplicate open periods, and reject new enrollment into archived groups. Migration `V4__sections_groups_and_membership.sql` creates these tables and constraints.

`GET/POST/PATCH /api/v1/organizations/{organizationId}/trainings` implements the schedule. Training searches use the required half-open time interval and stable `startsAt,id` ordering. Creation validates active groups and trainers, stage durations, and `endsAt > startsAt`; only planned trainings can be edited, and cancellation requires a reason. Migration `V5__trainings_reports_and_attendance.sql` creates training, coach, report, and attendance tables.

`PUT /api/v1/organizations/{organizationId}/trainings/{trainingId}/report` saves a draft or closes a report. Closing requires an elapsed training, all participants marked, and atomically changes the training to `COMPLETED`. `PUT /api/v1/organizations/{organizationId}/trainings/{trainingId}/attendance` snapshots the group composition on first use and updates only those participants; closed or cancelled trainings are read-only.

`GET/POST/PATCH/DELETE /api/v1/organizations/{organizationId}/athletes/{athleteId}/results` manages progress measurements with date, metric, unit, value, comment, and manual personal-best flag. `GET /standards` and `GET /ranks` expose read-only seeded records. Parents and athletes are restricted to their own accessible cards; writes require `TRAINER` and `progress.write`. Migration `V6__progress_standards_and_ranks.sql` creates the progress tables.

`GET/POST/PATCH /api/v1/organizations/{organizationId}/events` manages drafts and status transitions for camps, competitions, medical exams, and fundraisers. Ordinary events require time and location; fundraisers require a positive target and collection deadline. Participant membership can be added or removed while the event is editable, and linked parents/athletes can respond before the response deadline. Migration `V7__events_and_participants.sql` creates event and participant storage.

`GET/POST/PATCH /api/v1/organizations/{organizationId}/announcements` manages drafts, publication, archiving, recipient visibility, read markers, and consent response history. Recipients must be active members of the organization; only trainers with `announcements.write` can author or publish, while responses are accepted only for published announcements before their deadline. Migration `V8__announcements_and_responses.sql` creates announcement storage.

`POST /api/v1/organizations/{organizationId}/files` and the document endpoints use server-generated storage names outside the public web directory. Uploads are limited to 10 MB and validate PDF/PNG/JPEG extension, MIME type, and magic signature. Document access is scoped to the organization and the athlete's linked parent/athlete or a member with `documents.read`; `FILES_ROOT` configures the storage directory. Migration `V9__files_and_documents.sql` creates file metadata and documents.

`POST /api/v1/organizations/{organizationId}/charges` creates manual charges and `GET /charges/{chargeId}` returns calculated paid and remaining amounts. `POST /payments` requires `Idempotency-Key`, locks the charge row, rejects overpayment, and returns the previous payment on retry. Amounts use two-decimal PostgreSQL numerics. Migration `V10__charges_and_payments.sql` creates charges, payments, and the organization-scoped idempotency constraint.

`GET /api/v1/organizations/{organizationId}/finance/summary` calculates recorded receipts for an inclusive period and current outstanding/overdue debt. `GET/POST/PATCH /api/v1/organizations/{organizationId}/dictionaries/{dictionaryType}` supports `sport-types`, `training-types`, and `venues`; names are unique within an organization and addresses are accepted only for venues. Migration `V11__dictionaries.sql` creates dictionary storage.

`GET /api/v1/organizations/{organizationId}/dashboard` returns role-scoped counters, upcoming trainings/events, optional finance totals, and a paginated activity log. Parent and athlete views require their own accessible `athleteId`. Migration `V12__activity_log.sql` creates the activity journal without storing passwords or tokens.

`GET /api/v1/organizations/{organizationId}/reports/types` exposes available report sources. `GET /reports/{reportType}` provides paginated table data for attendance, trainings, progress, and charges; `/export?format=CSV` creates UTF-8 CSV with a 10,000-row limit and formula-prefix escaping. OpenAPI generation is intentionally not part of this slice.

Flyway creates the initial identity/organization tables and the athlete/parent-link tables. The application does not create organizations through the website.

## Run Tests

```powershell
mvn test
```

## Run the Application

Copy `.env.example` to `.env`, then start PostgreSQL and the API:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The API is available at `http://localhost:8080`. OpenAPI JSON is served at `http://localhost:8080/v3/api-docs`; Swagger UI is at `http://localhost:8080/swagger-ui.html`.

Compose waits for PostgreSQL to become healthy before starting the API. Database data and uploaded files are stored in named Docker volumes. To stop the services, run `docker compose down`; `docker compose down -v` also deletes the stored database and files.

The session cookie is `HttpOnly` and `SameSite=Lax`. `SESSION_COOKIE_SECURE=false` is set for local HTTP; set it to `true` behind HTTPS in production. OpenAPI documents session-cookie authentication and the `X-CSRF-TOKEN` header required for state-changing requests. Get the CSRF token from `GET /api/v1/auth/csrf` before calling a mutation.