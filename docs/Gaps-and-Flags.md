# Telemedicine Physician — Current State, Gaps & Flags

Brief, current-state assessment grounded in the repo code/configs and the
verified `Features.md` / `Skills.md`. Items are facts observed in the repo, not
speculation.

## 1. Current state (snapshot)

| Area | State |
|------|-------|
| Physician shell + 3 MFEs | Present (consultation 3011, documentation 3012, patient-context 3013), MF-wired |
| Standalone physician-app | Present (:3015), single React app, video explicitly out of scope |
| physician-service | Present (:8114) — worklist, chat (REST+STOMP), providers, closure |
| physician-bff | Present (:8091) — aggregates, can return DEGRADED |
| Shared domain services | video/telemetry/clinical-docs/integration/notification/monitoring etc. |
| Event backbone | Kafka, 28 domain events, each + `.RETRY` + `.DLQ`, idempotent consumers |
| Gateway | Spring Cloud Gateway; physician routes `/bff/physician/**`, `/api/v1/physicians/**`, `/ws/chat/**` |
| Discovery / auth | Consul + Keycloak (client `physician-ui`), dev bypass `DevPhysicianAuthFilter` |
| Observability / orchestration | Prometheus + Jaeger; Docker Compose + Kubernetes + Kafka HA |

## 2. Repo-wise gaps & flags

### Flags (things to be aware of)
- **physician-app bypasses the gateway.** The standalone app (:3015) proxies `/api` and
  `/ws` **directly to physician-service (:8114)**, not through the gateway. Running it
  locally needs `physician-service` up via Gradle (`:physician-service:bootRun`).
- **physician-service is NOT in docker-compose.** It is a real Gradle module with full
  controllers, but the compose `domain` profile stops at `monitoring-service` (:8110). To
  run the physician worklist/chat domain locally you must start it outside the standard
  compose profiles. **This is the single biggest local-run gap.**
- **Two physician frontends.** The Module Federation portal (shell + 3 MFEs) and the
  standalone `physician-app` overlap in purpose. Clarify which is the production target;
  physician-app defines extra routes (review/send/close/withdrawn/previous) beyond the
  six documented portal screens.
- **Video out of scope in physician-app.** Media sessions are owned elsewhere (HIS/video
  vendor); the standalone app deliberately does not render video.
- **Node 20 required.** Node 24 breaks Nx (WebAssembly fallback). Strict ports; MFE portal
  `/api` proxy needs the gateway up. (See Skills.md gotchas.)

### Gaps (verify / likely incomplete)
- **Mock-heavy external layer.** External vendor calls go through `integration-service` to
  simulators; no real vendor credentials wired in-repo (expected for a sim-first build).
- **Compose coverage.** Because physician-service isn't in compose, an out-of-the-box
  `docker compose up` does not give a working physician worklist/chat end to end — confirm
  intended local-dev story or add it to a compose profile.
- **MFE count drift.** Frontend README says "15 domain MFEs" vs 14 non-template MFEs on
  disk; internally consistent in the docs but worth reconciling in the README.

## 3. Prototype vs repo (readable fragments)

> Based on readable fragments from `Bupa Telemedicine Physician Prototype.html` (a
> self-unpacking React bundle). Full visual review requires opening it in a browser;
> flagged here as partial.

- The physician prototype is a single-app clinical cockpit (worklist → patient context →
  consultation with chat → documentation → close), matching the standalone `physician-app`
  route set more closely than the federated portal.
- **Flag:** repo offers BOTH a federated portal and the standalone app; the prototype
  aligns with the standalone experience. Confirm which the prototype is meant to validate.
- **Action:** render the prototype and do a side-by-side screen walk-through (worklist,
  admit, red-flag ack, consultation, chat, note/diagnosis, closure, medical leave/prescription).

## 4. PRD comparison — PENDING

The PRD (`PRD-TM-001-v0.2`) is a PDF whose text layer could not be reliably extracted in
this environment (output was font/glyph binary). **PRD-vs-repo gap analysis is not done.**
Provide the PRD as text/`.md`/`.docx`, or install a working Python + `pypdf`, and this
section will be completed.
