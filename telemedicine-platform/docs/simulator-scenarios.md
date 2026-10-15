# Part 4 — External simulators scenario catalogue

Dev/test only. The `external-simulators` app runs on port **8200** under the compose `sim` profile.

## Admin API

| Endpoint | Purpose |
|----------|---------|
| `GET /sim/admin/vendors` | List all 12 vendor configs |
| `PUT /sim/admin/vendors/{vendor}` | Set latency, jitter, error/timeout rates, enabled |
| `POST /sim/admin/scenarios/happy` | Zero latency/errors |
| `POST /sim/admin/scenarios/chaos` | Random 100–500ms latency + 10% errors |
| `POST /sim/admin/scenarios/resilience` | Vonage 100% error, payment 20% error, HIS 200ms latency |
| `GET /sim/admin/state` | Dump appointment/FONASA/video/notification state |
| `DELETE /sim/admin/state` | Clear stateful stores |

## Scenario header

Send `X-Sim-Scenario` on any vendor call.

| Value | Behaviour |
|-------|-----------|
| `success` | Happy path (default) |
| `failure` / `error` | HTTP 503 |
| `timeout` | Simulated vendor timeout |
| `partial` | HTTP 200 with incomplete payload |
| `not-found` | HIS/MPI 404 |
| `unpaid` / `payment-pending` | Payment not confirmed |
| `locked-out` | FONASA immediate lockout |
| `session-expired` | Video session 410 |
| `capacity-exceeded` | Video 429 |

## Stateful flows

1. **Appointment** — first HIS GET creates state; payment validate marks paid; SAP verify checks existence.
2. **FONASA** — challenge stores code `123456`; 3 wrong attempts → lockout.
3. **Video** — session stored per provider; token 404 if session missing; `/outage` toggles provider failure.

## k6 load script

Run `telemedicine-platform/scripts/k6/simulator-load.js` after compose `infra,observability,sim,edge` is up.

## WireMock parity

Use `SimulatorStubs.registerAllWithScenarios(server)` in service tests. Contract field sets are validated by `SimulatorStubsContractSyncTest`.
