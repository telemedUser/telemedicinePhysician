#!/usr/bin/env pwsh
# Part 7 — Hardening and go-live readiness verification (steps 7.0–7.11)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$platformRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $platformRoot
$backendRoot = Join-Path $repoRoot 'telemedicine-backend'
$frontendRoot = Join-Path $repoRoot 'telemedicine-frontend'
$failures = @()

function Assert-File {
    param([string]$Name, [string]$Path, [string]$Pattern = '')
    if (-not (Test-Path $Path)) {
        $script:failures += "$Name - file not found: $Path"
        Write-Host "[FAIL] $Name" -ForegroundColor Red
        return
    }
    if ($Pattern) {
        $content = Get-Content $Path -Raw
        if ($content -notmatch $Pattern) {
            $script:failures += "$Name - pattern not found in $Path"
            Write-Host "[FAIL] $Name" -ForegroundColor Red
            return
        }
    }
    Write-Host "[ OK ] $Name" -ForegroundColor Green
}

Write-Host "==> Part 7 verification (full checklist)" -ForegroundColor Cyan

# 7.0 Event consumers
$consumerPatterns = @(
    @{ Name = 'ConfigChangeListener (video)'; Path = 'services/video-service/src/main/java/com/telemedicine/videoservice/kafka/ConfigChangeListener.java'; Pattern = 'ConfigurationChanged' },
    @{ Name = 'ConfigChangeListener (telemetry)'; Path = 'services/telemetry-service/src/main/java/com/telemedicine/telemetryservice/kafka/ConfigChangeListener.java'; Pattern = 'ConfigurationChanged' },
    @{ Name = 'ConfigChangeListener (notification)'; Path = 'services/notification-service/src/main/java/com/telemedicine/notificationservice/kafka/ConfigChangeListener.java'; Pattern = 'ConfigurationChanged' },
    @{ Name = 'AppointmentEventListener'; Path = 'services/waiting-room-service/src/main/java/com/telemedicine/waitingroomservice/kafka/AppointmentEventListener.java'; Pattern = 'AppointmentCreated' },
    @{ Name = 'ChatMessageSent consumer'; Path = 'platform/audit-service/src/main/java/com/telemedicine/auditservice/kafka/AuditDomainEventListener.java'; Pattern = 'CHAT_MESSAGE_SENT' },
    @{ Name = 'DeviceTestCompleted consumer'; Path = 'services/monitoring-service/src/main/java/com/telemedicine/monitoringservice/kafka/MonitoringDomainEventListener.java'; Pattern = 'DEVICE_TEST_COMPLETED' },
    @{ Name = 'PrescriptionIssued consumer'; Path = 'services/reporting-service/src/main/java/com/telemedicine/reportingservice/kafka/ReportingDomainEventListener.java'; Pattern = 'PRESCRIPTION_ISSUED' }
)
foreach ($c in $consumerPatterns) {
    Assert-File $c.Name (Join-Path $backendRoot $c.Path) $c.Pattern
}
Assert-File 'Flyway V4 ensure consumer tables' (Join-Path $backendRoot 'shared/java-common-persistence/src/main/resources/db/migration/common/V4__ensure_consumer_tables.sql') 'processed_events'
$dupV2 = Join-Path $backendRoot 'shared/java-common-persistence/src/main/resources/db/migration/common/V2__processed_events.sql'
if (Test-Path $dupV2) {
    $failures += 'Duplicate Flyway V2__processed_events.sql still exists'
    Write-Host '[FAIL] Flyway V2 duplicate removed' -ForegroundColor Red
} else {
    Write-Host '[ OK ] Flyway V2 duplicate removed' -ForegroundColor Green
}

# 7.1 JSON Schemas (28)
$schemasDir = Join-Path $backendRoot 'shared/contracts/events/schemas'
$schemaCount = (Get-ChildItem $schemasDir -Filter '*.v1.json' -ErrorAction SilentlyContinue).Count
if ($schemaCount -ge 28) {
    Write-Host "[ OK ] 7.1 event JSON schemas ($schemaCount files)" -ForegroundColor Green
} else {
    $failures += "7.1 - expected 28 schemas, found $schemaCount"
    Write-Host "[FAIL] 7.1 event JSON schemas ($schemaCount/28)" -ForegroundColor Red
}
Assert-File '7.1 schema contract test' (Join-Path $backendRoot 'shared/java-common-events/src/test/java/com/telemedicine/common/events/EventCatalogueContractTest.java') 'payloadSchemasMatchRecords'

# 7.2 Backend Dockerfiles
Assert-File '7.2 Dockerfile.spring-service' (Join-Path $backendRoot 'docker/Dockerfile.spring-service') 'GRADLE_MODULE'
$backendServices = @(
    'platform/config-service', 'platform/audit-service', 'platform/scheduler-service',
    'bff/patient-bff', 'bff/physician-bff', 'bff/admin-bff',
    'services/waiting-room-service', 'services/identity-service', 'services/video-service',
    'services/telemetry-service', 'services/clinical-docs-service', 'services/notification-service',
    'services/integration-service', 'services/survey-service', 'services/reporting-service', 'services/monitoring-service'
)
foreach ($svc in $backendServices) {
    Assert-File "7.2 Dockerfile $svc" (Join-Path $backendRoot "$svc/Dockerfile") 'GRADLE_MODULE|Dockerfile.spring-service'
}

# 7.3 Frontend Dockerfiles
Assert-File '7.3 Dockerfile.mfe' (Join-Path $frontendRoot 'docker/Dockerfile.mfe') 'NX_APP'
$frontendApps = @(
    'physician-shell', 'admin-shell',
    'mfe-patient-consent', 'mfe-patient-identity', 'mfe-patient-waiting-room',
    'mfe-patient-consultation', 'mfe-patient-documents', 'mfe-patient-survey',
    'mfe-physician-consultation', 'mfe-physician-documentation', 'mfe-physician-patient-context',
    'mfe-admin-config', 'mfe-admin-dashboards', 'mfe-admin-audit', 'mfe-admin-access', 'mfe-admin-monitoring'
)
foreach ($app in $frontendApps) {
    Assert-File "7.3 Dockerfile $app" (Join-Path $frontendRoot "apps/$app/Dockerfile") 'nginx'
}

# 7.4 K8s manifests and overlays
Assert-File '7.4 backend kustomization' (Join-Path $backendRoot 'deploy/kubernetes/base/kustomization.yaml') 'config-service'
Assert-File '7.4 frontend kustomization' (Join-Path $frontendRoot 'deploy/kubernetes/base/kustomization.yaml') 'physician-shell'
Assert-File '7.4 overlay dev' (Join-Path $platformRoot 'kubernetes/overlays/dev/kustomization.yaml') 'resources:'
Assert-File '7.4 overlay staging' (Join-Path $platformRoot 'kubernetes/overlays/staging/kustomization.yaml') 'resources:'
Assert-File '7.4 overlay prod' (Join-Path $platformRoot 'kubernetes/overlays/prod/kustomization.yaml') 'resources:'
Assert-File '7.4 ingress shells' (Join-Path $platformRoot 'kubernetes/ingress/ingress.yaml') 'physician-shell'

# 7.5 HA/DR
foreach ($ha in @('bff-ha.yaml', 'domain-ha.yaml', 'kafka-ha.yaml', 'postgres-ha.yaml', 'redis-ha.yaml')) {
    Assert-File "7.5 HA $ha" (Join-Path $platformRoot "kubernetes/ha/$ha") ''
}
Assert-File '7.5 DR exercise' (Join-Path $platformRoot 'kubernetes/dr/dr-exercise.yaml') 'CronJob'
Assert-File '7.5 backup cronjob' (Join-Path $platformRoot 'kubernetes/dr/backup-cronjob.yaml') 'pg_dump'

# 7.6 CI/CD
foreach ($wf in @('backend-ci.yml', 'frontend-ci.yml', 'deploy-staging.yml', 'deploy-prod.yml', 'performance-test.yml')) {
    Assert-File "7.6 workflow $wf" (Join-Path $repoRoot ".github/workflows/$wf") ''
}
Assert-File '7.6 backend coverage gate' (Join-Path $repoRoot '.github/workflows/backend-ci.yml') 'check|jacoco|80'
Assert-File '7.6 frontend coverage gate' (Join-Path $repoRoot '.github/workflows/frontend-ci.yml') '80|coverage'

# 7.7 Security
Assert-File '7.7 security-review.md' (Join-Path $platformRoot 'docs/security-review.md') 'Identity'
$netPol = Join-Path $platformRoot 'kubernetes/security/network-policies.yaml'
Assert-File '7.7 network-policies.yaml' $netPol 'gateway-ingress-egress'
Assert-File '7.7 realm-telemedicine-prod.json' (Join-Path $platformRoot 'infra/keycloak/realm-telemedicine-prod.json') 'bruteForceProtected'
Assert-File '7.7 application-prod-kafka.yml' (Join-Path $platformRoot 'infra/kafka/application-prod-kafka.yml') 'kafka'

# 7.8 k6
Assert-File '7.8 patient-journey pre-consultation' (Join-Path $platformRoot 'tests/k6/patient-journey.js') 'pre-consultation'
foreach ($script in @('gateway-load.js', 'bff-load.js', 'domain-services.js', 'event-throughput.js')) {
    Assert-File "7.8 k6 $script" (Join-Path $platformRoot "tests/k6/$script") 'p\(95\)<300'
}

# 7.9 Prometheus
$alerts = Join-Path $platformRoot 'infra/prometheus/rules/platform-alerts.yml'
foreach ($alert in @('OutboxPublishStalled', 'DLQMessagesAccumulating', 'VideoProviderFailover', 'HighIdentityFailureRate', 'ConsultationAbandonmentHigh', 'CertificateExpiringSoon', 'SLOBurnRateCritical')) {
    Assert-File "7.9 alert $alert" $alerts $alert
}

# 7.10 Docs
$tdd = Join-Path $platformRoot 'docs/technical-design-document.md'
foreach ($section in @('Production Kubernetes', 'CI/CD', 'HA/DR')) {
    Assert-File "7.10 TDD $section" $tdd $section
}
foreach ($rb in @('RB-007', 'RB-008', 'RB-009', 'RB-010', 'RB-011')) {
    Assert-File "7.10 runbook $rb" (Join-Path $platformRoot 'docs/runbooks.md') $rb
}
Assert-File '7.10 traceability.json' (Join-Path $platformRoot 'docs/traceability.json') 'NFR'
Assert-File '7.10 production-readiness.md' (Join-Path $platformRoot 'docs/production-readiness.md') 'verify-part7'

# 7.11 kustomization security
Assert-File '7.11 kustomization network-policies' (Join-Path $platformRoot 'kubernetes/kustomization.yaml') 'security/network-policies.yaml'

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "verify-part7: FAILED ($($failures.Count) checks):" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}

Write-Host ""
Write-Host "verify-part7: ALL CHECKS PASSED" -ForegroundColor Green
exit 0
