param(
    [Parameter(Mandatory = $true)]
    [string]$Lang,
    [Parameter(Mandatory = $true)]
    [string]$Name
)

$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Slug = $Name.Replace("_", "-")

if ($Lang -eq "java") {
    $Source = Join-Path $Root "services\_template-java"
    $Target = Join-Path $Root "services\$Slug"
    Copy-Item -Recurse $Source $Target
    Get-ChildItem -Path $Target -Recurse -File | ForEach-Object {
        (Get-Content $_.FullName -Raw)
            .Replace("template-java", $Slug)
            .Replace("templatejava", ($Slug -replace "-", "")) | Set-Content $_.FullName -NoNewline
    }
    Write-Host "Created Java service at services/$Slug"
    Write-Host "Add to settings.gradle and gateway application.yml"
}
elseif ($Lang -eq "python") {
    $Source = Join-Path $Root "services\_template-python"
    $Target = Join-Path $Root "services\$Slug"
    Copy-Item -Recurse $Source $Target
    Get-ChildItem -Path $Target -Recurse -File | ForEach-Object {
        (Get-Content $_.FullName -Raw).Replace("template-python", $Slug) | Set-Content $_.FullName -NoNewline
    }
    Write-Host "Created Python service at services/$Slug"
    Write-Host "Add gateway route for lb://$Slug"
}
else {
    Write-Error "Unknown language: $Lang. Use java or python."
    exit 1
}
