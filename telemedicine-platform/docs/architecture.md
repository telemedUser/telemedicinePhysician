# Telemedicine Platform Architecture

## Overview

Three-repository layout for independent deployability:

| Repository | Role |
|------------|------|
| `telemedicine-backend` | API Gateway, Consul, Java/Python microservices |
| `telemedicine-frontend` | Shell container + Module Federation microfrontends |
| `telemedicine-platform` | Full-stack orchestration, K8s ingress, CI templates |

## Request flow

```
Browser → Shell (port 3000)
            ├─ loads MFE remotes from mfe-manifest.json
            └─ /api/* → Gateway (port 8080) → Consul lb://service-name
```

## Service discovery

All backend microservices register with Consul on startup. The gateway resolves routes using `lb://<service-name>` — external clients never call services directly.

## Microfrontend registry

Only MFEs listed in `apps/shell/public/config/mfe-manifest.json` are loaded at runtime via `@module-federation/runtime`. Deploy a new remote, add a manifest entry, and the shell picks it up without rebuilding (when using runtime federation URLs).

## Environments

| Environment | Command | Notes |
|-------------|---------|-------|
| Phase 0 (infra + gateway + sim) | `cd telemedicine-platform && cp .env.example .env && docker compose up -d --build` | Consul :8500, Gateway :8080, Simulators :8200 |
| Backend only | `make -C telemedicine-backend up` | Consul UI :8500, Gateway :8080 |
| Frontend only | `pnpm --dir telemedicine-frontend dev:all` | Shell :3000, MFE :3001 |
| Full stack + apps | `COMPOSE_PROFILES=infra,observability,sim,edge,apps docker compose up -d --build` | All demo services |

## Port map (Phase 0)

| Service | Port |
|---------|------|
| Gateway | 8080 |
| External simulators | 8200 |
| Consul UI | 8500 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka (host) | 29092 |
| MinIO API / console | 9000 / 9001 |
| Keycloak | 8180 |
| Prometheus | 9090 |
| Jaeger UI | 16686 |
| OTLP gRPC / HTTP | 4317 / 4318 |

## Kubernetes

Apply platform kustomization (adjust paths if repos are split):

```bash
kubectl apply -k telemedicine-platform/kubernetes
```

Ingress routes:
- `app.example.com` → shell
- `app.example.com/mfe/template/` → template-mfe
- `api.example.com` → gateway

## Adding new services

See [adding-a-service.md](./adding-a-service.md) and [adding-an-mfe.md](./adding-an-mfe.md).
