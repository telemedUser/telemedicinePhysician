# Telemedicine Physician — Architecture (HLD + LLD) & Flows

Current state of the repo. Grounded in the code, configs, and the verified
`Features.md` / `Skills.md`. Diagrams are Mermaid. This is the **physician**
repo; it shares the microservices + microfrontends base with the patient repo
but adds `physician-service` and the physician portal/BFF.

## 1. High-Level Design (HLD)

```mermaid
flowchart TB
  subgraph Browser
    PS["Physician Shell (React 18, :3010)"]
    MFE["Physician MFEs (Module Federation)<br/>consultation 3011 · documentation 3012 · patient-context 3013"]
    PA["Standalone physician-app (React/Vite, :3015)<br/>video out of scope here"]
  end

  PS -->|"loads remoteEntry.js at runtime"| MFE

  subgraph Edge
    GW["Spring Cloud Gateway :8080<br/>Resilience4j CB · Redis rate-limit · lb://"]
    KC["Keycloak :8180 (OAuth2 PKCE, client physician-ui)"]
    CONSUL["Consul :8500 (discovery)"]
  end

  MFE -->|"/api/* (proxied)"| GW
  PS -->|"OAuth2 PKCE"| KC
  PA -->|"OAuth2 PKCE"| KC
  GW --> CONSUL

  subgraph Backend["Java Spring Boot services (lb:// via Consul)"]
    PBFF["physician-bff :8091"]
    PSVC["physician-service :8114<br/>worklist · chat (STOMP) · providers · closure"]
    VID["video :8103"]
    TEL["telemetry :8104"]
    DOC["clinical-docs :8105"]
    INT["integration :8107"]
    NOT["notification :8106"]
    MON["monitoring :8110"]
  end

  GW -->|"/bff/physician/**"| PBFF
  GW -->|"/api/v1/physicians/**, /ws/chat/**"| PSVC
  GW --> VID & TEL & DOC & INT
  PA -->|"/api + /ws DIRECT, bypasses gateway"| PSVC

  subgraph Async["Event backbone"]
    K["Kafka :9092<br/>28 domain topics + .RETRY + .DLQ"]
  end
  PSVC & DOC & VID -. publish .-> K
  K -. "consume (idempotent)" .-> NOT & MON

  subgraph External["Simulated external vendors"]
    SIM["external-simulators :8200 /sim/**"]
  end
  INT --> SIM
```

**Request landing (how a call flows):**
- **MFE portal path:** Browser → Physician Shell (:3010) → MFE remote →
  `/api/*` proxied → Gateway (:8080) validates Keycloak JWT, CB + rate-limit,
  Consul `lb://` → `physician-bff` (`/bff/physician/**`) or `physician-service`
  (`/api/v1/physicians/**`). Real-time chat is STOMP over WebSocket via the
  gateway at `/ws/chat/**`.
- **Standalone physician-app path:** physician-app (:3015) proxies `/api` and
  `/ws` **directly to physician-service (:8114), bypassing the gateway** — so it
  needs `physician-service` running (Gradle `:physician-service:bootRun`), which
  is **not** in docker-compose.

## 2. Low-Level Design (LLD) — consultation slice

```mermaid
sequenceDiagram
  participant U as Physician (browser)
  participant S as Shell / consultation MFE
  participant G as Gateway :8080
  participant KC as Keycloak
  participant B as physician-bff :8091
  participant P as physician-service :8114
  participant K as Kafka
  participant M as monitoring-service

  U->>KC: OAuth2 Authorization Code + PKCE (client physician-ui)
  KC-->>U: access token (JWT)
  U->>S: open consultation
  S->>G: GET /bff/physician/consultation/{apptId} (Bearer JWT)
  G->>B: route via Consul (lb://)
  B->>P: aggregate worklist/context/chat/telemetry
  B-->>S: ConsultationResponse (video, chat, telemetry, patientContext)
  S->>G: WS /ws/chat/** (STOMP)
  G->>P: ChatStompController
  S->>G: POST /api/v1/physicians/me/consultations/{id}/close
  G->>P: ConsultationClosureController (requires NOTE,DIAGNOSIS)
  P-->>K: publish ConsultationClosed
  K-->>M: consume (idempotent)
```

**Key LLD facts (from code):**
- `physician-bff` aggregates downstream calls and can return
  `AggregationStatus.DEGRADED` with an advisory message.
- `physician-service` owns worklist (Kafka-fed projection), clinical chat (REST
  + STOMP WebSocket), providers, and consultation closure (emits
  `ConsultationClosed` only when the minimum completion set `NOTE,DIAGNOSIS` is met).
- Role guards: `@IsPhysician` / `@RequiresRole`; dev bypass via
  `DevPhysicianAuthFilter` (`TELEMEDICINE_SECURITY_MODE=DEV`).
- Events use `@DomainEventListener` + `@IdempotentConsumer`; `.RETRY` + `.DLQ` per topic.

## 3. Journey flow diagram (brief)

```mermaid
flowchart LR
  WL["Worklist / admit<br/>physician-service"] --> PC["Patient context<br/>patient-context MFE"]
  PC --> CON["Consultation (video+chat+telemetry)<br/>consultation MFE + bff"]
  CON --> DOCU["Clinical documentation<br/>documentation MFE + clinical-docs"]
  DOCU --> CLOSE["Close consultation<br/>ConsultationClosed event"]
```
Chat is real-time STOMP/WebSocket; closure is gated on NOTE+DIAGNOSIS and emits
a Kafka event consumed by notification/monitoring.

## 4. What is what (quick legend)

| Layer | Tech | Examples |
|-------|------|----------|
| Microfrontends | React 18 + Vite + Module Federation | physician-shell + 3 MFEs |
| Standalone app | React 18 + Vite (no MF) | physician-app :3015 (direct to :8114) |
| Edge | Spring Cloud Gateway | :8080, JWT, CB, rate-limit, /ws/chat/** |
| Discovery / auth | Consul / Keycloak | :8500 / :8180 (physician-ui) |
| Services | Java 21 + Spring Boot 3.3.5 | physician-bff :8091, physician-service :8114 + shared domain svcs |
| Real-time | STOMP over WebSocket | clinical chat /ws/chat/** |
| Async | Kafka | 28 domain events + RETRY + DLQ |
| External (sim) | external-simulators | :8200 /sim/** |
| Observability | Prometheus + Jaeger | :9090 / :16686 |
