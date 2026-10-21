# Running State Snapshot — Physician Module

Captured at the end of the session that implemented per-physician login. These are background dev
processes; if the machine/terminals restarted they are gone and must be re-launched with the
commands in AGENT-HANDOFF.md §3. Nothing here auto-starts.

## Expected services when fully up

| Service | Port | Start from | Notes |
|---------|------|-----------|-------|
| Docker infra (postgres/redis/consul/kafka) | 5432 / 6379 / 8500 / 29092 | Docker Desktop | Shared; kafka external listener on **29092** |
| physician-service (INTEGRATION) | 8114 | `telemedicine_physician_integration\telemedicine-backend` gradle `:physician-service:bootRun` | DB `physician`. Scopes worklist by X-Physician-Id. |
| physician-app (INTEGRATION) | 3015 | `...integration\telemedicine-frontend` → `corepack pnpm exec nx serve physician-app` | Login page first; proxies /api,/ws → :8114, /booking-api → :8120 |
| patient-booking-service (INTEGRATION) | 8120 | `telemedicinePatient_integration\telemedicine-backend` gradle bootRun | Optional here; needed for physician "View details" (/booking-api) and for bookings to flow onto the worklist |
| patient-booking app (INTEGRATION) | 3001 | `...integration\telemedicine-frontend\apps\patient-booking` → `corepack pnpm dev` | Optional |

## Do-not-run-together

- `telemedicine_physician` vs `telemedicine_physician_integration` — both :8114 / :3015 / DB `physician`. Run only the integration one.
- `telemedicinePatient` vs `telemedicinePatient_integration` — both :8120 / :3001 / DB `patient_booking`.

## Demo physician accounts (password `doctor123`)

| Email | physicianId | Worklist items |
|-------|-------------|----------------|
| arjun.khanna@connectedcare.in | doc-khanna | 7 |
| meera.nair@connectedcare.in | doc-nair | 1 |
| vikram.reddy@connectedcare.in | doc-reddy | 1 |
| lakshmi.iyer@connectedcare.in | doc-iyer | 1 |

## Smoke test after restart

```
curl http://localhost:8114/actuator/health
curl -X POST http://localhost:8114/api/auth/physician-sign-in -H "Content-Type: application/json" -d "{\"email\":\"arjun.khanna@connectedcare.in\",\"password\":\"doctor123\"}"
curl -H "X-Market: IN" -H "X-Physician-Id: doc-khanna" http://localhost:8114/api/v1/physicians/me/worklist
open http://localhost:3015
```
