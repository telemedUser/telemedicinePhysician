$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

$checks = @(
    "telemedicine-backend\gateway\src\main\resources\application.yml",
    "telemedicine-backend\services\_template-java\Dockerfile",
    "telemedicine-backend\services\_template-python\requirements.txt",
    "telemedicine-frontend\apps\shell\vite.config.ts",
    "telemedicine-frontend\apps\_template-mfe\vite.config.ts",
    "telemedicine-frontend\packages\mfe-registry\src\index.ts",
    "telemedicine-platform\docker-compose.full-stack.yml",
    "telemedicine-platform\docs\architecture.md"
)

$pass = 0
$fail = 0
foreach ($path in $checks) {
    $full = Join-Path $Root $path
    if (Test-Path $full) {
        Write-Host "OK  $path"
        $pass++
    } else {
        Write-Host "MISSING  $path"
        $fail++
    }
}

Write-Host ""
Write-Host "Passed: $pass  Failed: $fail"
if ($fail -gt 0) { exit 1 }
Write-Host "Structure verification passed."
