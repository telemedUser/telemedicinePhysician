# Telemedicine Platform — Mock Datastore Plan

Plan for a **backend-only**, JSON-backed mock datastore that lets services run full business flows without PostgreSQL, while keeping a **seamless transition** to real databases later.

> **Scope:** Backend services only. Frontends always call APIs/BFFs — never read JSON files directly.

---

## Goals

| Goal | How |
|------|-----|
| Develop without Postgres | `mock` persistence profile loads JSON seed files |
| Same service code paths | Controllers → services → **repository ports** (unchanged) |
| Seamless DB cutover | Swap Spring profile + run Flyway; no controller changes |
| Realistic test data | JSON mirrors domain entities and cross-service references |
| Writable dev sessions | In-memory overlay on top of JSON seeds for create/update/delete |

---

## Non-goals

- Frontend mock servers or MSW fixtures as the source of truth
- Sharing one JSON file across services (each service owns its schema)
- Replacing `external-simulators` (those mock **vendor** systems; this mocks **our** datastore)

---

## Architecture

```
Controller
    ↓
Application Service  (business rules, events)
    ↓
Repository Port      (interface — domain layer)
    ↓
┌───────────────────┴────────────────────┐
│  @Profile("mock")   @Profile("postgres") │
│  Json*Repository    Jpa*Repository       │
└──────────────────────────────────────────┘
    ↓                       ↓
mock-data/*.json      PostgreSQL + Flyway
```

### Key rule

**No service reads JSON from a controller.** JSON is an implementation detail of the mock repository, hidden behind the same interface used by JPA.

---

## New shared module: `java-common-mock-data`

Add under `telemedicine-backend/shared/java-common-mock-data/`:

| Component | Responsibility |
|-----------|----------------|
| `JsonStore<T>` | Load JSON array from classpath or filesystem; index by id field |
| `JsonRepository<T>` | Base class implementing CRUD against `JsonStore` + in-memory overlay |
| `MockDataProperties` | `telemedicine.mock.data-dir`, `telemedicine.mock.writable=true` |
| `@EnableMockData` | Auto-config for `@Profile("mock")` services |

### Spring profiles

| Profile | Persistence | When |
|---------|-------------|------|
| `mock` | JSON + in-memory overlay | Local dev, CI unit tests, demos |
| `dev` | Optional: mock **or** Postgres via `telemedicine.persistence.mode` | Team choice |
| `postgres` / prod | JPA + Flyway | Staging, production |

### Configuration (per service)

```yaml
# application-mock.yml
telemedicine:
  persistence:
    mode: mock
  mock:
    data-dir: classpath:mock-data/waiting-room
    writable: true

spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

```yaml
# application-postgres.yml
telemedicine:
  persistence:
    mode: postgres

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:telemedicine_waiting_room}
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate
```

**Cutover:** change `spring.profiles.active` from `mock` to `postgres` (or set `telemedicine.persistence.mode=postgres`). No API or controller changes.

---

## JSON data layout

Central seed directory per service (committed to git):

```
telemedicine-backend/
└── mock-data/
    ├── README.md
    ├── _schemas/                    # optional JSON Schema for validation
    │   ├── appointment.schema.json
    │   └── consent-record.schema.json
    ├── waiting-room-service/
    │   ├── consents.json
    │   ├── session-links.json
    │   ├── queue-entries.json
    │   └── device-tests.json
    ├── identity-service/
    │   ├── identity-checks.json
    │   └── fonasa-challenges.json
    ├── integration-service/
    │   └── appointments.json
    ├── video-service/
    │   ├── sessions.json
    │   └── chat-messages.json
    ├── config-service/
    │   └── platform-config.json
    ├── audit-service/
    │   ├── audit-events.json
    │   └── consent-ledger.json
    └── ... (one folder per stateful service)
```

### Example: `mock-data/integration-service/appointments.json`

```json
[
  {
    "id": "apt-001",
    "patientId": "pat-001",
    "physicianId": "phy-001",
    "status": "SCHEDULED",
    "scheduledAt": "2026-08-12T10:00:00Z",
    "paymentStatus": "CONFIRMED",
    "source": "sim-his"
  },
  {
    "id": "apt-002",
    "patientId": "pat-002",
    "physicianId": "phy-001",
    "status": "OPEN_PATIENT",
    "scheduledAt": "2026-08-12T14:00:00Z",
    "paymentStatus": "PENDING",
    "source": "sim-his"
  }
]
```

### Cross-service ID conventions

Use stable IDs across files so BFF aggregation works:

| ID prefix | Owner service | Example |
|---------|---------------|---------|
| `apt-*` | integration-service | `apt-001` |
| `pat-*` | integration-service (MPI view) | `pat-001` |
| `consent-*` | waiting-room-service | `consent-001` |
| `sess-*` | video-service | `sess-001` |

Document canonical IDs in `datastore/README.md`.

---

## Repository port pattern (per service)

### 1. Define port (domain layer)

```java
public interface AppointmentRepository {
    Optional<Appointment> findById(String id);
    List<Appointment> findByStatus(AppointmentStatus status);
    Appointment save(Appointment appointment);
}
```

### 2. Mock implementation

```java
@Profile("mock")
@Repository
public class JsonAppointmentRepository extends JsonRepository<Appointment>
        implements AppointmentRepository {

    public JsonAppointmentRepository(ObjectMapper mapper, MockDataProperties props) {
        super(mapper, props.resolve("integration-service/appointments.json"), "id");
    }
}
```

### 3. Postgres implementation

```java
@Profile("postgres")
@Repository
public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, String>,
        AppointmentRepository {

    @Override
    default Optional<Appointment> findById(String id) {
        return findById(id).map(AppointmentEntity::toDomain);
    }
    // ... MapStruct or manual mapping entity ↔ domain
}
```

### 4. Service uses port only

```java
@Service
public class AppointmentService {
    private final AppointmentRepository repository;

    public Appointment consult(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Appointment not found"));
    }
}
```

---

## Writable mock behaviour

For dev sessions that create/update records:

1. **Load** seed JSON at startup into `JsonStore`
2. **Overlay** mutations in a `ConcurrentHashMap` (copy-on-write per entity)
3. **Optional persist** — write overlay to `target/mock-data-overlay/` on shutdown (gitignored)
4. **Reset** — `POST /actuator/mock/reset` (mock profile only) reloads seeds

This keeps JSON files clean in git while allowing interactive testing.

---

## BFF handling

| Layer | Mock data? |
|-------|------------|
| Domain services | Yes — via JsonRepository |
| BFFs | **No** — BFFs call domain services over HTTP; they stay stateless |
| Gateway | No |
| external-simulators | Separate concern (vendor APIs) |

Patient journey BFF aggregates live HTTP responses; when domain services use mock repos, the BFF automatically reflects JSON seed data.

---

## Implementation phases

### Phase A — Foundation (1–2 days)

| Task | Output |
|------|--------|
| Create `java-common-mock-data` module | `JsonStore`, `JsonRepository`, auto-config |
| Register in `settings.gradle` | Gradle module wired |
| Add `application-mock.yml` template to `_template-java` | New services get mock profile out of the box |
| Add `mock-data/README.md` + ID conventions | Documented seed contract |

### Phase B — Pilot service: `integration-service` (1 day)

| Task | Output |
|------|--------|
| `Appointment` domain model + `AppointmentRepository` port | Clean hexagonal boundary |
| `JsonAppointmentRepository` | Reads `appointments.json` |
| `JpaAppointmentRepository` + Flyway `V1__appointments.sql` | Postgres path |
| Controller uses `AppointmentService` only | No inline stub maps |

**Pilot exit criteria:** Same integration tests pass with `-Dspring.profiles.active=mock` and `-Dspring.profiles.active=postgres`.

### Phase C — Roll out to stateful services (3–5 days)

Priority order (matches [build-sequence.md](./build-sequence.md)):

1. `waiting-room-service` — consents, session-links, queue, device-tests
2. `identity-service` — identity checks, FONASA challenges
3. `config-service` — platform config
4. `audit-service` — audit events, consent ledger
5. `video-service` — sessions, chat messages
6. `clinical-docs-service`, `survey-service` — documents, responses

Stateless or pass-through services (`notification-service`, `reporting-service`, `monitoring-service`) can remain HTTP/event-only until KPI logic needs local state.

### Phase D — Seed pack & validation (1 day)

| Task | Output |
|------|--------|
| JSON Schema validation on startup (optional) | Fail fast on bad seed data |
| `tools/validate-mock-data.mjs` | CI check — schemas + referential integrity |
| Complete journey seed set | One `apt-demo` flows consent → video → docs → survey |

### Phase E — Postgres cutover (ongoing)

| Task | Output |
|------|--------|
| Flyway migrations match JSON entity shapes | Same field names and types |
| `tools/mock-data-to-sql.py` | One-time seed importer for staging |
| Remove `ddl-auto: update` | All services use `validate` + Flyway |
| CI matrix: `mock` + `postgres` profiles | Both paths tested |

---

## Seamless transition checklist

When moving a service from mock to Postgres:

- [ ] Repository port interface unchanged
- [ ] Flyway migration creates tables matching JSON entity fields
- [ ] JPA entity maps 1:1 to domain model (MapStruct recommended)
- [ ] Seed importer loads JSON into Postgres for staging/demo
- [ ] Switch profile: `spring.profiles.active=postgres`
- [ ] Run integration tests — same assertions, both profiles
- [ ] Remove any remaining inline stub responses from controllers

---

## Testing strategy

| Test type | Profile | Data source |
|-----------|---------|-------------|
| Unit tests | none / mock | In-memory `JsonStore` with test fixtures |
| Service integration tests | `mock` | `src/test/resources/mock-data/` overrides |
| Testcontainers integration | `postgres` | Flyway migrations + SQL fixtures |
| Journey e2e | `mock` or `postgres` | Full seed pack; BFF + services in compose |

---

## CI integration

```yaml
# Example GitHub Actions matrix entry
strategy:
  matrix:
    persistence: [mock, postgres]
steps:
  - name: Test waiting-room-service
    run: ./gradlew :waiting-room-service:test -Dspring.profiles.active=${{ matrix.persistence }}
```

---

## Relationship to existing components

| Component | Role | Overlap with mock datastore |
|-----------|------|----------------------------|
| `external-simulators` | Mocks **vendor** APIs (HIS, SAP, Vonage…) | None — keep separate |
| Controller stub maps | Quick scaffolding | **Replace** with repository-backed services |
| `ddl-auto: update` | Auto schema | **Replace** with Flyway when postgres profile enabled |
| Flyway migrations (existing) | Postgres schema | Must match JSON entity shapes |

---

## File & module checklist

| Item | Path |
|------|------|
| Shared mock library | `telemedicine-backend/shared/java-common-mock-data/` |
| Seed data root | `telemedicine/datastore/seeds/` |
| Deletion guide | `telemedicine/datastore/DELETION.md` |
| Seed validator | `telemedicine-backend/tools/validate-mock-data.mjs` |
| SQL seed importer | `telemedicine-backend/tools/mock-data-to-sql.py` |
| This plan | `telemedicine-platform/docs/mock-datastore-plan.md` |
| Build order | `telemedicine-platform/docs/build-sequence.md` |

---

## Recommended next step

Implement **Phase A + Phase B** (shared library + `integration-service` pilot). That establishes the pattern every other service copies — one repository port, two implementations, one JSON seed file per aggregate.
