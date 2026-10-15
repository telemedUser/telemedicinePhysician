#!/usr/bin/env pwsh
# Part 6 — Microfrontends verification
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root 'telemedicine-frontend'
Push-Location $frontend

try {
    Write-Host "==> 1. pnpm install"
    pnpm install
    if ($LASTEXITCODE -ne 0) { throw 'pnpm install failed' }

    Write-Host "==> 2. MFE and shell tests"
    $apps = @(
        'shell', 'physician-shell', 'admin-shell',
        'mfe-patient-consent', 'mfe-patient-identity', 'mfe-patient-waiting-room',
        'mfe-patient-consultation', 'mfe-patient-documents', 'mfe-patient-survey',
        'mfe-physician-consultation', 'mfe-physician-documentation', 'mfe-physician-patient-context',
        'mfe-admin-config', 'mfe-admin-dashboards', 'mfe-admin-audit',
        'mfe-admin-access', 'mfe-admin-monitoring'
    )
    foreach ($app in $apps) {
        $vitestConfig = Join-Path "apps/$app" 'vitest.config.ts'
        if (Test-Path $vitestConfig) {
            Write-Host "  Testing $app..."
            pnpm exec vitest run --config $vitestConfig
            if ($LASTEXITCODE -ne 0) { throw "Tests failed for $app" }
        } else {
            Write-Warning "No vitest.config.ts for $app — skipping tests"
        }
    }

    Write-Host "==> 3. Build all apps"
    pnpm nx run-many -t build --projects=apps/*
    if ($LASTEXITCODE -ne 0) { throw 'App build failed' }

    Write-Host "==> 4. Manifest roles + version"
    @(
        'apps/shell/public/config/mfe-manifest.json',
        'apps/physician-shell/public/config/mfe-manifest.json',
        'apps/admin-shell/public/config/mfe-manifest.json'
    ) | ForEach-Object {
        $manifest = Get-Content (Join-Path $frontend $_) -Raw | ConvertFrom-Json
        if (-not $manifest.shellVersion) { throw "$_ missing shellVersion" }
        foreach ($remote in $manifest.remotes) {
            if (-not $remote.roles) { throw "$($remote.name) missing roles in $_" }
            if (-not $remote.version) { throw "$($remote.name) missing version in $_" }
        }
    }

    Write-Host "==> 5. Workspace dependency check"
    Get-ChildItem (Join-Path $frontend 'apps') -Directory | ForEach-Object {
        $pkgJsonPath = Join-Path $_.FullName 'package.json'
        if (-not (Test-Path $pkgJsonPath)) { return }
        $pkgJson = Get-Content $pkgJsonPath -Raw | ConvertFrom-Json
        if ($pkgJson.dependencies) {
            $pkgJson.dependencies.PSObject.Properties | Where-Object { $_.Name -like '@telemedicine/*' } | ForEach-Object {
                if ($_.Value -ne 'workspace:*') {
                    Write-Warning "$($_.Name) in $($_.DirectoryName) should use workspace:*"
                }
            }
        }
    }

    Write-Host ""
    Write-Host "verify-part6: ALL CHECKS PASSED" -ForegroundColor Green
}
finally {
    Pop-Location
}
