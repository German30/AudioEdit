# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage: install deps (cached until package-lock changes), then produce
# the optimized production bundle in dist/audio-edit-front/browser.
# ---------------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /workspace

# Dependency layer — reused until the lockfile changes.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

# App sources + production build (no file-replacements => environment.ts,
# which points the API at the same origin: apiBaseUrl "/api/v1").
COPY angular.json tsconfig*.json ./
COPY src ./src
COPY public ./public
RUN npm run build

# ---------------------------------------------------------------------------
# Runtime stage: a standalone nginx image (unprivileged, uid 101, port 4200)
# that serves the built SPA and reverse-proxies API traffic to a backend
# running somewhere else. Nothing here depends on the backend being present.
# ---------------------------------------------------------------------------
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime

# BACKEND_ORIGIN : full origin of the API (scheme://host:port), no path.
# RESOLVER       : DNS server nginx uses to resolve that host at request time.
# Both MUST usually be overridden at deploy time. NGINX_ENVSUBST_FILTER keeps
# envsubst from touching nginx's own $variables in the template.
ENV BACKEND_ORIGIN=http://host.docker.internal:8080 \
    RESOLVER=127.0.0.11 \
    NGINX_ENVSUBST_FILTER='^(BACKEND_ORIGIN|RESOLVER)$'

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/dist/audio-edit-front/browser/ /usr/share/nginx/html/

EXPOSE 4200

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -qO- http://localhost:4200/healthz >/dev/null 2>&1 || exit 1

# The base image's entrypoint runs envsubst over /etc/nginx/templates/*.template
# and then starts nginx in the foreground.
