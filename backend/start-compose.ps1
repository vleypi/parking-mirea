# Starts PostgreSQL and the Spring Boot backend from backend/docker-compose.yml using .env from the project root.
param(
    [int]$WaitTimeoutSeconds = 600
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ComposePath = Join-Path $PSScriptRoot "docker-compose.yml"
$EnvPath = Join-Path $ProjectRoot ".env"

if (-not (Test-Path $EnvPath)) {
    Write-Host ".env not found, generating it..." -ForegroundColor Yellow
    & (Join-Path $PSScriptRoot "init-env.ps1")
}

Write-Host "Building and starting containers (database volume is preserved)..." -ForegroundColor Cyan
& docker compose --env-file $EnvPath -f $ComposePath up -d --build --wait --wait-timeout $WaitTimeoutSeconds

if ($LASTEXITCODE -ne 0) {
    Write-Host "Startup failed. Backend logs:" -ForegroundColor Red
    & docker compose --env-file $EnvPath -f $ComposePath logs --tail 150 backend
    exit $LASTEXITCODE
}

$values = @{}
foreach ($line in Get-Content -LiteralPath $EnvPath) {
    $t = $line.Trim()
    if (-not $t -or $t.StartsWith("#")) { continue }
    $eq = $t.IndexOf("=")
    if ($eq -gt 0) { $values[$t.Substring(0, $eq).Trim()] = $t.Substring($eq + 1).Trim() }
}
$appPort = if ($values["APP_PORT"]) { $values["APP_PORT"] } else { "8080" }
$pgPort = if ($values["POSTGRES_PORT"]) { $values["POSTGRES_PORT"] } else { "55432" }

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " PARKING BACKEND IS READY" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host " Swagger UI:   http://localhost:$appPort/swagger-ui.html"
Write-Host " REST API:     http://localhost:$appPort/api"
Write-Host " OpenAPI JSON: http://localhost:$appPort/v3/api-docs"
Write-Host " Health:       http://localhost:$appPort/actuator/health"
Write-Host " PostgreSQL:   jdbc:postgresql://localhost:$pgPort/$($values['POSTGRES_DB'])"
Write-Host " Admin login:  $($values['BOOTSTRAP_ADMIN_LOGIN']) (password in .env)"
Write-Host " Web client:   cd frontend; pnpm dev -> http://localhost:3000"
Write-Host "============================================================" -ForegroundColor Green

& docker compose --env-file $EnvPath -f $ComposePath ps
