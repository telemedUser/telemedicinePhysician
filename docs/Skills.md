# Skills — Telemedicine Physician (developer onboarding)

What you need to know to work in the **telemedicinePhysician** workspace, and how to build, run, and
test it locally on **Windows / PowerShell**. Versions and commands here were verified against
`build.gradle`, `gradle.properties`, `package.json`, `nx.json`, the Vite configs, `docker-compose.yml`,
and the `.env.example` in this repo.

---

## 1. Tech stack per layer

### Backend (`telemedicine-backend`)

| Area | Technology | Source of truth |
|------|-----------|-----------------|
| Language / runtime | **Java 21** (`sourceCompatibility`/`targetCompatibility = VERSION_21`) | `build.gradle` |
| Build | **Gradle** multi-module (`settings.gradle`, `build-logic` convention plugins) | `settings.gradle`, `build.gradle` |
| Framework | **Spring Boot 3.3.5** | `gradle.properties` (`springBootVersion`) |
| Cloud | **Spring Cloud 2023.0.3** — Gateway, Consul discovery, Resilience4j | `gradle.properties` (`springCloudVersion`) |
| Edge | **Spring Cloud Gateway** (`gateway`, `:8080`) | `gateway/src/main/resources/application.yml` |
| Service discovery | **HashiCorp Consul** (`:8500`, `lb://` routing) | gateway + per-service `application.yml` |
| AuthN/Z | **Keycloak** (OAuth2/OIDC, resource servers, RBAC annotations) | `java-common-security`, service configs |
| Persistence | PostgreSQL + Spring Data JPA + **Flyway** (schema-per-service) | `physician-service/application.yml` |
| Messaging | **Apache Kafka** (transactional outbox, idempotent consumers) | `physician-service` `kafka/` package |
| Observability | Micrometer/Prometheus, OpenTelemetry/Jaeger | `java-common-observability` |
| Quality | **Checkstyle 10.18.2**, JaCoCo 0.8.12, ArchUnit, Testcontainers | `gradle.properties`, build-logic |
| Python (template only) | FastAPI template at `services/_template-python` (pip) | backend `Makefile` |

Shared libs version line is `1.0.0`; services stay on `0.1.0-SNAPSHOT`.

### Frontend (`telemedicine-frontend`)

| Area | Technology | Source of truth |
|------|-----------|-----------------|
| Runtime | **Node 20** required (see gotchas) | top-level `README.md` prerequisites |
| Package manager | **pnpm 9.15.0** (`packageManager` field) | `package.json` |
| Monorepo | **Nx 20.4.0** | `package.json`, `nx.json` |
| Bundler / dev server | **Vite 5.4** | app `vite.config.mts` |
| UI | **React 18.3** + **TypeScript ~5.6** | `package.json` |
| Microfrontends | **Module Federation** via `@module-federation/vite ^1.2` + `@module-federation/runtime ^0.8` | `package.json`, shell/MFE vite configs |
| Routing | `react-router-dom 6.28` | `package.json` |
| Testing | **Vitest 2.1** + Testing Library + **MSW 2.7** + **axe-core** / `vitest-axe` | `package.json`, `vitest.shared.ts` |
| Workspace globs | `apps/*`, `packages/*`, `tools/*` | `pnpm-workspace.yaml` |

### Platform (`telemedicine-platform`)

| Area | Technology | Source of truth |
|------|-----------|-----------------|
| Local orchestration | **Docker Compose** (profile-driven) | `docker-compose.yml`, `.env.example` |
| Production | **Kubernetes** + Kustomize overlays | `kubernetes/` |
| Make targets | `make up`, `up-all`, `down`, `nuke`, `verify` | `Makefile` |
| Pinned infra images | Consul 1.19, Postgres 16.4, Redis 7.4, Kafka 3.7, Keycloak 25, Prometheus 2.54, Jaeger 1.57 | `.env.example` |

---

## 2. Required developer skills / concepts

- **Microservices** and schema-per-service data ownership.
- **Service discovery** with Consul and gateway `lb://` routing.
- **BFF pattern** — journey/worklist/consultation aggregation with degraded partial responses.
- **OAuth2 / OIDC Authorization Code + PKCE** (Keycloak); JWT roles in `realm_access.roles`.
- **Module Federation / microfrontends** — runtime remote loading from a manifest, shared singletons.
- **Nx monorepo** project graph, targets, caching; **Vite** config and dev server/proxy.
- **WCAG 2.1 AA accessibility** (design-system, axe tests) and **i18n** (es-CL / en).
- **Observability / tracing** — correlation IDs, Prometheus, Jaeger.
- **Docker / Kubernetes** for local and production environments.
- **STOMP over WebSocket** for the physician clinical chat (`/ws/chat/**` → `physician-service`).

---

## 3. Repo layout — where things live

```
telemedicinePhysician/
├── README.md                     # top-level quick start + architecture
├── datastore/                    # optional JSON seed "mock database" (safe to delete)
│   └── seeds/integration-service/   # appointments.json, mpi-patients.json (ids: apt-*, pat-*)
├── telemedicine-backend/         # Java/Gradle microservices
│   ├── build.gradle, settings.gradle, gradle.properties, Makefile
│   ├── gateway/                  # Spring Cloud Gateway (:8080)
│   ├── bff/                      # admin-bff, patient-bff, physician-bff
│   ├── services/                 # domain services (+ _template-java, _template-python)
│   │   └── physician-service/    # worklist, provider mgmt, chat, consultation closure (:8114)
│   ├── platform/                 # config-service, scheduler-service, audit-service
│   ├── shared/                   # java-common-* libs, python-common, contracts
│   ├── simulators/external-simulators/   # vendor mocks (/sim/**, :8200)
│   └── docs/ARCHITECTURE.md
├── telemedicine-frontend/        # Nx + pnpm + Vite + React MF monorepo
│   ├── nx.json, package.json, pnpm-workspace.yaml, tsconfig.base.json
│   ├── vite.aliases.mts, vitest.shared.ts, vitest.workspace.ts
│   ├── apps/                     # shells + MFEs + physician-app + _template-mfe
│   └── packages/                 # 18 shared packages
└── telemedicine-platform/        # Docker Compose + K8s + scripts + docs
    ├── docker-compose.yml, .env.example, Makefile
    ├── scripts/verify-*.ps1 / .sh
    ├── kubernetes/, infra/, ci/
    └── docs/
```

### Physician-specific frontend map

| Project (Nx name) | Dir | Port | Kind |
|-------------------|-----|------|------|
| `physician-shell` | `apps/physician-shell` | 3010 | Module Federation host (clinical theme, PiP) |
| `mfe-physician-consultation` | `apps/mfe-physician-consultation` | 3011 | MF remote → `/physician/consultation` |
| `mfe-physician-documentation` | `apps/mfe-physician-documentation` | 3012 | MF remote → `/physician/documentation` |
| `mfe-physician-patient-context` | `apps/mfe-physician-patient-context` | 3013 | MF remote → `/physician/patient-context` |
| `physician-app` | `apps/physician-app` | 3015 | standalone React app (no MF) |

Other shells (verified from their `vite.config.mts`): `shell` (patient) on `3000`, `admin-shell` on `3020`. Patient and admin MFEs also live under `apps/`.

---

## 4. Build, run, and test locally (Windows / PowerShell)

### 4.0 Prerequisites

- **Docker Desktop** (for the backend/infra stack).
- **Node 20** and **pnpm** (`npm i -g pnpm`, or `corepack enable` to honour `pnpm@9.15.0`).
- **Java 21 + Gradle** only if you build/run Java services outside Docker.

### 4.1 Start the backend stack

From `telemedicine-platform` (the default `.env.example` profile is
`infra,observability,sim,edge,platform`, which brings up infra, observability, simulators, the
gateway, platform services, and the BFFs — including `physician-bff` on `:8091`):

```powershell
cd telemedicine-platform
Copy-Item .env.example .env     # first time only (PowerShell: Copy-Item, not cp)
docker compose up -d --build
```

Verify Phase 0 exit criteria:

```powershell
pwsh ./scripts/verify-phase0.ps1
```

Key URLs:

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| External simulators | http://localhost:8200 |
| Consul UI | http://localhost:8500 |
| Keycloak | http://localhost:8180 |
| Prometheus | http://localhost:9090 |
| Jaeger UI | http://localhost:16686 |

To add the backend domain services, add the `domain` profile:

```powershell
$env:COMPOSE_PROFILES="infra,observability,sim,edge,platform,domain"
docker compose up -d --build
```

> `physician-service` (`:8114`) is **not** defined in `docker-compose.yml` (the `domain` profile
> stops at `monitoring-service` `:8110`). To exercise the physician worklist/chat/provider domain
> APIs locally, run it with Gradle against the running infra containers, e.g.:
>
> ```powershell
> cd telemedicine-backend
> ./gradlew :physician-service:bootRun
> ```
>
> The physician **BFF** does come up via the `platform` profile.

### 4.2 Run the physician frontend (one terminal per project)

CORS in `.env.example` already allows `http://localhost:3010`. Each Vite server uses `strictPort:
true`, so the exact port must be free or the server fails rather than picking another.

Module Federation portal (shell + its three remotes):

```powershell
cd telemedicine-frontend
pnpm install                                  # first time

# terminal 1 — host
pnpm nx serve physician-shell                 # http://localhost:3010

# terminal 2..4 — remotes
pnpm nx serve mfe-physician-consultation      # http://localhost:3011
pnpm nx serve mfe-physician-documentation     # http://localhost:3012
pnpm nx serve mfe-physician-patient-context   # http://localhost:3013
```

Standalone physician app:

```powershell
pnpm nx serve physician-app                   # http://localhost:3015
```

Dev auth bypass (skip Keycloak sign-in, seed a PHYSICIAN role):

```powershell
$env:VITE_AUTH_MODE="dev"; pnpm nx serve physician-shell
```

Physician PiP mode:

```powershell
$env:VITE_PIP_MODE="true"; pnpm nx serve physician-shell
```

Fallback if Nx misbehaves — run Vite directly from the app directory:

```powershell
pnpm exec vite --config apps/physician-shell/vite.config.mts
# or
cd apps/physician-shell; pnpm exec vite
```

> **Proxy targets differ by app.** The MFE portal — `physician-shell`,
> `mfe-physician-consultation`, `mfe-physician-documentation`, and `mfe-physician-patient-context` —
> all proxy `/api/*` to the gateway on `http://localhost:8080`, so the backend compose stack must be
> up for their API/PiP/consultation flows. The standalone `physician-app` is the exception: its
> `vite.config.mts` proxies `/api` **and** `/ws` directly to `physician-service` on
> `http://localhost:8114` (bypassing the gateway), so running it requires `physician-service` up via
> Gradle (`:physician-service:bootRun`), not the gateway.

### 4.3 Build and test the frontend

```powershell
cd telemedicine-frontend
pnpm build                 # nx run-many -t build
pnpm test                  # library tests (nx run-many -t test, tag:type:lib)
pnpm test:coverage         # vitest run --coverage (thresholds: 80 lines/80 fns/75 branches)
pnpm lint                  # ESLint across the workspace
pnpm typecheck             # tsc --noEmit -p tsconfig.base.json
pnpm storybook             # design-system Storybook on :6006
```

Per-project test (physician MFEs have their own `vitest.config.ts` wrapping `vitest.shared.ts`):

```powershell
pnpm nx test mfe-physician-consultation
```

### 4.4 Build and test the backend

```powershell
cd telemedicine-backend
./gradlew build                 # compile + unit tests + quality gates
./gradlew :physician-service:test
./gradlew :physician-bff:test
./gradlew sharedLibsCheck       # quality gate across shared libraries
```

Verification scripts (PowerShell variants exist alongside `.sh`):

```powershell
cd telemedicine-platform
pwsh ./scripts/verify-part5.ps1    # packages
pwsh ./scripts/verify-part6.ps1    # MFEs + shells
```

---

## 5. Conventions and patterns

### Coding standards

- **Backend:** Checkstyle (10.18.2) + JaCoCo coverage gate (80% line / 75% branch) applied by
  `build-logic` convention plugins; `-parameters` compiler flag; JUnit platform.
- **Frontend:** shared ESLint config (`@telemedicine/eslint-config`, extended by root `.eslintrc.cjs`)
  with `jsx-a11y` and `react-hooks`; Prettier (`.prettierrc`); shared TS presets
  (`tsconfig.base.json`, `@telemedicine/tsconfig-presets`).

### Testing

- **Frontend:** Vitest + jsdom, Testing Library, MSW handlers (`@telemedicine/test-utils`), and axe
  accessibility assertions (`vitest-axe`). Shared config in `vitest.shared.ts`; setup in
  `vitest.setup.ts`.
- **Backend:** JUnit 5, Testcontainers (integration), ArchUnit (architecture rules), event assertions
  via `java-common-testing`.

### CI (`.github/workflows/`)

`backend-ci.yml`, `frontend-ci.yml`, `performance-test.yml`, `deploy-staging.yml`, `deploy-prod.yml`.
(Platform also ships copyable CI templates under `telemedicine-platform/ci/templates/`.)

### How to add a backend service

1. `make scaffold-java NAME=my-service` (Linux/macOS) or
   `powershell -File tools/scaffold-service.ps1 -Lang java -Name my-service` (Windows).
2. Register the module in `settings.gradle` (`include` + `projectDir`).
3. Add a gateway route in `gateway/src/main/resources/application.yml` and a matching Resilience4j
   circuit-breaker instance.
4. Give it a Consul-registered `application.yml`, a Postgres schema + Flyway migrations, and events
   wiring if needed. See `telemedicine-platform/docs/adding-a-service.md`.

### How to add an MFE

1. Scaffold: `pnpm nx g @telemedicine/mfe:app <name>` (generator registered in `nx.json`) or
   `pnpm scaffold:mfe` / `node tools/scaffold-mfe.mjs`.
2. In the MFE's `vite.config.mts`: set the `federation` `name`, `exposes { './App': './src/App.tsx' }`,
   `shared` react singletons, a unique `strictPort` port, and the `/api` proxy to `:8080`.
3. Add a `project.json` with `build`/`serve`/`preview` targets (vite via `nx:run-commands`).
4. **Register the remote in the shell manifest** — for the physician portal that is
   `apps/physician-shell/public/config/mfe-manifest.json`: add `{ name, url, route, enabled, module,
   roles, version, minShellVersion }`. See `telemedicine-platform/docs/adding-an-mfe.md`.

### mfe-manifest registration (physician shell)

The shell loads `/config/mfe-manifest.json` at runtime, filters by `enabled`, by the user's roles
(`roles: ["PHYSICIAN"]`), and by config/feature-flag enablement, then mounts each remote at its
`route`. The remote `url` currently points at `http://localhost:30xx/assets/remoteEntry.js`.

---

## 6. Common gotchas

- **Node version / Nx WebAssembly failure.** Use **Node 20**. On Node 24 the Nx daemon falls back to
  an unsupported WebAssembly mode and commands fail with errors like *"Cannot find project 'shell'"*.
  If you see that, you are almost certainly on the wrong Node version (not a missing project).
  If `nvm`/`nvm-windows` is not installed, install Node 20 directly or via `fnm`/`volta`.
- **PowerShell execution policy can block pnpm.** If `pnpm` scripts are blocked, run:
  `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.
- **Strict ports.** Every dev server sets `strictPort: true`. Free the exact port (3010–3013, 3015)
  or the server exits instead of choosing another.
- **`/api` proxy targets differ by app.** The MFE portal dev servers (`physician-shell` + the three
  `mfe-physician-*` remotes) proxy `/api/*` to the gateway on `:8080`; start the backend compose
  stack first or their API/consultation/PiP flows will fail. The standalone `physician-app` instead
  proxies `/api` and `/ws` directly to `physician-service` on `:8114` — so it needs
  `physician-service` running (Gradle `:physician-service:bootRun`), not the gateway. Starting only
  the gateway and then running `physician-app` will still fail because nothing listens on `:8114`.
- **`physician-service` is not in Docker Compose.** Run it with Gradle (`:physician-service:bootRun`)
  if you need the worklist/chat/provider domain APIs. The physician BFF is in compose.
- **PowerShell syntax.** Use `Copy-Item` (not `cp`), `;` to chain (not `&&`), and `$env:VAR="x"` to
  set per-session environment variables (not `VAR=x`).
- **Manifest `remoteEntry.js` path.** The manifest requests `/assets/remoteEntry.js` while Vite
  federation serves `remoteEntry.js` at root; if a remote won't load, verify the served path.
- **Deprecated compose files.** `docker-compose.infrastructure.yml` and
  `docker-compose.full-stack.yml` are empty markers — use `docker-compose.yml` with profiles.

---

## 7. Accuracy notes

- No `.nvmrc` or `engines` field exists in the frontend `package.json`; the Node 20 requirement comes
  from the top-level `README.md` prerequisites and the Nx/WebAssembly behavior described above.
- The frontend README says "17 packages"; the filesystem has **18** under `packages/`.
- `physician-service` binds `:8114` per its `application.yml` but is absent from `docker-compose.yml`.
- The patient `shell` Vite dev port is `3000` (compose `SHELL_PORT=3000`); the README's "(:3000
  patient)" matches the dev server, while `physician-shell` is `3010` and `admin-shell` is `3020`.
