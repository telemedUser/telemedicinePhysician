# Physician Service — Technical Design

> Blupa New Telemedicine Platform · PRD-TM-001 v0.2
> Bounded context: **Provider & Physician Management** (+ Physician Worklist projection and Consultation Closure orchestration)
> Scope note: **Video conferencing is explicitly out of scope.** This service never creates, tokenises or terminates media sessions. The `video-service` owns that. Where a physician "joins", this service only records the state transition and the consultation-closure lifecycle; media is referenced, never managed.

---

## 1. Purpose & Scope

### 1.1 Why this service exists

The monorepo already has a `physician-bff` (channel aggregation) but **no domain service that owns the physician/provider records, availability, worklist projection and consultation-closure lifecycle**. Today the BFF fabricates worklist entries from a monitoring snapshot (`PhysicianWorklistService` loops over `waitingRoomQueue`). That is a placeholder. This service becomes the authoritative owner of the data the BFF should aggregate.

The service realises these PRD sections:

| PRD section | Requirement IDs | What this service owns |
|---|---|---|
| §21 Provider & Physician Management | TM-PRV-001..008 | Provider identity projection, specialties, modes, routing attributes, availability/capacity, status, credentials, productivity metrics |
| §12.3 Physician Worklist | TM-WLT-005..009 | Worklist read model: who is ready, waiting time, readiness, flags, red-flag acknowledgement |
| §12.1 Physician surface auth | TM-WLT-004 | HIS SSO assertion acceptance / standalone OIDC (delegated to shared security) |
| §14.2 Physician closure | TM-DOC-008..009 | Closure orchestration: minimum completion set, emit `ConsultationClosed` |
| §13.2 Patient context | TM-CON-003 | Assembles physician patient-context read model (demographics, history, prior virtual consults, flags) |
| §9 directory source | TM-DIR-001, TM-RTE-001b | Publishes provider availability/capacity used by Provider & Routing |

### 1.2 Explicitly out of scope

- **Video / media sessions** (`video-service`) — tokens, join, failover, recording.
- **Clinical document generation** (`clinical-docs-service`, ReceMed, Medipass) — this service *triggers closure* and references documents but does not author or transmit them.
- **Being the system of record for provider master data.** Per TM-DIR-001 and TM-INT-001, authoritative provider data lives in the HIS and is reached only through `integration-service`. This service holds a **projection** with source reference + staleness policy (TM-ARC-020).
- **Routing/scoring** (Provider & Routing context) — this service *supplies* availability and capacity; it does not score candidates.

### 1.3 Source-of-truth posture (TM-ARC-019)

| Data | Classification | Owner |
|---|---|---|
| Provider identity, credentials, specialties | **Cached projection** of HIS | HIS (via integration-service) |
| Availability & capacity rules, per-mode limits | **System of record** (platform-native) | this service |
| Provider operational status (active/suspended/on-leave) | **System of record** | this service |
| Worklist read model | **Derived projection** from waiting-room / appointment events | this service |
| Consultation closure record | **System of record** for the closure fact; documents remain in their SoR | this service |
| Productivity metrics | **Derived analytics** from the event stream | this service |

---

## 2. Position in the Architecture

```
                       ┌──────────────────────────┐
  Physician (in HIS)   │        API Gateway        │  JWT validation, rate limit,
  → physician-bff ────▶│  lb://physician-service   │  circuit breaker (TM-ARC-001)
                       └────────────┬─────────────┘
                                    │ /api/v1/physicians/**
                                    ▼
          ┌──────────────────────────────────────────────┐
          │              physician-service                │
          │  REST API · domain · projections · outbox     │
          └───┬───────────┬──────────────┬────────────────┘
     sync REST│           │ consume Kafka│ publish Kafka (outbox)
              ▼           ▼              ▼
   integration-service  (StatusChanged,  (ConsultationClosed,
   (HIS provider,        PatientEntered-  ProviderStatusChanged*,
    appointment,         WaitingRoom,     ProviderUnavailable*)
    MPI context)         DeviceTest-
                         Completed,
                         ClinicalDocumentIssued,
                         Prescription/MedicalLeaveIssued,
                         ConnectionQualityDegraded)
```

`*` new event types this service adds to the shared catalogue (see §8).

Design rules honoured (§27):
- **Stateless** (TM-ARC-007): all state in Postgres + Kafka; no session affinity.
- **No shared DB** (TM-NFR-019): dedicated `physician` database.
- **Adapter boundary** (TM-INT-001): HIS reached only via `integration-service`; no vendor SDK imported.
- **Market-neutral core** (TM-MKT-001/008): every row carries `market`; no market-conditional branch — behaviour comes from `config-service`.
- **Idempotency** (TM-ARC-010): idempotency key on every state-changing endpoint; `@IdempotentConsumer` on every listener.

---

## 3. Domain Model

### 3.1 Aggregates

**Provider (projection + platform-owned operational overlay)**
- `providerId` (HIS id, projection key), `market`, `displayName`, `specialties[]`, `subSpecialties[]`, `languages[]`, `modes[]` (video/audio/chat), `networkRefs[]`, `routingAttributes` (years-experience, rating, education), `status` (ACTIVE / SUSPENDED / ON_LEAVE / OFFBOARDED).
- `sourceRef`, `lastRefreshedAt`, `stalenessPolicy` (TM-ARC-020).
- Credentials: `[{ type, number, validFrom, validUntil, verified }]` — used by TM-PRV-006 (block routing on expired credential). The service **flags** expiry; it is not a credential verification SoR (out of scope per PRD §5).

**Availability (system of record)**
- `providerId`, `market`, recurring availability windows, per-mode concurrency limits (`maxConcurrent`), capacity counters.
- Status changes take **immediate** effect on the published availability snapshot (TM-PRV-003).

**WorklistItem (derived read model)**
- `appointmentId`, `providerId`, `patientRef` (pseudonymous), `mode`, `enteredAt`, `waitingSeconds` (computed), `readiness` (NOT_READY / IN_PROGRESS / READY), `consultationStatus` (CHECKED_IN / OPEN_DOCTOR / STARTED / CLOSED), `technicalStatus` (OK / DEGRADED / IMPAIRED), `telemetryAvailable`, `flags` (FIRST_VISIT, FOLLOW_UP, GUEST, RED_FLAG), `redFlagAcknowledgedAt`, `redFlagAcknowledgedBy`.
- Rebuilt from events; never authored by API writes except the red-flag acknowledgement and force-transition.

**ConsultationClosure (system of record for the closure fact)**
- `appointmentId`, `providerId`, `startedAt`, `closedAt`, `completionSet` (note/diagnosis/prescription/certificate/referral/follow-up booleans), `minimumSatisfied`, `exceptionReason`, `documentRefs[]`.
- Emits `ConsultationClosed` only when the configurable minimum completion set is satisfied **or** an audited exception is recorded (TM-DOC-009).

### 3.2 Clinical concepts as FHIR (TM-ARC-023)

- Provider → **FHIR Practitioner** / **PractitionerRole** at the read-API boundary.
- Patient context assembled for the physician → references **Patient**, **Encounter**, **Condition**, **Observation**, **DocumentReference** (read-only, sourced via integration-service; this service does not persist clinical bodies — TM-SEC-019 data minimisation).
- Operational concepts (worklist item, readiness, queue position) stay **platform-native** (TM-ARC-024), not forced into FHIR.

---

## 4. Persistence

Dedicated Postgres database `physician`. Flyway migrations under `db/migration` (+ shared `db/migration/common`), `ddl-auto: validate` — matching the notification-service convention.

```
V1__physician_schema.sql
  provider(provider_id PK, market, display_name, status, routing_attributes JSONB,
           source_ref, last_refreshed_at, staleness_seconds, created_at, updated_at)
  provider_specialty(provider_id FK, specialty, sub_specialty)
  provider_mode(provider_id FK, mode)                     -- video|audio|chat
  provider_language(provider_id FK, language)
  provider_credential(id PK, provider_id FK, type, number, valid_from, valid_until, verified)
  availability(id PK, provider_id FK, market, day_of_week, start_time, end_time,
               mode, max_concurrent)
  worklist_item(appointment_id PK, provider_id, market, patient_ref, mode, entered_at,
                readiness, consultation_status, technical_status, telemetry_available,
                flags JSONB, red_flag_acknowledged_at, red_flag_acknowledged_by, updated_at)
  consultation_closure(appointment_id PK, provider_id, market, started_at, closed_at,
                       completion_set JSONB, minimum_satisfied, exception_reason,
                       document_refs JSONB, created_at, updated_at)
```

- Every table carries `market` (TM-MKT-002); all repository queries are market-scoped.
- No clinical bodies stored — only references and pseudonymous actor/patient refs (TM-SEC-014/019).
- Outbox table provided by `java-common-events` for transactional publish.

---

## 5. API Surface (OpenAPI 3.1 — TM-ARC-003)

Base path `/api/v1/physicians`. Gateway route `lb://physician-service` on `Path=/api/v1/physicians/**` with a `physicianCircuitBreaker`. Contract published to `shared/contracts/physician-service.yaml` and validated in CI (TM-ARC-018).

### 5.1 Provider management (TM-PRV-001..008) — `ADMIN` / `PROVIDER_ADMIN`
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/physicians/{providerId}` | Provider projection (FHIR Practitioner view available via `?view=fhir`) |
| GET | `/api/v1/physicians` | Search/filter by specialty, mode, language, status, market |
| PUT | `/api/v1/physicians/{providerId}/status` | Set ACTIVE/SUSPENDED/ON_LEAVE — immediate routing/directory effect (TM-PRV-003) |
| GET | `/api/v1/physicians/{providerId}/availability` | Availability & capacity |
| PUT | `/api/v1/physicians/{providerId}/availability` | Manage availability, per-mode limits (TM-PRV-002) |
| GET | `/api/v1/physicians/{providerId}/metrics` | Utilisation, duration, documentation time, no-show/failure rate (TM-PRV-007) |

### 5.2 Worklist (TM-WLT-005..009) — `PHYSICIAN`
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/physicians/me/worklist` | Physician's worklist (identity from JWT), sortable/filterable |
| GET | `/api/v1/physicians/me/worklist/{appointmentId}/context` | Patient context read model (TM-CON-003) |
| POST | `/api/v1/physicians/me/worklist/{appointmentId}/red-flag/ack` | Record red-flag acknowledgement (TM-WLT-009) |

### 5.3 Consultation closure (TM-DOC-008/009) — `PHYSICIAN`
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/physicians/me/consultations/{appointmentId}/close` | Submit completion set; emits `ConsultationClosed` when minimum satisfied or audited exception |

### 5.4 Internal (service-to-service) — `@IsService`
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/internal/physicians/{providerId}/availability-snapshot` | For Provider & Routing (TM-RTE-001b) — includes credential-validity gate (TM-PRV-006) |
| POST | `/api/v1/internal/jobs/provider-projection/refresh` | Scheduled reconciliation with HIS (TM-INT-009) |

All state-changing endpoints accept an `Idempotency-Key` header (TM-ARC-010). Responses use the shared `ApiResponse<T>` envelope.

---

## 6. Eventing (TM-ARC-005, TM-INT-012)

### 6.1 Consumed (idempotent, out-of-order tolerant — TM-INT-013)

| Event | Source | Effect on this service |
|---|---|---|
| `AppointmentCreated` | integration-service | Create worklist item, resolve provider, set FIRST_VISIT/FOLLOW_UP flag |
| `PatientEnteredWaitingRoom` | waiting-room-service | Set `enteredAt`, readiness IN_PROGRESS |
| `DeviceTestCompleted` | waiting-room-service | Update readiness → READY / technical status |
| `StatusChanged` | waiting-room-service | Map CHECKED_IN/OPEN_DOCTOR → consultationStatus; publish to worklist within 3 s (TM-WRM-001c, TM-WLT-006) |
| `ConnectionQualityDegraded` | video-service | Set technicalStatus DEGRADED/IMPAIRED (read-only ref to media; no media control) |
| `ClinicalDocumentIssued` / `PrescriptionIssued` / `MedicalLeaveIssued` | clinical-docs-service | Flip completion-set booleans + collect `documentRefs` for closure |
| `ConsentWithdrawn` | waiting-room-service | On mid-consultation withdrawal, surface to worklist for controlled closure (TM-CNS-001c) |

### 6.2 Published (via transactional outbox)

| Event | When | Consumers |
|---|---|---|
| `ConsultationClosed` *(exists)* | Minimum completion set satisfied or audited exception (TM-DOC-009) | survey, reporting, claims, notification |
| `ProviderStatusChanged` *(new)* | Provider status set active/suspended/on-leave (TM-PRV-003) | provider-routing, directory |
| `ProviderUnavailable` *(new)* | Physician unavailability handling (TM-PRV-008) | waiting-room, notification |

New event types must be added to `EventTypes.java`, `shared/contracts/events/event-catalogue.yaml` and `telemedicine-platform/infra/kafka/topics.txt` **together** (the contract test fails the build otherwise — see `EventTypes` javadoc).

Wire naming `tm.<context>.<entity>.<action>` (TM-ANA-008), e.g. `tm.provider.status.changed`, `tm.consultation.session.closed`.

---

## 7. Cross-Cutting Concerns

- **Security (§23):** JWT via gateway; `@RequiresRole`/`@IsService` from `java-common-security`. Physician identity from HIS SSO assertion (TM-WLT-004) — role `PHYSICIAN`; standalone workspace uses OIDC+MFA. RBAC least-privilege; worklist scoped to the authenticated physician.
- **Audit (§23, TM-SEC-025):** status changes, red-flag acknowledgement, force-transition, closure exceptions written to the immutable audit store via `java-common-audit` with actor/role/target/before-after.
- **Observability (§28):** `java-common-observability` → OpenTelemetry traces, Prometheus `/actuator/prometheus`, structured logs with correlation ID; **no clinical data in logs** (TM-SEC-014). Latency budget: worklist propagation P95 ≤ 3 s (TM-NFR-003).
- **Degraded mode (TM-INT-008):** if integration-service (HIS) is unavailable, serve the provider projection with a staleness marker (TM-IDN-006c pattern) rather than blocking the worklist.
- **Resilience (TM-INT-004):** timeouts, retry with backoff+jitter, circuit breaker, bulkhead on the integration-service client.
- **Config/flags (TM-ARC-011/013):** minimum-completion-set, staleness tolerance, worklist flags read from `config-service`; `ConfigChangeListener` refreshes within 60 s (TM-NFR-010).

---

## 8. Module & Package Layout

```
services/physician-service/
├── build.gradle                 # same java-common deps as notification-service
├── Dockerfile
├── DESIGN.md                    # this document
└── src/
    ├── main/java/com/telemedicine/physicianservice/
    │   ├── PhysicianServiceApplication.java
    │   ├── api/
    │   │   ├── ProviderController.java
    │   │   ├── WorklistController.java
    │   │   ├── ConsultationClosureController.java
    │   │   ├── PhysicianInternalController.java
    │   │   └── dto/ (requests/responses)
    │   ├── domain/
    │   │   ├── provider/      (ProviderEntity, Availability, Credential, repos)
    │   │   ├── worklist/      (WorklistItemEntity, repo)
    │   │   └── closure/       (ConsultationClosureEntity, repo)
    │   ├── service/
    │   │   ├── ProviderService.java
    │   │   ├── WorklistService.java
    │   │   ├── PatientContextService.java
    │   │   └── ConsultationClosureService.java
    │   ├── client/
    │   │   └── IntegrationProviderClient.java   # HIS provider/appointment/MPI via integration-service
    │   ├── kafka/
    │   │   ├── WorklistProjectionListener.java   # appointment/waiting-room/quality events
    │   │   ├── DocumentCompletionListener.java    # document issuance events → completion set
    │   │   └── ConfigChangeListener.java
    │   ├── config/
    │   │   ├── PhysicianServiceConfiguration.java
    │   │   └── PhysicianServiceProperties.java
    │   └── fhir/ FhirPractitionerMapper.java      # projection → FHIR Practitioner view
    ├── main/resources/
    │   ├── application.yml        # port 8114
    │   └── db/migration/V1__physician_schema.sql
    └── test/java/...              # service, listener, contract tests (>80% — TM-NFR-008)
```

Package root `com.telemedicine.physicianservice`, mirroring `com.telemedicine.notificationservice`.

---

## 9. Wiring Checklist (to make it deployable)

1. `settings.gradle` → `include 'physician-service'` + `project(':physician-service').projectDir = file('services/physician-service')`.
2. `gateway/.../application.yml` → add route `physician-service` on `/api/v1/physicians/**` with `physicianCircuitBreaker`.
3. `shared/contracts/physician-service.yaml` → OpenAPI 3.1 contract.
4. `EventTypes.java` + `event-catalogue.yaml` + `topics.txt` → add `ProviderStatusChanged`, `ProviderUnavailable` (together).
5. `physician-bff` → point `PhysicianWorklistService` at `lb://physician-service` instead of the monitoring placeholder.
6. Postgres `physician` DB in deploy compose/k8s; service env for `CONSUL_HOST`, `DB_*`, `KAFKA_BOOTSTRAP`.

---

## 10. Requirement Traceability (summary)

| Requirement | Realised by |
|---|---|
| TM-PRV-001..008 | §3 Provider/Availability/Credential aggregates, §5.1 provider API, §6.2 Provider events |
| TM-WLT-004..009 | §5.2 worklist API, §6.1 projection listeners, §7 security |
| TM-CON-003 | §3 PatientContext read model, §5.2 context endpoint |
| TM-DOC-008/009 | §3 ConsultationClosure, §5.3 closure API, `ConsultationClosed` emission |
| TM-ARC-003/004/005/007/010/019/020/023/024 | §2, §4, §6 |
| TM-INT-001/004/008/009/012/013 | §2, §6, §7 |
| TM-MKT-001/002/008 | market column everywhere, config-driven behaviour |
| TM-NFR-003/008/019 | §7 latency budget, test target, dedicated DB |

---

## 11. Implemented in this pass (backend for the physician frontend)

Stack: **Java 21 · Spring Boot · Spring Data JPA · PostgreSQL · Gradle · Flyway**, RESTful, matching the monorepo conventions (shared `java-common-*` libs, `ApiResponse<T>` envelope, `BaseEntity` auditing, domain exceptions → RFC 9457 via the shared `GlobalExceptionHandler`).

### Provider & Physician Management (§21) — REST
- `GET /api/v1/physicians?status=&specialty=&mode=&language=` — directory search (market-scoped via `X-Market`).
- `GET /api/v1/physicians/{providerId}` — full detail: specialties, languages, modes, credentials (with expiry flag, TM-PRV-006), availability windows (TM-PRV-002), productivity metrics (TM-PRV-007), projection freshness (`stale`, TM-ARC-020).
- `PUT /api/v1/physicians/{providerId}/status` — change operational status, immediate effect (TM-PRV-003).
- Tables: `provider`, `provider_specialty`, `provider_mode`, `provider_language`, `provider_credential`, `availability`, `provider_metrics` (`V2__provider_schema.sql`), seeded with the demo directory (`V3__seed_providers.sql`). These responses match the physician app's `ProviderSummary` / `ProviderDetail` types exactly, so the app's `searchProviders` / `getProvider` / `setProviderStatus` light up with no frontend change.

### Clinical chat (§13.3) — real-time, open-source
- Transport: **Spring WebSocket + STOMP** (`spring-boot-starter-websocket`, Apache 2.0) — self-hosted, no proprietary vendor SDK.
  - WebSocket endpoint `/ws/chat` (SockJS fallback). Subscribe `/topic/consultation/{appointmentId}`; publish `/app/consultation/{appointmentId}/send`.
- Durable REST surface + history: `GET` / `POST /api/v1/physicians/me/consultations/{appointmentId}/chat`.
- Transcript persisted to Postgres (`chat_message`, `V4__chat_schema.sql`), retained as clinical data. A **PHI-free** `ChatMessageSent` marker is emitted (no body) via the existing event contract.
- Gateway: `/ws/chat/**` routed to `lb://physician-service`.

### Waiting room (admit / status) — reused, open-source for now
- The physician "admit" action (`POST /api/v1/waiting-room/status/{id}` → `OPEN_DOCTOR`) is already implemented in **waiting-room-service** with REST + Postgres + a transactional outbox; the physician app calls it directly. The worklist read model here is maintained from `StatusChanged`/waiting-room events (`WorklistProjectionListener`). Kafka is the propagation mechanism; no third-party waiting-room library was needed. (The user's "use Kafka later" note is already satisfied by the outbox→Kafka path that exists; the worklist projection consumes it.)

### Not yet wired (future passes)
- `ProviderStatusChanged` / `ProviderUnavailable` domain events (need registration in `EventTypes` + `event-catalogue.yaml` + `topics.txt` together).
- Provider projection refresh job from the HIS via integration-service.
- Documentation "send" with per-system confirmation and AI draft are composed in the physician BFF / clinical-docs-service, not here.

### Build / run
From `telemedicine-backend`:
```bash
./gradlew :physician-service:build        # compile + test
./gradlew :physician-service:bootRun       # run (needs Postgres `physician` DB, Consul, Kafka)
# or the whole stack:
make up
```
Port 8114; registered in Consul; routed at the gateway under `/api/v1/physicians/**` and `/ws/chat/**`.
