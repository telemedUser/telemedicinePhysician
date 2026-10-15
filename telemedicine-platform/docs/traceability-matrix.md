# Telemedicine Platform — Requirements Traceability Matrix

| Requirement ID | Source | Capability | Module(s) | Verification |
|----------------|--------|------------|-----------|--------------|
| FE-PKG-01 | Part 5 | Reusable frontend packages (17) | shared-types, design-system, api-client, auth, session-context, mfe-registry, adapters, consent-kit, device-test, i18n, feature-flags, observability-web, test-utils | `verify-part5.ps1`; package tests ≥80%; Storybook build |
| FE-MFE-01 | Part 6 | Patient journey MFEs (6) | shell, mfe-patient-* | `verify-part6.ps1`; journey gating; i18n namespaces |
| FE-MFE-02 | Part 6 | Physician MFEs (3) | physician-shell, mfe-physician-* | PiP mode; worklist + documentation |
| FE-MFE-03 | Part 6 | Admin MFEs (5) | admin-shell, mfe-admin-* | KPI dashboards; config CRUD; audit search |
| PAT-02 | Patient fn 2 | Platform & clinical consent | waiting-room-service/consent, consent-kit, mfe-patient-consent | Consent grant/withdraw + `HttpConsentVerifier` |
| PAT-03 | Patient fn 3 | Identity validation | identity-service, mfe-patient-identity | `/api/v1/identity/validate`; MPI via integration ACL |
| PAT-04 | Patient fn 4 | FONASA 2FA | identity-service, integration-service/FONASA adapter | 2FA via integration internal API |
| PAT-05 | Patient fn 5 | Device test | device-test, waiting-room-service/device-test | POST device-test + `DeviceTestCompleted` event |
| PAT-06 | Patient fn 6 | Waiting room queue | waiting-room-service/queue, mfe-patient-waiting-room | GET queue; Redis + `PatientEnteredWaitingRoom` |
| PAT-07 | Patient fn 7 | Video consultation | video-service, video-adapter, mfe-patient-consultation | Session + token; `ProviderSelector` failover |
| PAT-08 | Patient fn 8 | Clinical chat | video-service/chat, chat-adapter | Chat persist + `ChatMessageSent` (PHI-free payload) |
| PAT-09 | Patient fn 9 | Clinical documents | clinical-docs-service, mfe-patient-documents | MinIO + download token; physician-bff docs POST |
| PAT-10 | Patient fn 10 | Satisfaction survey | survey-service, mfe-patient-survey | `ConsultationClosed` → `SurveyRequested` → NPS |
| PHY-01..07 | Physician fns | PiP consultation & docs | physician-shell, physician-bff, physician MFEs | Worklist + consultation MFE |
| ADM-01..07 | Admin fns | Config, KPIs, audit, RBAC | config-service, reporting-service, audit-service, admin MFEs | Admin BFF + dashboards; gateway RBAC on `/bff/admin/**` and `/api/v1/config/**` |
| NFR-01 | NFR table | OAuth2 + JWT + RBAC | gateway, java-common-security, auth | [security-review.md](./security-review.md); [realm-telemedicine-prod.json](../infra/keycloak/realm-telemedicine-prod.json); `verify-part7.ps1` |
| NFR-02 | NFR table | TLS, HSTS, network segmentation | kubernetes/ingress, network-policies | [security-review.md](./security-review.md#transport--network); `kubernetes/security/network-policies.yaml` |
| NFR-03 | NFR table | MFA, password policy, session timeouts | Keycloak prod realm | [realm-telemedicine-prod.json](../infra/keycloak/realm-telemedicine-prod.json); security-review items 4, 7–11 |
| NFR-04 | NFR table | Kafka TLS | application-prod-kafka.yml | [operational-manual.md](./operational-manual.md#kafka-tls-production); `infra/kafka/application-prod-kafka.yml` |
| NFR-05 | NFR table | Circuit breakers, DLQ, failover | gateway, java-common-integration, video-service | [platform-alerts.yml](../infra/prometheus/rules/platform-alerts.yml); [RB-008](./runbooks.md#rb-008-dlq-messages-accumulating), [RB-009](./runbooks.md#rb-009-video-provider-failover-production) |
| NFR-06 | NFR table | Outbox pattern + stall detection | java-common-events, java-common-persistence | `OutboxPublishStalled` alert; [RB-007](./runbooks.md#rb-007-outbox-publish-stalled) |
| NFR-07 | NFR table | HA/DR RPO/RTO | kubernetes/ha, kubernetes/dr | [operational-manual.md#disaster-recovery](./operational-manual.md#disaster-recovery); [RB-004](./runbooks.md#rb-004-database-failover-dr) |
| NFR-08 | NFR table | p95 latency < 300ms | tests/k6 | `tests/k6/patient-journey.js`, `gateway-load.js`, `bff-load.js`, `domain-services.js`, `event-throughput.js` |
| NFR-09 | NFR table | SLO burn-rate alerting | monitoring-service, Prometheus rules | [platform-alerts.yml](../infra/prometheus/rules/platform-alerts.yml) `slo-recording-rules` group |
| NFR-10 | NFR table | 80%+ test coverage | CI templates | `verify-part5.ps1`, `verify-part6.ps1`, `ci/templates/*.yml` |
| NFR-11 | NFR table | Production ops readiness | docs, scripts | [production-readiness.md](./production-readiness.md); `verify-part7.ps1` |
| NFR-12 | NFR table | Audit, consent, retention | audit-service, waiting-room/consent | `verify-phase2.ps1`; security-review items 33–35 |
| SCOPE-01 | Appendix A | Scheduling read-only | integration-service ACL | No local scheduling logic |
| SCOPE-02 | Appendix A | Payment read-only | integration-service payment validate | Adapter-only validation |
| EXT-01..08 | External systems | Vendor simulation | external-simulators `/sim/**` | `verify-part4.ps1`; admin API + scenario catalogue |
| RES-01..03 | Resilience NFR | Failover, latency, chaos drills | SimulatorEngine, `/sim/admin/scenarios/*` | failover.http; k6 `simulator-load.js` |

## Event traceability

| Event | Producer | Consumer(s) |
|-------|----------|-------------|
| LinkGenerated | waiting-room-service | notification-service, audit-service |
| ConsentGranted / ConsentWithdrawn | waiting-room-service | audit-service |
| IdentityValidated | identity-service | waiting-room-service, audit-service |
| VideoSessionStarted / Ended | video-service | monitoring-service, reporting-service |
| ChatMessageSent | video-service | audit-service |
| TelemetryMeasured | telemetry-service | monitoring-service, reporting-service |
| ClinicalDocumentIssued | clinical-docs-service | notification-service, reporting-service |
| ConsultationClosed | integration-service | survey-service, notification-service |
| ConnectionQualityDegraded | monitoring-service | reporting-service, alerting |
| SurveyRequested / SurveyCompleted | survey-service | notification-service, reporting-service |
| NotificationSent / Failed | notification-service | monitoring-service, reporting-service |
| IntegrationFailed | integration-service | monitoring-service, reporting-service |
| ConfigurationChanged | config-service | video-service, telemetry-service, notification-service, feature-flags |

## Part 6 verification

```powershell
cd telemedicine-platform
./scripts/verify-part6.ps1
```

Covers: all 14 MFEs + 3 shells build and test; manifest `roles` + `version`; workspace dependency declarations.


Run after `COMPOSE_PROFILES=infra,observability,sim,edge,platform,domain`:

```powershell
cd telemedicine-platform
./scripts/verify-part3.ps1
```

Gradle (no compose):

```powershell
cd telemedicine-backend
./gradlew :external-simulators:check :java-common-testing:test --tests "*SimulatorStubs*"
```

## Part 4 verification

Run after `COMPOSE_PROFILES=infra,observability,sim,edge`:

```powershell
cd telemedicine-platform
./scripts/verify-part4.ps1
```

See [simulator-scenarios.md](./simulator-scenarios.md).

## Part 7 verification (production hardening)

Run after security artifacts, k6 scripts, and Prometheus rules are in place:

```powershell
cd telemedicine-platform
./scripts/verify-part7.ps1
```

Covers: security-review checklist, network policies, Keycloak prod realm, Kafka TLS config, k6 test suite presence, Prometheus alert rules, documentation completeness, traceability.json, production-readiness.md.

Machine-checkable NFR map: [traceability.json](./traceability.json)

## Scope boundary confirmation

- **Scheduling** and **payment processing** remain external; platform validates via `integration-service` adapters only.
- **ANURA telemetry** is independently deployable in `telemetry-service` with zero compile-time coupling to `video-service`.
