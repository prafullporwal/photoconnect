# =============================================================================
# generate-keys.ps1 — one-time RSA key pair generation for JWT signing (Windows)
# =============================================================================
# Run ONCE per dev environment. Keys land in auth-service/keys/ which is
# gitignored.
# =============================================================================
param(
    [string]$KeysDir = "auth-service/keys"
)

$ErrorActionPreference = 'Stop'

New-Item -ItemType Directory -Force -Path $KeysDir | Out-Null
$Private = Join-Path $KeysDir "private_key.pem"
$Public  = Join-Path $KeysDir "public_key.pem"

if ((Test-Path $Private) -and (Test-Path $Public)) {
    Write-Host "Keys already exist at $KeysDir. Delete them first if you want to rotate."
    exit 0
}

if (-not (Get-Command openssl -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: openssl is not on PATH." -ForegroundColor Red
    Write-Host "Easiest fix on Windows: install Git for Windows (ships openssl)"
    Write-Host "  winget install --id Git.Git"
    Write-Host "Or directly:"
    Write-Host "  choco install openssl"
    exit 1
}

& openssl genpkey -algorithm RSA -out $Private -pkeyopt rsa_keygen_bits:2048
& openssl rsa -in $Private -pubout -out $Public

Write-Host "Generated:" -ForegroundColor Green
Write-Host "  $Private  (keep secret)"
Write-Host "  $Public   (safe to distribute)"
