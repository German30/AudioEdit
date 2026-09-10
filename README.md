# Auralis — Frontend

Angular 20 SPA (standalone components, signals, SCSS) for **Auralis**, a
professional audio-editing SaaS. It talks to the Spring Boot backend in
`../AudioEdit-Back`.

## Requirements

| Tool | Notes |
|------|-------|
| Node 22 + npm | for the local dev workflow |
| Docker | to build / run the frontend container |

The backend (`../AudioEdit-Back`) is a **separate deployment** with its own
image and compose file — start it however you like; this project only needs to
reach it over the network.

## Run it — Docker (standalone image)

The frontend is a self-contained nginx image: it serves the built SPA and
reverse-proxies `/api`, `/oauth2`, `/login/oauth2` and `/actuator/health` to
whatever `BACKEND_ORIGIN` points at. Nothing here depends on the backend
containers.

```bash
cp .env.example .env          # set BACKEND_ORIGIN to your API URL
docker compose up -d --build  # -> http://localhost:4200
```

Or without compose:

```bash
docker build -t auralis-frontend .
docker run --rm -p 4200:4200 \
  -e BACKEND_ORIGIN=https://api.your-domain.example \
  auralis-frontend
```

| Env | Default | Meaning |
|-----|---------|---------|
| `BACKEND_ORIGIN` | `http://host.docker.internal:8080` | full origin of the API (`scheme://host:port`, no path) |
| `RESOLVER` | `127.0.0.11` | DNS nginx uses to resolve that host at request time (Docker's embedded DNS; change for k8s / bare metal) |
| `WEB_PORT` | `4200` | host port `compose.yaml` publishes |

The SPA is built with a **relative** API base (`/api/v1`), so the browser only
ever talks to this container — no CORS between browser and API. nginx uses a
`resolver` + a variable `proxy_pass`, so the container starts even if the
backend is down and picks up DNS changes without a reload.

**On the backend deployment** set, so it trusts this frontend:

```
FRONTEND_REDIRECT_URI          = http://localhost:4200/oauth/callback   # this app's public URL
CORS_ORIGINS                   = http://localhost:4200
SERVER_FORWARD_HEADERS_STRATEGY = framework                             # honour X-Forwarded-*
AURALIS_AUTH_DEV_LOGIN_ENABLED = true                                   # optional: email/name dev-login
```

## Run it — local dev

```bash
npm install
npm start        # http://localhost:4200
```

`environment.development.ts` points the API at `http://localhost:8080/api/v1`, so
start the backend first (in `../AudioEdit-Back`).

The dev server and the `web` container both use port 4200 — run one or the
other, not both.

```bash
npm run build    # production bundle -> dist/audio-edit-front/browser
npm test         # unit tests (Karma/Jasmine)
```

## Layout

| Path | Responsibility |
|------|----------------|
| `src/app/core/models` | TypeScript types mirroring the backend DTOs |
| `src/app/core/api` | one service per backend area (auth, plans, subscription, assets, projects, arrangement, versions, renders, admin) |
| `src/app/core/auth` | session service (signals + `localStorage`), bearer/refresh interceptor, error interceptor, route guards |
| `src/app/shared` | reusable UI: icon set, spinner, usage meter, empty state, toast host, waveform, page header |
| `src/app/features` | pages — `marketing` (landing + pricing), `auth`, `shell`, `dashboard`, `assets`, `subscription`, `settings`, `admin`, `editor` |

The project **editor** (`features/editor`) is a read-only scaffold today: it
renders the timeline from `/graph` and already does version snapshot/restore and
render enqueue + polling, but clip/effect editing is not built yet.

## Docker files

| File | Purpose |
|------|---------|
| `Dockerfile` | multi-stage: Node build → `nginx-unprivileged` (uid 101, port 4200) serving `dist/.../browser` |
| `nginx/default.conf.template` | vhost — SPA fallback, gzip, immutable asset caching, `resolver` + variable `proxy_pass` to `${BACKEND_ORIGIN}` (substituted at start) |
| `compose.yaml` | runs the frontend container only; `BACKEND_ORIGIN` / `RESOLVER` / `WEB_PORT` from `.env` |
| `.env.example` | the three knobs above + the backend-side settings this frontend needs |
