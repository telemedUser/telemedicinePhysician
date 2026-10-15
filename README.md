# Telemedicine — Microservices + Microfrontends

Base project for a telemedicine platform with independently deployable backend microservices and frontend microfrontends.

## Repositories

| Folder | Description | Start command |
|--------|-------------|---------------|
| [telemedicine-backend](./telemedicine-backend/) | Spring Cloud Gateway, Consul, Java + Python services | `make -C telemedicine-backend up` |
| [telemedicine-frontend](./telemedicine-frontend/) | Nx monorepo, shell + Module Federation MFEs | `pnpm --dir telemedicine-frontend dev:all` |
| [telemedicine-platform](./telemedicine-platform/) | Full-stack Docker Compose + K8s + CI | `make -C telemedicine-platform up` |

## Phase 0 quick start

```bash
cd telemedicine-platform
cp .env.example .env
docker compose up -d --build
pwsh ./scripts/verify-phase0.ps1
```

Exit criteria: infra + gateway + Consul healthy; simulators respond on `/sim/**` via gateway.

## Prerequisites

- Docker Desktop (for full-stack)
- Java 21 + Gradle (optional, Docker builds Java services)
- Python 3.11+ + pip (optional, for local Python dev)
- Node 20 + pnpm (for frontend dev)

## Architecture

```
Browser → Shell (:3000) → MFE remotes (Module Federation)
                ↓
         Gateway (:8080) → Consul → Java / Python services
                              ↘ external-simulators (:8200) /sim/**
```

## Next steps

Follow the build sequence in [telemedicine-platform/docs/build-sequence.md](./telemedicine-platform/docs/build-sequence.md).
