# Telemedicine Platform — Production Readiness Checklist

Gate document for production go-live. All items must be green before traffic cutover.

## Infrastructure

- [ ] Kubernetes cluster provisioned with CNI supporting NetworkPolicy
- [ ] `kubectl apply -k telemedicine-platform/kubernetes/` succeeds
- [ ] PostgreSQL HA primary + standby operational
- [ ] Kafka 3-broker cluster with TLS enabled
- [ ] Redis cluster for rate limiting and cache
- [ ] MinIO with SSE and bucket replication
- [ ] Keycloak prod realm imported from `realm-telemedicine-prod.json`
- [ ] TLS certificates issued (cert-manager) and probed by Prometheus

## Security

- [ ] [security-review.md](./security-review.md) signed off (all 35 controls PASS or excepted)
- [ ] Network policies applied and validated (direct BFF access blocked)
- [ ] Gateway JWT validation enabled (`TELEMEDICINE_SECURITY_MODE=PROD`)
- [ ] MFA enforced for ADMIN role
- [ ] Secrets stored in K8s Secret / vault (none in git)
- [ ] `external-simulators` not deployed in production namespace
- [ ] Kafka TLS configured per `application-prod-kafka.yml`

## Performance & reliability

- [ ] k6 suite passes with p95 < 300ms:
  - `patient-journey.js`
  - `gateway-load.js`
  - `bff-load.js`
  - `domain-services.js`
  - `event-throughput.js`
- [ ] Prometheus alert rules loaded (`platform-alerts.yml`)
- [ ] SLO burn-rate recording rules active
- [ ] Gateway HPA and PDB verified under load
- [ ] DR exercise completed (RPO < 15 min, RTO < 2 hr documented)

## CI/CD

- [ ] Backend pipeline: build → test (≥80%) → image → deploy
- [ ] Frontend pipeline: lint → test (≥80%) → build → publish MFE artifacts
- [ ] Blue/Green ingress annotations configured
- [ ] Canary rollback tested on SLO alert
- [ ] `verify-part7.ps1` integrated as deployment gate

## Documentation & operations

- [ ] [technical-design-document.md](./technical-design-document.md) — production K8s, CI/CD, security, HA/DR sections current
- [ ] [operational-manual.md](./operational-manual.md) — prod deployment, secret rotation, DR, Kafka TLS
- [ ] [runbooks.md](./runbooks.md) — RB-001 through RB-011 complete
- [ ] [traceability-matrix.md](./traceability-matrix.md) — NFR verification links updated
- [ ] [traceability.json](./traceability.json) — machine-checkable NFR map
- [ ] On-call rotation and escalation path documented

## Sign-off

| Gate | Owner | Date | Status |
|------|-------|------|--------|
| Infrastructure | | | |
| Security | | | |
| Performance | | | |
| Operations | | | |
| Product / Clinical | | | |

**Final command:** `./scripts/verify-part7.ps1` must report ALL CHECKS PASSED.
