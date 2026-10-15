# physician-app

Single, standalone web application for the **physician microservice** — not a Module Federation host or remote. Every physician screen is a local route in one deployable app.

> **Video conferencing is out of scope.** The consultation workspace provides clinical chat, read-only telemetry, patient context and consultation closure. Media sessions are owned by the HIS video component / `video-service` and are intentionally not rendered here.

## Screens (routes)

| Route | Screen | PRD |
|---|---|---|
| `/worklist` | Physician worklist — readiness, waiting time, technical status, flags, admit, red-flag acknowledgement | §12.3 (TM-WLT-005..009) |
| `/worklist/:appointmentId/context` | Patient context — demographics, insurance, medications, allergies, history | §13.2 (TM-CON-003) |
| `/consultation/:appointmentId` | Consultation workspace — clinical chat + telemetry (read-only) + closure (no video) | §13.2 / §14.2 |
| `/consultation/:appointmentId/documentation` | Documentation — diagnosis, prescription, medical leave (autosave) | §14 |
| `/providers` | Provider management — search/filter | §21 (TM-PRV-001..004) |
| `/providers/:providerId` | Provider detail — status change, availability, credentials, productivity metrics | §21 (TM-PRV-002/003/006/007) |

## Architecture

- React 18 + Vite + react-router (`BrowserRouter`), no Module Federation.
- Reuses the shared workspace packages: `design-system`, `api-client`, `auth`, `feature-flags`, `i18n`, `observability-web`, `chat-adapter`, `telemetry-adapter`, `shared-types`.
- Providers composed in `src/components/AppProviders.tsx` (Observability → I18n → Auth → ApiClient → FeatureFlags → Theme). No MFE registry or cross-MFE session context.
- API calls go through the gateway at `/api/*` (dev Vite proxy → `http://localhost:8080`). Endpoints are wrapped in `src/api/physicianApi.ts` and typed in `src/api/types.ts` (mirrors `shared/contracts/physician-service.yaml`).
- Access is guarded by `RequireAuth` + `RequireRole(['PHYSICIAN'])`.

## Run

```bash
# from telemedicine-frontend/
pnpm nx serve physician-app          # http://localhost:3015
VITE_AUTH_MODE=dev pnpm nx serve physician-app   # bypass Keycloak sign-in locally
pnpm nx build physician-app          # production build → dist/apps/physician-app
```

Container: `apps/physician-app/Dockerfile` (nginx) is wired into `deploy/docker-compose.dev.yml` on port `3015`.

## Env

| Var | Default | Purpose |
|---|---|---|
| `VITE_KEYCLOAK_URL` | `http://localhost:8180` | Keycloak base |
| `VITE_KEYCLOAK_REALM` | `telemedicine` | Realm |
| `VITE_KEYCLOAK_CLIENT_ID` | `physician-ui` | OIDC client |
| `VITE_AUTH_MODE` | — | `dev` to bypass sign-in |
| `VITE_API_BASE_URL` | `/api` | API base (proxied in dev) |
