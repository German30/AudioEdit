# Auralis Backend

Spring Boot 3.4 / Java 21 API for the Auralis professional audio-editing SaaS
platform. Rebuilds the original hackathon toy as a real service: OAuth2 login,
plan-based subscriptions with quota enforcement, S3-compatible asset storage,
a non-destructive edit model, and asynchronous FFmpeg rendering.

## Requirements

| Tool | Notes |
|------|-------|
| JDK 21 | `java -version` should report 21 (only for the dev workflow) |
| Maven 3.9+ | or `./mvnw` (only for the dev workflow) |
| Docker | the whole stack (API + MySQL + MinIO) runs from `compose.yaml` |
| **ffmpeg + ffprobe** | needed when running the API outside Docker: on `PATH` or set `FFMPEG_PATH` / `FFPROBE_PATH`. The Docker image already bundles them |

Everything is in one `compose.yaml` (project `auralis`): `app` (the API),
`mysql` (host port **3307**), `minio` (`:9000`, console `:9001`) and `minio-init`
(creates the bucket).

## Run it — all in Docker

```bash
docker compose up -d --build
```

Starts MySQL + MinIO + the API container. The API runs the **prod** profile:
Flyway owns the schema (`db/migration/V1__baseline.sql`) and Hibernate only
validates it. API on `http://localhost:8080`.

## Run it — dev loop (API from your IDE / Maven)

```bash
docker compose up -d mysql minio minio-init   # infra only, no API container
mvn spring-boot:run                           # dev profile by default
```

The **dev** profile points the datasource at the compose MySQL on
`localhost:3307` and uses Hibernate `ddl-auto=update` (Flyway off), so the schema
always matches the entities. `spring-boot-docker-compose` auto-start is disabled
in dev (it would launch a second API on :8080), so start the infra yourself as
shown above.

- API base: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

### Auth in dev without real OAuth apps

```bash
curl -sX POST localhost:8080/api/v1/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","displayName":"You"}'
# -> { accessToken, refreshToken, expiresIn }  (dev profile only)
```

Send `Authorization: Bearer <accessToken>` on every other call. Refresh with
`POST /api/v1/auth/token { "refreshToken": "..." }` (rotates the refresh token).

Production login: `GET /oauth2/authorization/google` or `/github` → the provider →
`OAuth2LoginSuccessHandler` mints our tokens and redirects to
`auralis.auth.frontend-redirect-uri` with them in the query string.

## Build / test

```bash
mvn test            # unit tests (no infra needed)
mvn package         # -> target/auralis-backend.jar
mvn -Dtest=FilterGraphBuilderTest test   # single test class
```

## Configuration

Everything is overridable by environment variable — see `application.yml`.
Notable ones:

| Var | Default (dev) | Meaning |
|-----|---------------|---------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `3306` / `auralis` / `root` / `root123` | MySQL |
| `JWT_SECRET` | dev-only Base64 HMAC key | **set a real 32+ byte Base64 secret in prod** |
| `GOOGLE_CLIENT_ID` / `_SECRET`, `GITHUB_CLIENT_ID` / `_SECRET` | `changeme` | OAuth2 apps |
| `S3_ENDPOINT` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_BUCKET` / `S3_PATH_STYLE` | MinIO local | object storage |
| `FFMPEG_PATH` / `FFPROBE_PATH` | `ffmpeg` / `ffprobe` | media binaries |
| `FRONTEND_REDIRECT_URI` | `http://localhost:5173/oauth/callback` | where OAuth2 login lands |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | allowed SPA origins |

## Module map

| Package | Responsibility |
|---------|----------------|
| `common` | `BaseEntity` (UUID PK, audit, optimistic lock), `ApiError` + `GlobalExceptionHandler`, `PageResponse` |
| `config` | `SecurityConfig` (OAuth2 login + JWT resource server, stateless, CORS), OpenAPI, async pools |
| `auth` | `User` / `RefreshToken`, `JwtService`, `AuthService` (login/refresh/rotate/logout), `CurrentUser`, dev-login. Emits `UserRegisteredEvent` |
| `subscription` | `Plan` + `PlanEntitlements`, `Subscription`, `UsageCounter`; `SubscriptionService` (lifecycle, listens for `UserRegisteredEvent` → Free plan), `QuotaService` (enforces every limit), `PlanSeeder`, billing webhook stub |
| `storage` | `StorageService` abstraction + `S3StorageService` (presigned upload/download), `StorageKeys` |
| `media` | `FfmpegRunner` (ProcessBuilder wrapper), `ProbeService` (ffprobe → `MediaInfo`) |
| `asset` | `AudioAsset` upload lifecycle (presign → PUT → complete), `AssetAnalyzer` (async ffprobe + `WaveformService` peaks), delete guarded by `AssetUsageChecker` |
| `project` | `Project` / `ProjectMember` / `Track` / `Clip` / `Effect` / `EditVersion`; `ProjectAccessService` (per-project roles), `GraphAssembler` (entities ⇄ `ProjectGraph` JSON), `VersionService` (snapshot / restore) |
| `render` | `RenderJob` queue, `RenderWorker` (atomic claim + poll), `RenderExecutionService` + `engine/FilterGraphBuilder` (compiles a `ProjectGraph` into one ffmpeg `-filter_complex` pass), `RenderJobStore` (transactional edges) |
| `admin` | `/api/v1/admin/**` (ADMIN role): user enable/disable/role, plan pricing + entitlements |

## How a render works

1. `POST /api/v1/projects/{id}/renders` → `VersionService.captureForRender` freezes
   the current graph as an `EditVersion`, `QuotaService.assertRenderAllowed`
   checks format / bitrate / stems / monthly minutes, a `RenderJob` is saved `QUEUED`.
2. `RenderWorker` polls, atomically flips one job `QUEUED → RUNNING`
   (`RenderJobRepository.claim`) and submits it to the bounded `renderExecutor`.
3. `RenderExecutionService` rehydrates the graph, downloads each source asset from
   storage, and `FilterGraphBuilder` turns it into a single ffmpeg command:
   per clip → `atrim` / `areverse` / `atempo` (playback rate) / effect chain /
   `afade` / `volume` / `adelay` (timeline position); per track → `amix` + track
   effects + gain + `pan`; then a master `amix` + master effects + gain.
4. Output is probed for real duration, uploaded to storage, the job goes
   `SUCCEEDED`, and `QuotaService` records billed render-seconds + stored bytes.
   Failures retry up to `auralis.render.max-attempts`, then land `FAILED` with a
   message.

## Not built yet / intentionally stubbed

- Real payment-provider integration (`BillingWebhookController` accepts a
  normalized shape; signature verification is a TODO).
- Per-job render progress percentage (only 0 / 100 today).
- Frontend (`../frontend`, next milestone).
