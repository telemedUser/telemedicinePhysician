# Telemedicine Platform — Security Review Checklist

Production go-live security gate. Each item must be **PASS** or have an approved exception before release.

## Identity & access

| # | Control | Implementation | Verify |
|---|---------|----------------|--------|
| 1 | OAuth2 Authorization Code + PKCE for shells | Keycloak clients `patient-portal`, `physician-ui`, `admin-console` | Shell login flow against prod realm |
| 2 | JWT validation at gateway (prod profile) | `telemedicine.security.mode=PROD`, `KEYCLOAK_JWK_URI` | Gateway rejects unsigned/expired tokens |
| 3 | RBAC enforced on gateway routes | `GatewaySecurityConfig` path → role map | Patient cannot call `/bff/admin/**` |
| 4 | MFA for administrators | `realm-telemedicine-prod.json` OTP required for `ADMIN` | Admin login prompts for TOTP |
| 5 | Step-up auth for clinical actions | identity-service FONASA 2FA before consultation | Identity validate + 2FA in journey |
| 6 | Service accounts for gateway → Keycloak | `gateway` confidential client, no direct grants | No password grant in prod |

## Authentication hardening (Keycloak)

| # | Control | Setting | Verify |
|---|---------|---------|--------|
| 7 | Password policy | min 12 chars, upper/lower/digit/special, not username | Attempt weak password change |
| 8 | Brute-force protection | lockout after 5 failures, 15 min wait | Lockout after repeated failures |
| 9 | Session idle timeout | 30 min SSO idle | Re-auth after idle |
| 10 | Max session lifespan | 8 hours | Force re-login after max |
| 11 | Access token TTL | 5 min + refresh rotation | Token expires as configured |

## Transport & network

| # | Control | Implementation | Verify |
|---|---------|----------------|--------|
| 12 | TLS 1.3 termination at ingress | `kubernetes/ingress/ingress.yaml` cert-manager | SSL Labs scan ≥ A |
| 13 | HSTS enabled | Ingress annotation `Strict-Transport-Security` | Response headers include HSTS |
| 14 | Kafka TLS in production | `application-prod-kafka.yml`, broker certs | Producer/consumer handshake succeeds |
| 15 | Network policies applied | `kubernetes/security/network-policies.yaml` | Pod-to-pod denied except allow-list |
| 16 | Gateway only ingress to BFF/domain | BFF policies accept gateway label only | Direct BFF pod access blocked |
| 17 | Simulators excluded from prod | No `external-simulators` in prod manifests | `kubectl get deploy -n telemedicine` |

## Data protection

| # | Control | Implementation | Verify |
|---|---------|----------------|--------|
| 18 | PHI redaction in logs | `java-common-observability` MDC filter | No national IDs in log samples |
| 19 | AES-256 column encryption | `java-common-persistence` converter | Encrypted columns in DB |
| 20 | MinIO SSE for clinical docs | Bucket policy + server-side encryption | Object metadata shows SSE |
| 21 | Encrypted backups | PostgreSQL WAL + snapshot encryption | Backup restore drill |
| 22 | Audit hash chain | audit-service append-only ledger | `GET /api/v1/audit/chain/verify` |

## Application security

| # | Control | Implementation | Verify |
|---|---------|----------------|--------|
| 23 | Rate limiting at gateway | Redis `RequestRateLimiter` | 429 after burst exceeded |
| 24 | Circuit breakers per route | resilience4j in gateway + services | Failover under vendor outage |
| 25 | CORS restricted to shell origins | `CORS_ALLOWED_ORIGINS` env | Preflight from unknown origin fails |
| 26 | Request size limits | gateway `max-header-size`, web filters | Oversized payload rejected |
| 27 | Webhook signature verification | integration-service inbound webhooks | Invalid signature → 401 |
| 28 | Idempotency keys on mutating APIs | `java-common-web` filter | Duplicate POST deduplicated |

## Secrets & operations

| # | Control | Implementation | Verify |
|---|---------|----------------|--------|
| 29 | Secrets in K8s Secret / vault | No secrets in ConfigMaps or git | `grep -r password` in manifests clean |
| 30 | Secret rotation procedure | [operational-manual.md](./operational-manual.md#secret-rotation) | Rotation drill completed |
| 31 | Certificate expiry monitoring | Prometheus `CertificateExpiringSoon` | Alert fires < 30 days |
| 32 | Dependency vulnerability scan | CI `backend-service.yml` OWASP/Trivy gate | No critical CVEs open |

## Compliance & audit

| # | Control | Implementation | Verify |
|---|---------|----------------|--------|
| 33 | Consent ledger immutable | audit-service consent module | Withdrawal creates new entry, no delete |
| 34 | 7-year audit retention | scheduler job `audit-retention` | Retention policy documented |
| 35 | DR RPO/RTO validated | DR runbook RB-004 exercise | RPO < 15 min, RTO < 2 hr |

## Sign-off

| Role | Name | Date | Result |
|------|------|------|--------|
| Security lead | | | |
| Platform architect | | | |
| Compliance officer | | | |
| Release manager | | | |
