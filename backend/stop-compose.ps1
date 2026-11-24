# Stops the containers. Data stays in the named volume; add -Reset to delete the database.
param(
    [switch]$Reset
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ComposePath = Join-Path $PSScriptRoot "docker-compose.yml"
$EnvPath = Join-Path $ProjectRoot ".env"

if ($Reset) {
    & docker compose --env-file $EnvPath -f $ComposePath down -v
} else {
    & docker compose --env-file $EnvPath -f $ComposePath down
}
exit $LASTEXITCODE
