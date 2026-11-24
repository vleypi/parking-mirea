# Creates .env in the project root with random DB password, JWT secret and temporary admin password.
param(
    [string]$AdminLogin = "admin",
    [int]$AppPort = 8080,
    [int]$PostgresPort = 55432,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$EnvPath = Join-Path $ProjectRoot ".env"

if ((Test-Path $EnvPath) -and -not $Force) {
    throw ".env already exists: $EnvPath. Use -Force to replace it."
}

function New-RandomBytes([int]$Length) {
    $bytes = New-Object byte[] $Length
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return $bytes
}

function New-Hex([int]$Length) {
    return (-join ((New-RandomBytes $Length) | ForEach-Object { $_.ToString("x2") }))
}

$DbPassword = New-Hex 24
$JwtSecret = [Convert]::ToBase64String((New-RandomBytes 64))
$AdminPassword = "Adm1n" + (New-Hex 8)

$content = @"
POSTGRES_DB=parking_db
POSTGRES_USER=parking_app
POSTGRES_PASSWORD=$DbPassword
POSTGRES_PORT=$PostgresPort
JWT_SECRET_BASE64=$JwtSecret
JWT_ISSUER=parking
ACCESS_TOKEN_MINUTES=15
REFRESH_TOKEN_DAYS=14
BOOTSTRAP_ADMIN_LOGIN=$AdminLogin
BOOTSTRAP_ADMIN_NAME=Administrator
BOOTSTRAP_ADMIN_PASSWORD=$AdminPassword
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:$AppPort
COOKIE_SECURE=false
PERMANENT_DELETE_ENABLED=false
APP_PORT=$AppPort
"@

[IO.File]::WriteAllText($EnvPath, $content, (New-Object Text.UTF8Encoding($false)))

Write-Host ""
Write-Host "Created: $EnvPath" -ForegroundColor Green
Write-Host "Admin login:    $AdminLogin" -ForegroundColor Cyan
Write-Host "Admin password: $AdminPassword" -ForegroundColor Yellow
Write-Host "Do not commit or send the .env file to anyone." -ForegroundColor Red
