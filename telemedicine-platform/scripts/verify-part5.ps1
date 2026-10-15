#!/usr/bin/env pwsh
# Part 5 — Reusable Frontend Packages verification
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root 'telemedicine-frontend'
Push-Location $frontend

try {
    Write-Host "==> 1. pnpm install"
    pnpm install
    if ($LASTEXITCODE -ne 0) { throw "pnpm install failed" }

    Write-Host "==> 2. Lock file exists"
    if (-not (Test-Path 'pnpm-lock.yaml')) { throw 'pnpm-lock.yaml missing' }

    Write-Host "==> 3. Package tests"
    $packages = @(
        'shared-types', 'design-system', 'api-client', 'auth', 'session-context',
        'mfe-registry', 'feature-flags', 'video-adapter', 'chat-adapter',
        'telemetry-adapter', 'consent-kit', 'device-test', 'i18n',
        'observability-web', 'test-utils'
    )
    foreach ($pkg in $packages) {
        Write-Host "  Testing $pkg..."
        Push-Location "packages/$pkg"
        pnpm exec vitest run
        if ($LASTEXITCODE -ne 0) { throw "Tests failed for $pkg" }
        Pop-Location
    }

    Write-Host "==> 4. Storybook build"
    Push-Location packages/design-system
    pnpm exec storybook build --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Storybook build failed' }
    Pop-Location

    Write-Host "==> 5. Workspace dependency check"
    $appsDir = Join-Path $frontend 'apps'
    Get-ChildItem $appsDir -Directory | ForEach-Object {
        $pkgJson = Get-Content (Join-Path $_.FullName 'package.json') -Raw | ConvertFrom-Json
        if ($pkgJson.dependencies) {
            $pkgJson.dependencies.PSObject.Properties | Where-Object { $_.Name -like '@telemedicine/*' } | ForEach-Object {
                if ($_.Value -ne 'workspace:*') {
                    Write-Warning "$($_.Name) in $($_.DirectoryName) should use workspace:*"
                }
            }
        }
    }

    Write-Host ""
    Write-Host "verify-part5: ALL CHECKS PASSED" -ForegroundColor Green
}
finally {
    Pop-Location
}
