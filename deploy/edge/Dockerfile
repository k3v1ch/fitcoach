# Образ edge: сборка фронтенда (если в frontend/ есть package.json) и nginx.
# Контекст сборки — корень репозитория; лишнее отсекает Dockerfile.dockerignore.
FROM node:22-alpine AS frontend
WORKDIR /src
COPY frontend/ ./
COPY deploy/edge/placeholder/ /placeholder/
RUN set -eu; \
    if [ -f package.json ]; then \
      if [ -f package-lock.json ]; then npm ci --no-audit --no-fund; else npm install --no-audit --no-fund; fi; \
      npm run build; \
      test -f dist/index.html; \
    else \
      mkdir -p dist && cp -R /placeholder/. dist/; \
    fi

FROM nginx:stable-alpine
RUN rm -f /etc/nginx/conf.d/default.conf
COPY deploy/edge/nginx.conf /etc/nginx/nginx.conf
COPY deploy/edge/snippets/ /etc/nginx/snippets/
COPY deploy/edge/templates/ /etc/nginx/templates/
COPY --from=frontend /src/dist/ /usr/share/nginx/html/
