# Telemedicine Platform

Orchestration layer that wires together the backend and frontend monorepos for local development and production deployment.

## Quick start (Phase 0)

From the workspace root:

```bash
cd telemedicine-platform
cp .env.example .env          # first time only
docker compose up -d --build
```

Or with Make:

```bash
make -C telemedicine-platform up
make -C telemedicine-platform verify
```

Default profiles (`infra,observability,sim,edge`) bring up infrastructure, observability, external simulators, and the API gateway.

### Verify Phase 0 exit criteria

```powershell
pwsh ./scripts/verify-phase0.ps1
```

```bash
./scripts/verify-phase0.sh
```

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| External simulators | http://localhost:8200 |
| Consul UI | http://localhost:8500 |
| Prometheus | http://localhost:9090 |
| Jaeger UI | http://localhost:16686 |
| Keycloak | http://localhost:8180 |

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/sim/his/appointments/apt-001
```

## Full stack with demo apps

```bash
COMPOSE_PROFILES=infra,observability,sim,edge,apps docker compose up -d --build
```

| Service | URL |
|---------|-----|
| Shell (container) | http://localhost:3000 |
| Template MFE | http://localhost:3001 |

## Structure

```
telemedicine-platform/
├── docker-compose.yml          # canonical profile-driven stack
├── .env.example                # environment contract
├── infra/                      # bootstrap configs (postgres, kafka, minio, keycloak, prometheus, consul)
├── kubernetes/
├── ci/templates/
├── scripts/
│   ├── verify-phase0.ps1
│   └── verify-phase0.sh
└── docs/
```

## Deprecated compose files

`docker-compose.infrastructure.yml` and `docker-compose.full-stack.yml` are empty markers. Use `docker-compose.yml` with profiles instead.

## Documentation

- [Architecture](./docs/architecture.md)
- [Build sequence](./docs/build-sequence.md)
- [Operational manual](./docs/operational-manual.md)
- [Adding a backend service](./docs/adding-a-service.md)
- [Adding a microfrontend](./docs/adding-an-mfe.md)

## CI templates

Copy workflow files from `ci/templates/` into `.github/workflows/` at the monorepo root or per-repo as needed.
