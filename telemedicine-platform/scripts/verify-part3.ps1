#Requires -Version 5.1
<#
.SYNOPSIS
  Asserts Part 3 exit criteria: all ten domain services, event-producing APIs, BFF aggregation.
#>
param(
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$ConsulUrl = "http://localhost:8500",
    [switch]$SkipGradle
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

Write-Host "`n=== Part 3 verification (domain services) ===" -ForegroundColor Cyan

$domainServices = @(
    "waiting-room-service", "identity-service", "video-service", "telemetry-service",
    "clinical-docs-service", "notification-service", "integration-service",
    "survey-service", "reporting-service", "monitoring-service"
)

if (-not $SkipGradle) {
    Write-Host "`n--- Gradle check ---" -ForegroundColor Cyan
    $backendRoot = Join-Path $PSScriptRoot "..\..\telemedicine-backend"
    Push-Location $backendRoot
    try {
        $modules = @(
            ":integration-service:check", ":waiting-room-service:check", ":identity-service:check",
            ":notification-service:check", ":video-service:check", ":telemetry-service:check",
            ":clinical-docs-service:check", ":survey-service:check",
            ":reporting-service:check", ":monitoring-service:check",
            ":patient-bff:check", ":physician-bff:check", ":admin-bff:check",
            ":external-simulators:check"
        ) -join " "
        $gradleCmd = ".\gradlew.bat $modules --no-daemon"
        Write-Host "Running: $gradleCmd"
        Invoke-Expression $gradleCmd
        if ($LASTEXITCODE -ne 0) {
            Assert-Ok "Gradle domain + BFF checks" $false "exit code $LASTEXITCODE"
        } else {
            Assert-Ok "Gradle domain + BFF checks" $true
        }
    } catch {
        Assert-Ok "Gradle domain + BFF checks" $false $_.Exception.Message
    } finally {
        Pop-Location
    }
}

Write-Host "`n--- Compose / Consul ---" -ForegroundColor Cyan
foreach ($svc in $domainServices) {
    $status = docker compose ps $svc --format "{{.Status}}" 2>$null
    $healthy = $status -match "healthy|Up"
    Assert-Ok "Container $svc running" $healthy $status
}

try {
    $catalog = Invoke-RestMethod -Uri "$ConsulUrl/v1/catalog/services" -TimeoutSec 10
    foreach ($svc in $domainServices) {
        Assert-Ok "Consul catalog has $svc" ($catalog.PSObject.Properties.Name -contains $svc)
    }
} catch {
    Assert-Ok "Consul reachable" $false $_.Exception.Message
}

Write-Host "`n--- Journey API smoke ---" -ForegroundColor Cyan
$appointmentId = "apt-verify-part3"

try {
    $createBody = '{"patientId":"pat-verify","physicianId":"phy-verify"}'
    $created = Invoke-RestMethod -Method Post -Uri "$GatewayUrl/v1/appointments/v2" -ContentType "application/json" -Body $createBody -TimeoutSec 20
    $appointmentId = if ($created.data.id) { $created.data.id } else { $created.id }
    Assert-Ok "Integration create appointment" ($null -ne $appointmentId)
} catch {
    Assert-Ok "Integration create appointment" $false $_.Exception.Message
}

try {
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/v1/appointments/v2/$appointmentId/payment/validate" -TimeoutSec 20 | Out-Null
    Assert-Ok "Payment validate" $true
} catch {
    Assert-Ok "Payment validate" $false $_.Exception.Message
}

try {
    $linkBody = @{ appointmentId = $appointmentId; channel = "email" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/waiting-room/session-link" -ContentType "application/json" -Body $linkBody -TimeoutSec 20 | Out-Null
    Assert-Ok "Session link generation" $true
} catch {
    Assert-Ok "Session link generation" $false $_.Exception.Message
}

try {
    $queue = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/waiting-room/queue/$appointmentId" -TimeoutSec 20
    Assert-Ok "Queue position API" ($null -ne $queue.data)
} catch {
    Assert-Ok "Queue position API" $false $_.Exception.Message
}

try {
    $sessionBody = @{ appointmentId = $appointmentId } | ConvertTo-Json
    $video = Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/video/sessions" -ContentType "application/json" -Body $sessionBody -TimeoutSec 20
    $sessionId = if ($video.data.sessionId) { $video.data.sessionId } else { $video.data.id }
    Assert-Ok "Video session create" ($null -ne $sessionId)
} catch {
    Assert-Ok "Video session create" $false $_.Exception.Message
}

try {
    $telemetryBody = @{ appointmentId = $appointmentId } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/telemetry/token" -ContentType "application/json" -Body $telemetryBody -TimeoutSec 20 | Out-Null
    Assert-Ok "Telemetry token" $true
} catch {
    Assert-Ok "Telemetry token" $false $_.Exception.Message
}

try {
    $snapshot = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/monitoring/snapshot" -TimeoutSec 20
    $data = $snapshot.data
    Assert-Ok "Monitoring snapshot (non-null fields)" (
        $null -ne $data.activeSessions -and $null -ne $data.waitingRoomQueue -and $null -ne $data.errorRate
    )
} catch {
    Assert-Ok "Monitoring snapshot" $false $_.Exception.Message
}

try {
    $kpis = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/reporting/kpis" -TimeoutSec 20
    Assert-Ok "Reporting KPIs" ($null -ne $kpis.data)
} catch {
    Assert-Ok "Reporting KPIs" $false $_.Exception.Message
}

try {
    $journey = Invoke-RestMethod -Uri "$GatewayUrl/bff/patient/journey/$appointmentId" -TimeoutSec 20
    Assert-Ok "Patient BFF journey" ($null -ne $journey.data)
} catch {
    Assert-Ok "Patient BFF journey" $false $_.Exception.Message
}

try {
    $docs = Invoke-RestMethod -Uri "$GatewayUrl/v3/api-docs" -TimeoutSec 15
    Assert-Ok "Gateway OpenAPI docs" ($null -ne $docs.openapi)
} catch {
    Assert-Ok "Gateway OpenAPI docs" $false $_.Exception.Message
}

if ($failures.Count -gt 0) {
    Write-Host "`nPart 3 verification FAILED ($($failures.Count) checks):" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}

Write-Host "`nPart 3 verification PASSED" -ForegroundColor Green
exit 0
