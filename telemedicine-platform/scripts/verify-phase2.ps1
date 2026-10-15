#Requires -Version 5.1
<#
.SYNOPSIS
  Asserts Phase 2 exit criteria: platform services, gateway hardening, BFF aggregation.
#>
param(
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$ConsulUrl = "http://localhost:8500"
)

$ErrorActionPreference = "Stop"
$failures = @()

function Assert-Ok {
    param([string]$Name, [bool]$Condition, [string]$Detail = "")
    if (-not $Condition) {
        $msg = $Name
        if ($Detail) { $msg += " — $Detail" }
        $script:failures += $msg
        Write-Host "[FAIL] $msg" -ForegroundColor Red
    } else {
        Write-Host "[ OK ] $Name" -ForegroundColor Green
    }
}

Write-Host "`n=== Phase 2 verification ===" -ForegroundColor Cyan

$platformServices = @(
    "config-service", "audit-service", "scheduler-service",
    "patient-bff", "physician-bff", "admin-bff", "gateway"
)

foreach ($svc in $platformServices) {
    $status = docker compose ps $svc --format "{{.Status}}" 2>$null
    $healthy = $status -match "healthy|Up"
    Assert-Ok "Container $svc running" $healthy $status
}

try {
    $catalog = Invoke-RestMethod -Uri "$ConsulUrl/v1/catalog/services" -TimeoutSec 10
    foreach ($svc in $platformServices) {
        Assert-Ok "Consul catalog has $svc" ($catalog.PSObject.Properties.Name -contains $svc)
    }
} catch {
    Assert-Ok "Consul reachable" $false $_.Exception.Message
}

# Config CRUD via gateway (dev profile — no auth)
try {
    $headers = @{ "Content-Type" = "application/json"; "X-Idempotency-Key" = [guid]::NewGuid().ToString() }
    $body = '{"value":"simulator"}'
    Invoke-RestMethod -Method Put -Uri "$GatewayUrl/api/v1/config/video.provider" -Headers $headers -Body $body -TimeoutSec 15 | Out-Null
    $config = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/config/video.provider" -TimeoutSec 15
    $value = if ($config.data) { $config.data.value } else { $config.value }
    Assert-Ok "Config PUT/GET video.provider" ($value -eq "simulator")
} catch {
    Assert-Ok "Config PUT/GET video.provider" $false $_.Exception.Message
}

# Audit ingest + query
try {
    $auditBody = @{
        actorId = "svc-test"
        actorRole = "SERVICE"
        action = "VERIFY_PHASE2"
        resourceType = "test"
        resourceId = "phase2"
    } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/audit" -ContentType "application/json" -Body $auditBody -TimeoutSec 15 | Out-Null
    $audit = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/audit?action=VERIFY_PHASE2&size=1" -TimeoutSec 15
    $count = if ($audit.data.content) { $audit.data.content.Count } elseif ($audit.data) { $audit.data.Count } else { 0 }
    Assert-Ok "Audit POST/GET" ($count -ge 1)
} catch {
    Assert-Ok "Audit POST/GET" $false $_.Exception.Message
}

# Chain verify
try {
    $chain = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/audit/chain/verify" -TimeoutSec 15
    $valid = if ($chain.data.valid -ne $null) { $chain.data.valid } else { $chain.valid }
    Assert-Ok "Audit chain verify" ($valid -eq $true)
} catch {
    Assert-Ok "Audit chain verify" $false $_.Exception.Message
}

# Scheduler catalog
try {
    $jobs = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/scheduler/jobs" -TimeoutSec 15
    $list = if ($jobs.data) { $jobs.data } else { $jobs }
    Assert-Ok "Scheduler job catalog" ($list.Count -ge 8)
} catch {
    Assert-Ok "Scheduler job catalog" $false $_.Exception.Message
}

# Admin BFF config facade
try {
    $adminConfig = Invoke-RestMethod -Uri "$GatewayUrl/bff/admin/config" -TimeoutSec 15
    Assert-Ok "Admin BFF config" ($null -ne $adminConfig.data)
} catch {
    Assert-Ok "Admin BFF config" $false $_.Exception.Message
}

# Correlation header echo
try {
    $cid = [guid]::NewGuid().ToString()
    $response = Invoke-WebRequest -Uri "$GatewayUrl/actuator/health" -Headers @{ "X-Correlation-Id" = $cid } -TimeoutSec 10 -UseBasicParsing
    Assert-Ok "Gateway echoes X-Correlation-Id" ($response.Headers["X-Correlation-Id"] -eq $cid)
} catch {
    Assert-Ok "Gateway echoes X-Correlation-Id" $false $_.Exception.Message
}

# OpenAPI on gateway
try {
    $docs = Invoke-RestMethod -Uri "$GatewayUrl/v3/api-docs" -TimeoutSec 15
    Assert-Ok "Gateway OpenAPI docs" ($null -ne $docs.openapi)
} catch {
    Assert-Ok "Gateway OpenAPI docs" $false $_.Exception.Message
}

if ($failures.Count -gt 0) {
    Write-Host "`nPhase 2 verification FAILED ($($failures.Count) checks):" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}

Write-Host "`nPhase 2 verification PASSED" -ForegroundColor Green
exit 0
