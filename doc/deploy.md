# ИС «ФитКоуч» — инструкция по развёртыванию

Документ раздела 5 ТЗ («Инструкция по развертыванию»). Описывает прод-стенд
`https://fitcoach.keldari.online` (сервер 45.151.182.64, Ubuntu 24.04) и установку с нуля.

## 1. Схема

```
Браузер ──HTTPS:443──▶ edge (nginx) ──▶ backend (Spring Boot :8080) ──▶ db (PostgreSQL 16)
            HTTP:80 → 301 на HTTPS        │                                   │
                                          └── том fitcoach_files (загрузки)    └── том fitcoach_pgdata
```

| Сервис | Образ | Наружу | Назначение |
|---|---|---|---|
| `edge` | `fitcoach.local/edge:<хеш frontend/ + deploy/edge/>` (nginx + сборка `frontend/`) | 80, 443 | TLS, заголовки защиты, лимиты запросов, раздача SPA, прокси `/api/`, Swagger |
| `backend` | `fitcoach.local/backend:<хеш backend/>` (из `backend/Dockerfile`) | нет | API `/api/v1`, работает от uid 10001 без capabilities |
| `db` | `postgres:16-alpine` | нет | БД; сеть `data` без выхода в интернет |
| `files-perms` | `alpine:3.20` | нет | разовый `chown` тома загрузок под uid 10001 |

Все файлы стенда — в каталоге `deploy/` репозитория:

- `deploy/docker-compose.yml` — прод-стек (compose-проект `fitcoach`);
- `deploy/edge/` — Dockerfile edge, `nginx.conf`, шаблон сайта, заголовки защиты, заглушка;
- `deploy/server/` — серверная часть: CI/CD, выкладка, бэкапы, firewall, SSH (`install.sh`);
- `deploy/.env.example` — шаблон переменных.

На сервере:

| Путь | Что там |
|---|---|
| `/opt/fitcoach/shared/.env` | секреты и настройки (root, 600) |
| `/opt/fitcoach/releases/<sha>` | выгруженные релизы (хранятся 5 последних) |
| `/opt/fitcoach/current` | ссылка на текущий релиз |
| `/opt/fitcoach/shared/tls` | ссылки на сертификат (Let's Encrypt или временный самоподписанный) |
| `/opt/fitcoach/shared/nginx/ratelimit-allow.conf` | IP без ограничения частоты запросов |
| `/var/lib/fitcoach/deploy-history` | история выкладок |
| `/var/log/fitcoach-ci/` | логи CI |
| `/var/backups/fitcoach/` | бэкапы |
| `/home/git/Iinformation-system` | git-репозиторий (push по SSH) |

## 2. Требования к серверу

- Ubuntu 22.04/24.04 LTS, 2 vCPU, от 2 ГБ RAM + 2 ГБ swap, 10 ГБ свободного диска.
- Docker 24+ с Compose v2, git, certbot, ufw, fail2ban.
- DNS: A-запись домена на IP сервера (в Cloudflare — режим DNS only, без проксирования).

## 3. Установка с нуля

```bash
# 1. Пакеты
apt-get update && apt-get install -y git certbot ufw fail2ban
curl -fsSL https://get.docker.com | sh

# 2. Пользователь и репозиторий git (push идёт по SSH под пользователем git)
adduser --disabled-password --gecos "" git
sudo -u git git clone --bare <источник> /home/git/Iinformation-system   # или существующий репозиторий
# в репозитории с рабочей копией: git config receive.denyCurrentBranch updateInstead

# 3. Серверная часть (скрипты, systemd-юниты, git-хуки, каталоги)
git clone /home/git/Iinformation-system /tmp/fc && cd /tmp/fc
deploy/server/install.sh base

# 4. Переменные окружения
cp deploy/.env.example /opt/fitcoach/shared/.env && chmod 600 /opt/fitcoach/shared/.env
#    задать DOMAIN, DATABASE_PASSWORD (openssl rand -hex 24), SMTP_*

# 5. Первая выкладка (временно с самоподписанным сертификатом)
fitcoach-deploy $(git -C /tmp/fc rev-parse HEAD)

# 6. Сертификат Let's Encrypt (продление — certbot.timer, nginx перечитывает сам)
fitcoach-cert issue

# 7. Защита
deploy/server/install.sh firewall   # ufw: только 22/80/443; DOCKER-USER: в контейнеры только 80/443
deploy/server/install.sh ssh        # root только по ключу; git — пароль или ключ, git-shell без туннелей
```

Проверка: `fitcoach-verify` — 25 проверок защиты и состояния; код выхода — число провалов.

## 4. Переменные `/opt/fitcoach/shared/.env`

| Переменная | Пример | Назначение |
|---|---|---|
| `DOMAIN` | `fitcoach.keldari.online` | домен сайта; из него строятся ссылки в письмах (`https://DOMAIN/activate?token=…`) |
| `DATABASE_NAME`, `DATABASE_USERNAME` | `sports_organization` | имя БД и пользователя |
| `DATABASE_PASSWORD` | `openssl rand -hex 24` | пароль БД |
| `SMTP_HOST`, `SMTP_PORT` | `smtp.mail.ru`, `465` | почтовый сервер для писем регистрации и восстановления |
| `SMTP_AUTH`, `SMTP_SSL_ENABLE` | `true`, `true` | для порта 465 (SSL); для 587 — `SMTP_STARTTLS_*=true` |
| `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM` | | учётная запись отправителя |

Фиксированные настройки бэкенда задаёт `deploy/docker-compose.yml`: `SESSION_COOKIE_SECURE=true`,
`PUBLIC_BASE_URL=https://${DOMAIN}`, `SERVER_FORWARD_HEADERS_STRATEGY=native` (реальный IP клиента
из заголовков nginx), `JAVA_TOOL_OPTIONS` (лимит памяти JVM).

## 5. CI/CD

Выкладка идёт через `git push`; отдельных CI-сервисов нет.

1. Хук `post-receive` ставит задание в `/var/spool/fitcoach-ci/` и показывает ход сборки прямо в терминале
   пушащего (строки `remote: [CI] …`). Ctrl+C не останавливает сборку.
2. Воркер `fitcoach-ci` (systemd, root) для **любой ветки**:
   - `mvn test` бэкенда во временном контейнере с временным PostgreSQL;
   - сборка образов `backend` и `edge` (включая `npm ci && npm run build` фронтенда).
3. Для ветки **`master`** при успехе — `fitcoach-deploy <sha>`: бэкап БД, `docker compose up -d`,
   проверка здоровья (healthcheck backend и edge + запрос через nginx). Если проверка не прошла —
   автоматический откат на предыдущий релиз.
4. Хук `update` запрещает удалять `master` и делать в него force-push.

```bash
git push origin master                         # тесты → сборка → выкладка
git push origin feature/x                      # только тесты и сборка
ssh git@fitcoach.keldari.online ci-status      # последние 10 прогонов
ssh git@fitcoach.keldari.online ci-status <id> # хвост лога прогона
```

Вручную на сервере (root):

```bash
fitcoach-deploy --status        # текущий релиз, история, контейнеры
fitcoach-deploy <sha40>         # выложить конкретный коммит
fitcoach-deploy --rollback      # вернуть предыдущий релиз
fitcoach-deploy --tags <sha40>  # теги образов коммита: что пересоберётся и перезапустится
fitcoach-deploy --cleanup       # убрать старые релизы и неиспользуемые образы
touch /opt/fitcoach/shared/ci-skip-tests   # временно выкладывать без тестов (удалить файл после!)
```

Откат возвращает код и образы, но не схему БД: миграции Flyway назад не откатываются. Перед каждой
выкладкой делается бэкап `pre-deploy-<sha>` — из него можно восстановить БД (раздел 7).

Образы помечаются по содержимому: тег `backend` — хеш каталога `backend/`, тег `edge` — хеш `frontend/`
и `deploy/edge/`. Что не менялось, то не пересобирается и не перезапускается, а тесты бэкенда
для уже проверенного кода не повторяются. Время простоя при выкладке:

| Что изменилось в коммите | Что видит пользователь |
|---|---|
| только документация или прочее | ничего, контейнеры не перезапускаются |
| `frontend/` или `deploy/edge/` | перезапуск nginx, 1–2 с |
| `backend/` | сайт открывается, API 30–60 с отвечает 502, пока стартует новый бэкенд; затем 1–2 с перезапуск nginx |

Серверные скрипты (`/usr/local/sbin/fitcoach-*`, git-хуки, systemd-юниты) CI **не обновляет**: иначе
любой, кто может пушить в `master`, получил бы root на сервере. После изменений в `deploy/server/`
DevOps ставит их вручную: `deploy/server/install.sh base` из свежей копии репозитория.

## 6. TLS

- Сертификат Let's Encrypt: `fitcoach-cert issue` (проверка через `/.well-known/acme-challenge/` на порту 80).
- Продление: `certbot.timer` дважды в сутки; хук `/etc/letsencrypt/renewal-hooks/deploy/fitcoach-reload-edge.sh`
  перечитывает nginx.
- Пока сертификата нет, `fitcoach-cert` подставляет самоподписанный — сайт работает, браузер предупреждает.
- Проверка: `certbot certificates`, `certbot renew --dry-run`.

## 7. Бэкапы и восстановление

`fitcoach-backup.timer` — ежедневно в 03:30 UTC; также перед каждой выкладкой.
Каталог `/var/backups/fitcoach/<UTC>-<метка>/`: `db.dump` (pg_dump -Fc), `files.tar.gz` (загрузки),
`repo.bundle` (весь git), `SHA256SUMS`. Хранятся 14 дней.

```bash
fitcoach-backup manual                       # внеочередной бэкап
cd /var/backups/fitcoach/<каталог> && sha256sum -c SHA256SUMS

# Восстановить БД (поверх текущей)
docker exec -i fitcoach-db-1 sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' < db.dump

# Восстановить загруженные файлы
docker run --rm -i -v fitcoach_files:/data alpine:3.20 sh -c 'rm -rf /data/* && tar -xzf - -C /data' < files.tar.gz
docker compose -p fitcoach -f /opt/fitcoach/current/deploy/docker-compose.yml --env-file /opt/fitcoach/shared/.env up files-perms

# Восстановить репозиторий
git clone --mirror repo.bundle /home/git/restored.git
```

Бэкапы лежат на том же диске; копию за пределы сервера стоит снимать отдельно.

## 8. Логи и состояние

```bash
fitcoach-verify                                         # сводная проверка
docker ps --filter label=com.docker.compose.project=fitcoach
docker logs -f fitcoach-backend-1                       # логи бэкенда (SLF4J)
docker logs -f fitcoach-edge-1                          # access/error log nginx
journalctl -u fitcoach-ci.service                       # запуски CI
systemctl list-timers fitcoach-backup.timer certbot.timer
```

Логи контейнеров ротируются Docker (json-file, 3 × 50 МБ).

## 9. Контракт для фронтенда

- Проект Vite в `frontend/`; сборка `npm ci && npm run build` → `frontend/dist/` (в образ edge).
  `node_modules` в git не нужен — он игнорируется при сборке.
- API — по относительному пути `/api/v1` (тот же домен): cookie сессии и CSRF работают без CORS.
- Vue Router в режиме history: неизвестные пути nginx отдаёт как `index.html`.
- Ссылки из писем бэкенда: `/activate?token=…` (регистрация), `/reset-password?token=…` (восстановление).
- CSP: скрипты только с своего домена; стили и шрифты — свой домен и Google Fonts.
  Новый внешний ресурс → правка `deploy/edge/snippets/security-headers.conf`.
- Локальная разработка с API прода: `VITE_PROXY_TARGET=https://fitcoach.keldari.online npm run dev`.

## 10. Частые проблемы

| Симптом | Причина и решение |
|---|---|
| `502` на запросах API 30–60 с после пуша в master | менялся `backend/`: идёт выкладка, стартует новый бэкенд |
| `429 Too Many Requests` | лимит nginx: `/api` — 20 запросов/с с IP, вход/регистрация — 10 в минуту. Для нагрузочного теста добавьте IP в `ratelimit-allow.conf` (`203.0.113.10 1;`) и `docker exec fitcoach-edge-1 nginx -s reload` |
| `ssh: Connection refused` с вашего IP | fail2ban: 3 неудачные попытки → бан на сутки. Снять: `fail2ban-client set sshd unbanip <IP>` |
| `git push`: `Permission denied` | git — по паролю команды или по ключу; root — только по ключу. 3 неверных пароля за 10 минут → бан IP на сутки |
| CI: `✖ тесты бэкенда упали` | `ssh git@… ci-status <id>` — вывод Maven; выкладка не выполнялась |
| Сборка падает по памяти | проверьте `free -h`, swap 2 ГБ должен быть включён |
