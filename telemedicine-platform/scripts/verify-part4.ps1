#Requires -Version 5.1
<#
.SYNOPSIS
  Asserts Part 4 exit criteria: hardened external-simulators with admin API, latency/errors, stateful flows.
#>
param(
    [string]$SimulatorUrl = "http://localhost:8200",
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

Write-Host "`n=== Part 4 verification (external simulators) ===" -ForegroundColor Cyan

if (-not $SkipGradle) {
    Write-Host "`n--- Gradle check ---" -ForegroundColor Cyan
    $backendRoot = Join-Path $PSScriptRoot "..\..\telemedicine-backend"
    Push-Location $backendRoot
    try {
        & .\gradlew.bat :external-simulators:check :java-common-testing:test --tests "*SimulatorStubs*" --no-daemon
        Assert-Ok "Gradle external-simulators + stubs tests" ($LASTEXITCODE -eq 0) "exit code $LASTEXITCODE"
    } catch {
        Assert-Ok "Gradle external-simulators + stubs tests" $false $_.Exception.Message
    } finally {
        Pop-Location
    }
}

Write-Host "`n--- Simulator health ---" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$SimulatorUrl/actuator/health" -TimeoutSec 10
    Assert-Ok "Simulator actuator UP" ($health.status -eq "UP")
} catch {
    Assert-Ok "Simulator reachable on $SimulatorUrl" $false $_.Exception.Message
}

Write-Host "`n--- Admin API ---" -ForegroundColor Cyan
try {
    $vendors = Invoke-RestMethod -Uri "$SimulatorUrl/sim/admin/vendors" -TimeoutSec 10
    Assert-Ok "Admin lists 12 vendors" ($vendors.PSObject.Properties.Count -ge 12)
} catch {
    Assert-Ok "GET /sim/admin/vendors" $false $_.Exception.Message
}

try {
    $body = @{ latencyMs = 200; latencyJitterMs = 0; errorRate = 0.0; timeoutRate = 0.0; scenario = "success"; enabled = $true } | ConvertTo-Json
    Invoke-RestMethod -Method Put -Uri "$SimulatorUrl/sim/admin/vendors/his" -Body $body -ContentType "application/json" -TimeoutSec 10 | Out-Null
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-RestMethod -Uri "$SimulatorUrl/sim/his/appointments/test-latency-001" -TimeoutSec 30 | Out-Null
    $sw.Stop()
    Assert-Ok "HIS latency >= 200ms" ($sw.ElapsedMilliseconds -ge 200) "$($sw.ElapsedMilliseconds)ms"
} catch {
    Assert-Ok "Latency injection" $false $_.Exception.Message
}

try {
    $body = @{ latencyMs = 0; errorRate = 1.0; enabled = $true } | ConvertTo-Json
    Invoke-RestMethod -Method Put -Uri "$SimulatorUrl/sim/admin/vendors/payment" -Body $body -ContentType "application/json" -TimeoutSec 10 | Out-Null
    try {
        Invoke-RestMethod -Uri "$SimulatorUrl/sim/payment/validate/test-error-001" -TimeoutSec 10 | Out-Null
        Assert-Ok "Payment errorRate 1.0 returns 503" $false "expected failure"
    } catch {
        Assert-Ok "Payment errorRate 1.0 returns 503" $true
    }
} catch {
    Assert-Ok "Payment error rate" $false $_.Exception.Message
}

Write-Host "`n--- Stateful flows ---" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Method Post -Uri "$SimulatorUrl/sim/admin/scenarios/happy" -TimeoutSec 10 | Out-Null
    $challenge = Invoke-RestMethod -Method Post -Uri "$SimulatorUrl/sim/fonasa/2fa/challenge" -Body '{"patientId":"patient-verify"}' -ContentType "application/json" -TimeoutSec 10
    $verifyBody = @{ patientId = "patient-verify"; challengeId = $challenge.challengeId; code = "123456" } | ConvertTo-Json
    $verify = Invoke-RestMethod -Method Post -Uri "$SimulatorUrl/sim/fonasa/2fa/verify" -Body $verifyBody -ContentType "application/json" -TimeoutSec 10
    Assert-Ok "FONASA verify success" ($verify.verified -eq $true)
} catch {
    Assert-Ok "FONASA challenge/verify" $false $_.Exception.Message
}

Write-Host "`n--- Provider failover ---" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Method Post -Uri "$SimulatorUrl/sim/vonage/outage" -Body '{"enabled":true}' -ContentType "application/json" -TimeoutSec 10 | Out-Null
    $vonageFailed = $false
    try {
        Invoke-RestMethod -Method Post -Uri "$SimulatorUrl/sim/vonage/session" -Body '{"appointmentId":"apt-outage"}' -ContentType "application/json" -TimeoutSec 10 | Out-Null
    } catch { $vonageFailed = $true }
    $acs = Invoke-RestMethod -Method Post -Uri "$SimulatorUrl/sim/acs/session" -Body '{"appointmentId":"apt-outage"}' -ContentType "application/json" -TimeoutSec 10
    Assert-Ok "Vonage outage fails" $vonageFailed
    Assert-Ok "ACS still works" ($acs.provider -eq "acs")
    Invoke-RestMethod -Method Post -Uri "$SimulatorUrl/sim/vonage/outage" -Body '{"enabled":false}' -ContentType "application/json" -TimeoutSec 10 | Out-Null
} catch {
    Assert-Ok "Provider failover" $false $_.Exception.Message
}

Write-Host "`n--- OpenAPI ---" -ForegroundColor Cyan
try {
    $docs = Invoke-RestMethod -Uri "$SimulatorUrl/sim/v3/api-docs" -TimeoutSec 10
    Assert-Ok "Springdoc OpenAPI available" ($null -ne $docs.openapi)
} catch {
    Assert-Ok "GET /sim/v3/api-docs" $false $_.Exception.Message
}

Write-Host "`n--- Compose profile exclusion ---" -ForegroundColor Cyan
try {
    Push-Location (Join-Path $PSScriptRoot "..")
    $config = docker compose --profile infra --profile edge --profile platform --profile domain config --services 2>$null
    Assert-Ok "Simulator absent from non-sim profiles" ($config -notcontains "external-simulators")
    Pop-Location
} catch {
    Assert-Ok "Compose profile check" $false $_.Exception.Message
}

Write-Host "`n=== Summary ===" -ForegroundColor Cyan
if ($failures.Count -gt 0) {
    Write-Host "FAILED ($($failures.Count) checks):" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}
Write-Host "All Part 4 checks passed." -ForegroundColor Green
exit 0
