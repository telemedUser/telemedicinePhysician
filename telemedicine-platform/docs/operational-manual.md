# Telemedicine Platform — Operational Manual



## Environments



| Environment | Compose stack | Gateway | Shells |

|-------------|---------------|---------|--------|

| Local Phase 0 | `telemedicine-platform/docker-compose.yml` (profiles: infra, observability, sim, edge) | :8080 | Patient :3000, Physician :3010, Admin :3020 (apps profile) |

| Local full stack | Same + `apps` profile | :8080 | Shell :3000, MFE :3001 |

| Production | Kubernetes (`telemedicine-platform/kubernetes`) | api.example.com | app.example.com |

## Production deployment

Deploy the full stack to Kubernetes using Kustomize:

```bash
cd telemedicine-platform

# Import production Keycloak realm (once per environment)
kubectl create secret generic keycloak-realm \
  --from-file=realm-telemedicine-prod.json=infra/keycloak/realm-telemedicine-prod.json \
  -n telemedicine --dry-run=client -o yaml | kubectl apply -f -

# Apply manifests (namespace, gateway HA, ingress, network policies, DR, service bases)
kubectl apply -k kubernetes/

# Verify rollout
kubectl rollout status deployment/gateway -n telemedicine
./scripts/verify-part7.ps1
```

Production-specific configuration:

| Setting | Source | Notes |
|---------|--------|-------|
| JWT validation | Gateway env `TELEMEDICINE_SECURITY_MODE=PROD` | Requires `KEYCLOAK_JWK_URI` |
| Kafka TLS | `infra/kafka/application-prod-kafka.yml` | Mount JKS truststore/keystore from Secret |
| Network segmentation | `kubernetes/security/network-policies.yaml` | Requires CNI with NetworkPolicy support |
| TLS certificates | cert-manager + ingress | Monitored by `CertificateExpiringSoon` alert |

Blue/Green cutover: update ingress annotation `nginx.ingress.kubernetes.io/canary-weight` or color selector per [runbooks.md](./runbooks.md#rb-006-deployment-rollback-bluegreen).

## Secret rotation

Rotate credentials on a quarterly schedule or immediately after suspected compromise.

| Secret | Rotation procedure | Consumers |
|--------|-------------------|-----------|
| Gateway Keycloak client secret | Regenerate in Keycloak → update K8s Secret → rolling restart gateway | gateway |
| Kafka TLS keystores | Issue new certs → update `kafka-tls` Secret → rolling restart brokers then services | all domain/platform services |
| PostgreSQL passwords | `ALTER ROLE` on primary → update service Secrets → rolling restart | all services with JDBC |
| Redis password | Update Redis ACL → update Secrets → rolling restart gateway + services | gateway, waiting-room |
| MinIO access keys | Create new key pair → update Secrets → revoke old key | clinical-docs, audit |

Rotation drill checklist:

1. Announce maintenance window (no DR failover during rotation)
2. Rotate one secret class at a time
3. Confirm `/actuator/health` green on all pods
4. Run `./scripts/verify-part7.ps1` smoke checks
5. Record rotation in audit log

## Disaster recovery

**Targets:** RPO < 15 minutes, RTO < 2 hours.

| Step | Action | Reference |
|------|--------|-----------|
| 1 | Detect regional outage (Prometheus + external probe) | RB-004 |
| 2 | Promote PostgreSQL standby in secondary region | `kubernetes/dr/postgres-failover.yaml` |
| 3 | Update DNS for `api.example.com` to secondary ingress | DNS runbook |
| 4 | Verify Flyway schema versions match primary | `kubectl exec` into postgres |
| 5 | Smoke test patient journey BFF | `verify-part7.ps1` check 12 |
| 6 | Document incident timeline | compliance ticket |

Post-failover: re-establish replication, validate Kafka consumer lag, confirm MinIO bucket sync.

## Kafka TLS (production)

Development compose uses plaintext Kafka on `:9092`. Production requires TLS on port **9093**.

### Broker configuration

- Three brokers with inter-broker SSL
- Client authentication via mutual TLS (JKS keystore per service)
- Truststore contains cluster CA certificate

### Spring Boot services

Import the production Kafka profile:

```yaml
spring:
  config:
    import: optional:file:/config/application-prod-kafka.yml
```

Or set environment variables directly:

| Variable | Purpose |
|----------|---------|
| `KAFKA_BOOTSTRAP` | `kafka-0.kafka.telemedicine.svc:9093,...` |
| `KAFKA_TRUSTSTORE_PASSWORD` | Truststore unlock |
| `KAFKA_KEYSTORE_PASSWORD` | Client keystore unlock |
| `KAFKA_KEY_PASSWORD` | Client key unlock |

Full profile: `infra/kafka/application-prod-kafka.yml`

### Kubernetes mount

```yaml
volumeMounts:
  - name: kafka-tls
    mountPath: /etc/kafka/tls
    readOnly: true
volumes:
  - name: kafka-tls
    secret:
      secretName: kafka-client-tls
```

### Verification

```bash
# From a domain service pod
openssl s_client -connect kafka-0.kafka.telemedicine.svc:9093 -CAfile /etc/kafka/tls/ca.crt

# Confirm producer metrics
curl -s http://gateway:8080/actuator/prometheus | grep kafka_producer
```

See [security-review.md](./security-review.md) item 14 and [production-readiness.md](./production-readiness.md).




```bash

cd telemedicine-platform

cp .env.example .env          # first time only

docker compose up -d --build    # uses COMPOSE_PROFILES from .env

./scripts/verify-phase0.sh      # or: pwsh ./scripts/verify-phase0.ps1

```



Default profiles in `.env`: `infra,observability,sim,edge`



## Compose profiles



| Profile | Services |

|---------|----------|

| `infra` | consul, postgres, redis, kafka (+ kafka-init), minio (+ minio-init), keycloak |

| `observability` | prometheus, jaeger |

| `logs` | elasticsearch (opt-in) |

| `sim` | external-simulators (:8200) |

| `edge` | gateway (:8080) |

| `platform` | config-service, audit-service, scheduler-service, patient-bff, physician-bff, admin-bff |

| `domain` | All ten core domain services (:8101–:8110) — waiting-room through monitoring |

| `apps` | template-java, template-python, shell, template-mfe |



**Do not** run the deprecated `docker-compose.infrastructure.yml` and `docker-compose.full-stack.yml` alongside `docker-compose.yml` — they are empty markers to prevent port collisions.



## Startup order



1. Infrastructure: PostgreSQL, Redis, Kafka (+ topic bootstrap), MinIO (+ bucket bootstrap), Keycloak, Consul

2. Observability: Prometheus, Jaeger

3. External simulators (Consul registration)

4. API gateway (waits for infra init jobs + Consul)

5. Platform services (`platform` profile): config, audit, scheduler, BFFs

6. Domain services (`domain` profile): integration ACL, waiting-room, identity, notification, video, telemetry, clinical-docs, survey, reporting, monitoring

7. Frontend shells and MFE remotes (`apps` profile)



## Health checks



- Gateway: `GET /actuator/health`

- External simulators: `GET /actuator/health` or `GET /health`

- Each service: `/actuator/health` only (no custom `/health` controllers on domain/platform services)

- Part 3 verify: `telemedicine-platform/scripts/verify-part3.ps1` (or `.sh`)
- Part 4 verify: `telemedicine-platform/scripts/verify-part4.ps1` (simulator admin API, latency, failover)



## External simulators (Part 4)

Dev/test only (`sim` compose profile, port **8200**). **Never** set `TELEMEDICINE_ENVIRONMENT=production` — the app refuses to start.

| Operation | Command |
|-----------|---------|
| List vendor config | `GET /sim/admin/vendors` |
| Inject latency | `PUT /sim/admin/vendors/his` body `{"latencyMs":200}` |
| Chaos drill | `POST /sim/admin/scenarios/chaos` |
| Reset happy path | `POST /sim/admin/scenarios/happy` |
| Resilience drill | `POST /sim/admin/scenarios/resilience` |
| Clear state | `DELETE /sim/admin/state` |

Scenario catalogue: [simulator-scenarios.md](./simulator-scenarios.md). HTTP journey files under `scripts/simulator-scenarios/`.



Environment contract: `telemedicine-platform/.env.example` → copy to `.env`.



Key variables:



| Variable | Purpose |

|----------|---------|

| `COMPOSE_PROFILES` | Which service groups to start |

| `CORS_ALLOWED_ORIGINS` | Gateway CORS for shells :3000, :3010, :3020 |

| `KAFKA_BOOTSTRAP` | In-network Kafka address (`kafka:9092`) |

| `KAFKA_HOST_PORT` | Host-side Kafka (`localhost:29092`) |

| `OTEL_EXPORTER_OTLP_ENDPOINT` | Traces → Jaeger (`http://jaeger:4317`) |

| `KEYCLOAK_JWK_URI` | JWT validation (prod gateway profile) |



## Platform services (Phase 2)

| Service | Port | Database | Notes |
|---------|------|----------|-------|
| `config-service` | 8112 | `config` | Typed keys, Redis snapshot cache, `ConfigurationChanged` events |
| `audit-service` | 8111 | `audit` | Append-only hash chain, consent ledger, MinIO exports |
| `scheduler-service` | 8113 | `scheduler` | Quartz JDBC clustered jobs; no outbox sweeper |

Runtime configuration is managed by **config-service**, cached in Redis, and read by services through `PlatformConfigClient` (`java-common-integration`). Admin changes flow through `admin-bff` or direct `PUT /api/v1/config/{key}` (admin role required in non-dev profiles).

Compose profile **`platform`** starts config, audit, scheduler, and the three BFFs:

```bash
cd telemedicine-platform
COMPOSE_PROFILES=infra,observability,sim,edge,platform docker compose up -d
./scripts/verify-phase2.ps1
```



## Monitoring



- Prometheus: http://localhost:9090 — auto-discovers Consul-registered services with `metrics-path=/actuator/prometheus` tag

- Jaeger UI: http://localhost:16686 — OTLP gRPC :4317, HTTP :4318

- Admin operational snapshot (Phase 8): `GET /api/v1/monitoring/snapshot`



## Backup & retention



- PostgreSQL: daily snapshots, 30-day retention (production)

- MinIO clinical documents: versioned buckets (`clinical-documents`, `prescriptions`, `audit-exports`)

- Audit events: append-only with hash chaining, 7-year retention policy



## Support escalation



1. L1 — run `verify-phase0.ps1`; check gateway and Consul health

2. L2 — inspect service logs (correlation ID from `X-Correlation-Id` header)

3. L3 — invoke DR runbook if regional outage

