# Telemedicine Platform — Runbooks

## RB-001 Gateway high error rate

**Trigger:** Error rate > 5% for 5 minutes on `api.example.com`

1. Check gateway pods: `kubectl get pods -n telemedicine -l app=gateway`
2. Verify Consul health: all upstream services registered
3. Inspect circuit breaker metrics in Prometheus
4. Scale gateway replicas if CPU > 70%
5. Roll back last deployment if error spike correlates with release

## RB-002 Video provider outage

**Trigger:** `ConnectionQualityDegraded` events or Vonage outage simulator in prod

1. Confirm active provider via config-service: `GET /api/v1/config/video.provider`
2. Switch provider via config-service: `PUT /api/v1/config/video.provider` to `acs` (or `simulator` in dev)
3. Optional drill: `POST /sim/vonage/outage` with `{"enabled":true}` on external-simulators (dev/test only)
4. Notify physicians via notification-service template `physician-provider-failover`
5. Post-incident: review failover timing against SLO

## RB-002b Validating provider failover (dev/test)

Use external-simulators only (compose profile `sim`):

1. `POST /sim/admin/scenarios/happy` — reset vendors
2. `POST /sim/vonage/outage` with `{"enabled":true}`
3. Create video session through gateway — expect failover to ACS when `video.provider=vonage`
4. `POST /sim/acs/session` directly — should return 200 while Vonage is out
5. Restore: `POST /sim/vonage/outage` with `{"enabled":false}`

Script: `telemedicine-platform/scripts/simulator-scenarios/failover.http`

## RB-003 Kafka consumer lag / outbox backlog

**Trigger:** Consumer lag > 10,000 messages or `KafkaConsumerLagHigh` alert

1. Check monitoring-service and outbox publisher logs in each service (see `java-common-persistence` `OutboxPublisher`).
2. Trigger per-service outbox recovery: inspect `outbox_events` where `published = false`; restart the affected service if the poller stalled. **Do not** use scheduler for outbox sweeps.
3. Manually trigger a platform job when needed: `POST /api/v1/scheduler/jobs/audit-retention/trigger` (retention) via gateway.
4. Scale affected consumer deployment
5. Replay dead-letter topic if needed (see RB-008)

## RB-004 Database failover (DR)

**Target:** RPO < 15 min, RTO < 2 hours

1. Promote PostgreSQL standby in secondary region (see `kubernetes/dr/postgres-failover.yaml`)
2. Update DNS for `api.example.com` to secondary ingress
3. Verify Flyway schema versions match primary
4. Run smoke test: patient journey BFF call
5. Document incident timeline for compliance

## RB-005 Consent withdrawal mid-journey

**Trigger:** Patient invokes consent withdrawal

1. `POST /api/v1/waiting-room/consent/withdraw`
2. Audit-service records immutable consent ledger entry
3. Terminate active video session if in consultation
4. Block further journey progression until re-consent

## RB-006 Deployment rollback (Blue/Green)

1. Switch ingress traffic annotation from `color=green` to `color=blue`
2. Verify canary metrics for 5 minutes before full cutover
3. Keep previous color running for 1 hour for fast rollback

## RB-007 Outbox publish stalled

**Trigger:** `OutboxPublishStalled` alert — unpublished events accumulating, zero publish rate

1. Identify affected service from alert label `{{ $labels.service }}`
2. Query outbox table: `SELECT count(*) FROM outbox_events WHERE published = false AND created_at < now() - interval '10 minutes'`
3. Check service logs for Kafka connection or TLS errors
4. Verify Kafka broker health and TLS certificates (see operational manual Kafka TLS section)
5. Rolling restart the affected service deployment
6. If stall persists, check `spring.kafka` SSL config against `application-prod-kafka.yml`
7. Escalate to L3 if unpublished count exceeds 10,000

## RB-008 DLQ messages accumulating

**Trigger:** `DLQMessagesAccumulating` alert — dead-letter topic depth > 100

1. Identify DLQ topic from alert: `kafka_topic_partition_current_offset{topic=~".*\\.dlq"}`
2. Inspect message headers for `correlationId` and original event type
3. Fix root cause (schema mismatch, downstream outage, poison message)
4. Replay messages to original topic using Kafka connect or manual consumer script
5. Purge DLQ only after successful replay and audit log entry
6. Scale consumer deployment if processing rate is insufficient

## RB-009 Video provider failover (production)

**Trigger:** `VideoProviderFailover` alert or physician reports unable to join video

1. Confirm active provider: `GET /api/v1/config/video.provider`
2. Check provider status pages (Vonage, Azure ACS)
3. If automatic failover occurred, verify sessions on backup provider in monitoring snapshot
4. Manual switch if needed: `PUT /api/v1/config/video.provider` to alternate provider (admin role)
5. Notify physicians via `physician-provider-failover` notification template
6. Post-incident: review failover timing against SLO; update config if primary provider unstable

## RB-010 High identity failure rate

**Trigger:** `HighIdentityFailureRate` alert — validation failure rate > 15%

1. Check FONASA/vendor connectivity via integration-service health
2. Review identity-service logs for lockout patterns (`IdentityValidationFailed` events)
3. Verify simulator is **not** deployed in production (should be absent)
4. Check for brute-force attack: correlate with gateway rate-limit metrics
5. If vendor outage, enable maintenance banner in patient shell via config-service
6. Reset lockout for affected patients via identity-service admin API (compliance approval required)

## RB-011 Certificate expiring / consultation abandonment

**Trigger:** `CertificateExpiringSoon` or `ConsultationAbandonmentHigh` alert

### Certificate expiring

1. Identify certificate from alert label `{{ $labels.instance }}`
2. Renew via cert-manager: `kubectl describe certificate -n telemedicine`
3. Force ingress reload if renewal succeeded but probe still failing
4. Verify SSL probe green in Prometheus within 15 minutes

### Consultation abandonment high

1. Check video provider health (cross-reference RB-009)
2. Review waiting-room queue depth and average wait time in monitoring snapshot
3. Inspect notification-service delivery rates for physician-ready alerts
4. Check for network quality degradation events (`ConnectionQualityDegraded`)
5. Review recent deployments for regression; rollback if correlated (RB-006)
6. Escalate to product if abandonment persists after infrastructure checks
