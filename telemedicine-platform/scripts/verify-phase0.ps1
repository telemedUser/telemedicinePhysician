#Requires -Version 5.1
<#
.SYNOPSIS
  Asserts Phase 0 exit criteria for the Telemedicine Platform stack.
#>
param(
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$ConsulUrl = "http://localhost:8500",
    [string]$PrometheusUrl = "http://localhost:9090",
    [string]$KeycloakUrl = "http://localhost:8180",
    [string]$SimulatorsUrl = "http://localhost:8200",
    [string]$Realm = "telemedicine"
)

$ErrorActionPreference = "Stop"
$failures = @()

function Assert-Ok {
    param([string]$Name, [bool]$Condition, [string]$Detail = "")
    if (-not $Condition) {
        $msg = $Name
        if ($Detail) { $msg += " - $Detail" }
        $script:failures += $msg
        Write-Host "[FAIL] $msg" -ForegroundColor Red
    } else {
        Write-Host "[ OK ] $Name" -ForegroundColor Green
    }
}

Write-Host "`n=== Phase 0 verification ===" -ForegroundColor Cyan

# Docker container health
$containers = docker compose ps --format json 2>$null | ForEach-Object { $_ | ConvertFrom-Json }
if (-not $containers) {
    $containers = docker compose ps -a 2>$null
}

$requiredServices = @(
    "consul", "postgres", "redis", "kafka", "minio", "keycloak",
    "prometheus", "jaeger", "external-simulators", "gateway"
)

foreach ($svc in $requiredServices) {
    $status = docker compose ps $svc --format "{{.Status}}" 2>$null
    $healthy = $status -match "healthy|Up"
    Assert-Ok "Container $svc running" $healthy $status
}

# Consul catalog
try {
    $catalog = Invoke-RestMethod -Uri "$ConsulUrl/v1/catalog/services" -TimeoutSec 10
    Assert-Ok "Consul catalog has gateway" ($catalog.PSObject.Properties.Name -contains "gateway")
    Assert-Ok "Consul catalog has external-simulators" ($catalog.PSObject.Properties.Name -contains "external-simulators")
} catch {
    Assert-Ok "Consul reachable" $false $_.Exception.Message
}

# Gateway health
try {
    $gwHealth = Invoke-RestMethod -Uri "$GatewayUrl/actuator/health" -TimeoutSec 15
    Assert-Ok "Gateway /actuator/health UP" ($gwHealth.status -eq "UP")
} catch {
    Assert-Ok "Gateway /actuator/health UP" $false $_.Exception.Message
}

# Simulators via gateway (exit criterion)
try {
    $his = Invoke-RestMethod -Uri "$GatewayUrl/sim/his/appointments/apt-test-001" -TimeoutSec 15
    Assert-Ok "Gateway /sim/his/appointments/{id} returns 200" ($his.appointmentId -eq "apt-test-001")
} catch {
    Assert-Ok "Gateway /sim/his/appointments/{id} returns 200" $false $_.Exception.Message
}

# Simulators direct
try {
    $simHealth = Invoke-RestMethod -Uri "$SimulatorsUrl/actuator/health" -TimeoutSec 10
    Assert-Ok "Simulators /actuator/health UP" ($simHealth.status -eq "UP")
} catch {
    Assert-Ok "Simulators /actuator/health UP" $false $_.Exception.Message
}

# Keycloak realm
try {
    Invoke-RestMethod -Uri "$KeycloakUrl/realms/$Realm/.well-known/openid-configuration" -TimeoutSec 15 | Out-Null
    Assert-Ok "Keycloak realm '$Realm' OIDC discovery" $true
} catch {
    Assert-Ok "Keycloak realm '$Realm' OIDC discovery" $false $_.Exception.Message
}

# Prometheus
try {
    $promHealth = Invoke-RestMethod -Uri "$PrometheusUrl/-/healthy" -TimeoutSec 10
    Assert-Ok "Prometheus healthy" ($promHealth.ToString().Trim() -eq "Prometheus Server is Healthy.")
} catch {
    Assert-Ok "Prometheus healthy" $false $_.Exception.Message
}

try {
    $targets = Invoke-RestMethod -Uri "$PrometheusUrl/api/v1/targets" -TimeoutSec 10
    $upCount = ($targets.data.activeTargets | Where-Object { $_.health -eq "up" }).Count
    Assert-Ok "Prometheus has UP targets" ($upCount -gt 0) "$upCount target(s) up"
} catch {
    Assert-Ok "Prometheus has UP targets" $false $_.Exception.Message
}

# Kafka topics (via kafka-init container logs)
$kafkaInit = docker compose logs kafka-init 2>$null | Select-String "Kafka topic bootstrap complete"
Assert-Ok "Kafka topics bootstrapped" ($null -ne $kafkaInit)

# MinIO buckets (via minio-init container logs)
$minioInit = docker compose logs minio-init 2>$null | Select-String "MinIO bucket bootstrap complete"
Assert-Ok "MinIO buckets bootstrapped" ($null -ne $minioInit)

Write-Host ""
if ($failures.Count -gt 0) {
    Write-Host "Phase 0 verification FAILED ($($failures.Count) issue(s)):" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}

Write-Host "Phase 0 verification PASSED" -ForegroundColor Green
exit 0
