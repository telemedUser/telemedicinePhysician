# Telemedicine Platform — Technical Design Document (TDD)

## Architecture summary

Three experience shells (Patient, Physician PiP, Administrator) consume three BFFs that aggregate domain microservices behind a Spring Cloud Gateway. All domain services are Java 21 / Spring Boot 3 with schema-per-service PostgreSQL, Kafka event bus, and Consul discovery.

## Layered architecture

1. **Experience** — 14 Module Federation MFE remotes across 3 shells
2. **BFF** — patient-bff, physician-bff, admin-bff (journey aggregation, no domain logic)
3. **Edge** — gateway (JWT, RBAC, rate limit, circuit breakers, CORS)
4. **Domain** — 10 core services + integration ACL + external simulators
5. **Platform** — config, scheduler, audit (hash-chained append-only store, consent ledger, MinIO exports)
6. **Data** — PostgreSQL, Redis, Kafka, MinIO, Keycloak

## Shared libraries

Nine Gradle modules under `telemedicine-backend/shared/` provide cross-cutting concerns: error model, security, events/outbox, observability, persistence, integration resilience, audit, and testing utilities.

## Domain services (Part 3)

Ten core domain services under `telemedicine-backend/services/` — each owns a Postgres schema, publishes catalogue events via transactional outbox, and exposes OpenAPI in `shared/contracts/`:

| Service | Port | Responsibility |
|---------|------|----------------|
| integration-service | 8107 | ACL for HIS, SAP, payment, FONASA, MPI, Zendesk |
| waiting-room-service | 8101 | Consent, session-link, device-test, queue, status |
| identity-service | 8102 | Identity validation, FONASA 2FA (via integration) |
| notification-service | 8106 | Templates, multi-channel delivery |
| video-service | 8103 | Video/chat sessions, provider failover |
| telemetry-service | 8104 | ANURA (independent deployable) |
| clinical-docs-service | 8105 | Documents, prescriptions, medical leave |
| survey-service | 8108 | Post-consultation NPS |
| reporting-service | 8109 | KPI aggregation (technical/functional/business) |
| monitoring-service | 8110 | Live snapshot, alerts, connection quality |

Compose profile `domain` starts all ten. Verify with `scripts/verify-part3.ps1`.

## External integration pattern

All vendor systems (HIS, SAP, FONASA, Vonage, ACS, ANURA, payment, RecMed, Zendesk) are accessed through adapter interfaces in domain services, with `external-simulators` providing deterministic dev/test responses.

### Simulator architecture (Part 4)

- Single Spring Boot app on :8200, compose profile `sim` only
- `SimulatorEngine` — per-vendor latency, error-rate, timeout-rate injection
- Admin API at `/sim/admin/**` for runtime tuning and chaos/resilience presets
- Stateful in-memory stores for appointment lifecycle, FONASA 2FA, video sessions, notification log
- `X-Sim-Scenario` header drives vendor-specific failure modes; WireMock parity via `SimulatorStubs`
- OpenAPI: `/sim/v3/api-docs`; contract in `shared/contracts/external-simulators.yaml`

## Security model

- OAuth2 Authorization Code + PKCE via Keycloak
- JWT validation at gateway (disabled in `dev` profile)
- RBAC roles: PATIENT, PHYSICIAN, ADMINISTRATOR
- PHI redaction in logs; AES-256 column encryption for sensitive fields
- Audit trail with hash chaining in audit-service

## Event-driven integration

Transactional outbox pattern publishes versioned `EventEnvelope` records to Kafka. See `shared/contracts/events/event-catalogue.yaml`.

## Deployment

### Local development

- Docker Compose with profiles: `infra`, `observability`, `sim`, `edge`, `platform`, `domain`, `apps`
- Keycloak dev realm: `infra/keycloak/realm-telemedicine.json`
- JWT validation disabled in gateway `dev` profile

### Production Kubernetes

Production manifests live under `telemedicine-platform/kubernetes/`:

| Component | Manifest | Notes |
|-----------|----------|-------|
| Namespace | `namespaces/telemedicine.yaml` | Isolated namespace |
| Gateway HA | `ha/gateway-ha.yaml` | HPA 3–10 replicas, PDB minAvailable 2 |
| Ingress | `ingress/ingress.yaml` | TLS termination, blue/green traffic split |
| Network policies | `security/network-policies.yaml` | Gateway → BFF → domain → infra segmentation |
| DR | `dr/postgres-failover.yaml` | Standby promotion procedure |
| Backend/frontend bases | `telemedicine-backend/deploy/kubernetes/base`, `telemedicine-frontend/deploy/kubernetes/base` | Per-service Deployments |

Apply with Kustomize:

```bash
kubectl apply -k telemedicine-platform/kubernetes/
```

Production Keycloak realm: `infra/keycloak/realm-telemedicine-prod.json` (MFA, password policy, session timeouts, brute-force protection).

## CI/CD

Pipeline templates under `telemedicine-platform/ci/templates/`:

| Template | Purpose |
|----------|---------|
| `backend-service.yml` | Gradle build, test (≥80% coverage gate), container image, deploy |
| `frontend-mfe.yml` | pnpm install, lint, test, build, per-MFE artifact publish |

Deployment strategy:

1. **Blue/Green** — ingress annotation switches `color=blue|green`; previous color kept warm 1 hour (RB-006).
2. **Canary** — 5% traffic to new color for 5 minutes; auto-rollback on SLO burn-rate alert.
3. **Verify gates** — `verify-part7.ps1` must pass before prod promotion.

## Security (production)

See [security-review.md](./security-review.md) for the full checklist.

- OAuth2 + PKCE via Keycloak; JWT validation at gateway (`TELEMEDICINE_SECURITY_MODE=PROD`)
- RBAC: PATIENT, PHYSICIAN, ADMIN roles enforced on gateway route map
- MFA required for administrators (TOTP in prod realm)
- Network policies restrict pod-to-pod traffic (gateway-only ingress to BFFs)
- Kafka TLS: `infra/kafka/application-prod-kafka.yml` — `spring.kafka.security.protocol=SSL`
- Rate limiting via Redis at gateway; circuit breakers per upstream route
- PHI redaction in logs; AES-256 column encryption; audit hash chain

## HA/DR

| Target | Design | Verification |
|--------|--------|--------------|
| RPO | < 15 minutes | PostgreSQL continuous replication + WAL shipping |
| RTO | < 2 hours | DR runbook RB-004; secondary region ingress failover |
| Gateway | 3+ replicas, HPA, PDB | `gateway-ha.yaml` |
| Kafka | 3 brokers, TLS, replication factor 3 | Broker health + consumer lag alerts |
| PostgreSQL | Primary + hot standby | `postgres-failover.yaml` promotion |
| Redis | Cluster mode (6 nodes) | Cache/rate-limit resilience |
| Object storage | MinIO bucket replication | Clinical document durability |

DR exercise: promote standby → update DNS → smoke test patient journey BFF → document timeline.

## Performance targets

- p95 API latency < 300ms (validated by k6 scripts in `tests/k6/`)
- 99.9% availability for patient journey endpoints (SLO burn-rate alerts in Prometheus)
- 80%+ automated test coverage enforced in CI

### k6 load test suite

| Script | Scope | Threshold |
|--------|-------|-----------|
| `patient-journey.js` | End-to-end journey endpoints | p95 < 300ms |
| `gateway-load.js` | Gateway health, OpenAPI, monitoring | p95 < 300ms |
| `bff-load.js` | Patient, physician, admin BFFs | p95 < 300ms |
| `domain-services.js` | All ten domain service read paths | p95 < 300ms |
| `event-throughput.js` | Appointment → link → video event path | p95 < 300ms |

Run: `k6 run tests/k6/patient-journey.js` (requires stack up).

## OpenAPI

Service contracts live in `telemedicine-backend/shared/contracts/`. Gateway aggregates OpenAPI docs at `/v3/api-docs` (production profile).
