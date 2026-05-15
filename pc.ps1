# =============================================================================
# PhotoConnect PowerShell helper (Windows-friendly equivalent of the Makefile)
# =============================================================================
# Usage:   .\pc.ps1 <command>
# Example: .\pc.ps1 up-all
#
# The big one is `up-all` - it boots the entire stack in one go:
#   1. Docker infrastructure (Postgres, MySQL, Redis, MinIO, Zipkin)
#   2. discovery-service (Eureka)        :8761
#   3. config-service                    :8888
#   4. auth-service                      :8081
#   5. photographer-service              :8082
#   6. customer-service                  :8083
#   7. api-gateway                       :8080
#   8. frontend (Vite dev server)        :5173
#
# Each backend service opens in its OWN console window so you can tail its
# logs independently. Closing a window stops that service.
#
# `down-all` kills processes on every PhotoConnect port and brings Docker down.
# =============================================================================

param(
    [Parameter(Position=0)]
    [ValidateSet(
        'help','up','down','down-clean','restart','logs','ps','build','test',
        'discovery-run','config-run','gateway-run','auth-run','photographer-run','customer-run',
        'frontend-run','auth-keys',
        'up-all','down-all','status'
    )]
    [string]$Command = 'help'
)

$ErrorActionPreference = 'Stop'

# ============================================================================
# Helpers
# ============================================================================

function Assert-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host ""
        Write-Host "ERROR: 'docker' command not found." -ForegroundColor Red
        Write-Host ""
        Write-Host "Install Docker Desktop for Windows:"
        Write-Host "  Option A (winget):  winget install -e --id Docker.DockerDesktop"
        Write-Host "  Option B (manual):  https://www.docker.com/products/docker-desktop/"
        Write-Host ""
        Write-Host "After install: start Docker Desktop and wait until the whale icon"
        Write-Host "in the system tray says 'Docker Desktop is running', then re-run this script."
        Write-Host ""
        exit 1
    }
    docker info --format '{{.ServerVersion}}' *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ERROR: docker CLI found but the daemon is not responding." -ForegroundColor Red
        Write-Host "Start Docker Desktop and wait for it to finish initializing, then retry."
        Write-Host ""
        exit 1
    }
}

function Invoke-Compose {
    param([string[]]$Args)
    & docker compose @Args
    if ($LASTEXITCODE -ne 0) {
        Write-Host "docker compose failed with exit code $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

# Open a new PowerShell window running a Maven Spring Boot service.
# The window title is set so you can identify it in the taskbar.
function Start-ServiceWindow {
    param(
        [Parameter(Mandatory)][string]$Title,
        [Parameter(Mandatory)][string]$Module,
        [hashtable]$ExtraEnv = @{}
    )

    # Build a `$env:KEY = 'VAL'; ...` prefix so env vars are set inside the new shell.
    $envSetup = ''
    foreach ($k in $ExtraEnv.Keys) {
        $val = $ExtraEnv[$k] -replace "'", "''"
        $envSetup += "`$env:$k = '$val'; "
    }

    $cmd = "$envSetup`$Host.UI.RawUI.WindowTitle = 'PhotoConnect: $Title'; " +
           "Write-Host '=== $Title ===' -ForegroundColor Cyan; " +
           "mvn -pl $Module spring-boot:run"

    Start-Process powershell.exe -ArgumentList @('-NoExit','-NoProfile','-Command',$cmd) `
                                 -WorkingDirectory $PSScriptRoot | Out-Null
}

# Open a new PowerShell window running the Vite frontend.
function Start-FrontendWindow {
    $frontend = Join-Path $PSScriptRoot 'frontend'
    $cmd = "`$Host.UI.RawUI.WindowTitle = 'PhotoConnect: frontend'; " +
           "Write-Host '=== frontend ===' -ForegroundColor Cyan; " +
           "if (-not (Test-Path 'node_modules')) { npm install }; npm run dev"

    Start-Process powershell.exe -ArgumentList @('-NoExit','-NoProfile','-Command',$cmd) `
                                 -WorkingDirectory $frontend | Out-Null
}

# Block until an HTTP endpoint returns 200, or timeout.
function Wait-Url {
    param(
        [Parameter(Mandatory)][string]$Url,
        [int]$TimeoutSec = 90,
        [string]$Label
    )
    if (-not $Label) { $Label = $Url }
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            if ($resp.StatusCode -eq 200) {
                Write-Host "  [OK]   $Label" -ForegroundColor Green
                return $true
            }
        } catch { Start-Sleep -Seconds 2 }
    }
    Write-Host "  [WARN] $Label did not respond within ${TimeoutSec}s - check its window for errors" -ForegroundColor Yellow
    return $false
}

# Block until a Docker container reports healthy.
function Wait-ContainerHealthy {
    param(
        [Parameter(Mandatory)][string]$Container,
        [int]$TimeoutSec = 90
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $status = docker inspect $Container --format '{{.State.Health.Status}}' 2>$null
        if ($status -eq 'healthy') {
            Write-Host "  [OK]   $Container is healthy" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 2
    }
    Write-Host "  [WARN] $Container did not become healthy within ${TimeoutSec}s" -ForegroundColor Yellow
    return $false
}

# Find every PID listening on the given TCP port and kill it.
function Stop-ProcessOnPort {
    param([Parameter(Mandatory)][int]$Port)
    $procIds = (netstat -ano | Select-String ":$Port\s" | Select-String 'LISTENING' |
                ForEach-Object { ($_ -split '\s+')[-1] }) | Sort-Object -Unique
    foreach ($procId in $procIds) {
        if ($procId -and $procId -ne '0') {
            try {
                Stop-Process -Id $procId -Force -ErrorAction Stop
                Write-Host "  [OK]   Stopped PID $procId on port $Port"
            } catch {
                Write-Host "  [WARN] Could not stop PID $procId on port $Port" -ForegroundColor Yellow
            }
        }
    }
}

function Write-Banner {
    param([string]$Text)
    Write-Host ""
    Write-Host "==============================================================" -ForegroundColor DarkCyan
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host "==============================================================" -ForegroundColor DarkCyan
}

# ============================================================================
# Command dispatch
# ============================================================================

switch ($Command) {
    'help' {
        Write-Host "PhotoConnect commands:"
        Write-Host ""
        Write-Host "  ALL-IN-ONE" -ForegroundColor Yellow
        Write-Host "  .\pc.ps1 up-all           Start EVERYTHING (Docker + 6 services + frontend)"
        Write-Host "  .\pc.ps1 down-all         Stop all Java services and Docker"
        Write-Host "  .\pc.ps1 status           Show which ports are listening"
        Write-Host ""
        Write-Host "  Infrastructure (Docker)"
        Write-Host "  .\pc.ps1 up               Start backing services in Docker"
        Write-Host "  .\pc.ps1 down             Stop Docker (keep volumes)"
        Write-Host "  .\pc.ps1 down-clean       Stop Docker AND delete data"
        Write-Host "  .\pc.ps1 restart          Restart Docker"
        Write-Host "  .\pc.ps1 logs             Tail all Docker logs"
        Write-Host "  .\pc.ps1 ps               List running containers"
        Write-Host ""
        Write-Host "  Maven"
        Write-Host "  .\pc.ps1 build            mvn clean install across all modules"
        Write-Host "  .\pc.ps1 test             Run all tests"
        Write-Host ""
        Write-Host "  Individual services (each blocks the terminal)"
        Write-Host "  .\pc.ps1 discovery-run    Eureka                   :8761"
        Write-Host "  .\pc.ps1 config-run       Config Server            :8888"
        Write-Host "  .\pc.ps1 gateway-run      API Gateway              :8080"
        Write-Host "  .\pc.ps1 auth-run         auth-service             :8081"
        Write-Host "  .\pc.ps1 photographer-run photographer-service     :8082"
        Write-Host "  .\pc.ps1 customer-run     customer-service         :8083"
        Write-Host "  .\pc.ps1 frontend-run     Vite dev server          :5173"
        Write-Host ""
        Write-Host "  Utility"
        Write-Host "  .\pc.ps1 auth-keys        Generate RSA key pair for auth-service"
    }

    'up' {
        Assert-Docker
        Invoke-Compose @('up','-d')
        Write-Host ""
        Write-Host "Infrastructure is up. URLs:" -ForegroundColor Green
        Write-Host "  Postgres : localhost:5432  (postgres/postgres)"
        Write-Host "  MySQL    : localhost:3306  (root/root)"
        Write-Host "  Redis    : localhost:6379"
        Write-Host "  MinIO API: http://localhost:9000  (minioadmin/minioadmin)"
        Write-Host "  MinIO UI : http://localhost:9001"
        Write-Host "  Zipkin   : http://localhost:9411"
    }
    'down'       { Assert-Docker; Invoke-Compose @('down') }
    'down-clean' { Assert-Docker; Invoke-Compose @('down','-v') }
    'restart'    { Assert-Docker; Invoke-Compose @('restart') }
    'logs'       { Assert-Docker; Invoke-Compose @('logs','-f','--tail=100') }
    'ps'         { Assert-Docker; Invoke-Compose @('ps') }
    'build'      { mvn -B clean install -DskipTests }
    'test'       { mvn -B test }

    'discovery-run'    { mvn -pl discovery-service spring-boot:run }
    'config-run'       { mvn -pl config-service spring-boot:run }
    'gateway-run'      {
        $env:AUTH_PUBLIC_KEY_PATH = Join-Path $PSScriptRoot 'auth-service\keys\public_key.pem'
        mvn -pl api-gateway spring-boot:run
    }
    'auth-run'         { mvn -pl auth-service spring-boot:run }
    'photographer-run' { mvn -pl photographer-service spring-boot:run }
    'customer-run'     { mvn -pl customer-service spring-boot:run }
    'frontend-run'     {
        Push-Location (Join-Path $PSScriptRoot 'frontend')
        try {
            if (-not (Test-Path 'node_modules')) { npm install }
            npm run dev
        } finally { Pop-Location }
    }
    'auth-keys'        { & "$PSScriptRoot\auth-service\scripts\generate-keys.ps1" }

    # ------------------------------------------------------------------------
    # The big one
    # ------------------------------------------------------------------------
    'up-all' {
        Assert-Docker

        Write-Banner 'Step 1/4 - Docker infrastructure'
        Invoke-Compose @('up','-d')
        Wait-ContainerHealthy 'pc-postgres' -TimeoutSec 60 | Out-Null
        Wait-ContainerHealthy 'pc-mysql'    -TimeoutSec 90 | Out-Null
        Wait-ContainerHealthy 'pc-redis'    -TimeoutSec 30 | Out-Null

        # Generate JWT keys if missing - both auth-service and the gateway need them.
        $publicKey = Join-Path $PSScriptRoot 'auth-service\keys\public_key.pem'
        if (-not (Test-Path $publicKey)) {
            Write-Host ""
            Write-Host "Generating RSA key pair for auth-service..." -ForegroundColor Cyan
            & "$PSScriptRoot\auth-service\scripts\generate-keys.ps1"
        }

        Write-Banner 'Step 2/4 - Foundation services (Eureka + Config)'
        Start-ServiceWindow -Title 'discovery-service :8761' -Module 'discovery-service'
        Start-ServiceWindow -Title 'config-service :8888'    -Module 'config-service'
        Wait-Url 'http://localhost:8761/actuator/health' -TimeoutSec 120 -Label 'Eureka  :8761' | Out-Null
        Wait-Url 'http://localhost:8888/actuator/health' -TimeoutSec 120 -Label 'Config  :8888' | Out-Null

        Write-Banner 'Step 3/4 - Business services + API gateway'
        Start-ServiceWindow -Title 'auth-service :8081'         -Module 'auth-service'
        Start-ServiceWindow -Title 'photographer-service :8082' -Module 'photographer-service'
        Start-ServiceWindow -Title 'customer-service :8083'     -Module 'customer-service'
        # Gateway needs an absolute path to the auth public key - mvn spring-boot:run
        # runs from the module directory so a relative path wouldn't resolve.
        Start-ServiceWindow -Title 'api-gateway :8080' -Module 'api-gateway' `
                            -ExtraEnv @{ 'AUTH_PUBLIC_KEY_PATH' = $publicKey }

        Wait-Url 'http://localhost:8081/actuator/health' -TimeoutSec 150 -Label 'auth-service         :8081' | Out-Null
        Wait-Url 'http://localhost:8082/actuator/health' -TimeoutSec 150 -Label 'photographer-service :8082' | Out-Null
        Wait-Url 'http://localhost:8083/actuator/health' -TimeoutSec 150 -Label 'customer-service     :8083' | Out-Null
        Wait-Url 'http://localhost:8080/actuator/health' -TimeoutSec 150 -Label 'api-gateway          :8080' | Out-Null

        Write-Banner 'Step 4/4 - Frontend (Vite)'
        Start-FrontendWindow
        Wait-Url 'http://localhost:5173/' -TimeoutSec 60 -Label 'Vite  :5173' | Out-Null

        Write-Host ""
        Write-Host "==============================================================" -ForegroundColor Green
        Write-Host "  PhotoConnect is up!" -ForegroundColor Green
        Write-Host "==============================================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "  Open the app:        " -NoNewline
        Write-Host "http://localhost:5173" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "  API Gateway          http://localhost:8080"
        Write-Host "  Eureka dashboard     http://localhost:8761"
        Write-Host "  Config Server        http://localhost:8888"
        Write-Host "  auth-service         http://localhost:8081"
        Write-Host "  photographer-service http://localhost:8082"
        Write-Host "  customer-service     http://localhost:8083"
        Write-Host "  MinIO console        http://localhost:9001  (minioadmin / minioadmin)"
        Write-Host "  Zipkin tracing       http://localhost:9411"
        Write-Host ""
        Write-Host "  To stop everything:  .\pc.ps1 down-all" -ForegroundColor DarkGray
    }

    'down-all' {
        Write-Banner 'Stopping all Java services'
        foreach ($port in 8080, 8081, 8082, 8083, 8761, 8888, 5173) {
            Stop-ProcessOnPort -Port $port
        }
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            Write-Banner 'Stopping Docker infrastructure'
            Invoke-Compose @('down')
        }
        Write-Host ""
        Write-Host "Everything stopped." -ForegroundColor Green
    }

    'status' {
        Write-Host ""
        Write-Host "Listening ports:"
        $rows = @(
            @{ Port=8761; Label='Eureka' },
            @{ Port=8888; Label='Config Server' },
            @{ Port=8080; Label='API Gateway' },
            @{ Port=8081; Label='auth-service' },
            @{ Port=8082; Label='photographer-service' },
            @{ Port=8083; Label='customer-service' },
            @{ Port=5173; Label='Frontend (Vite)' }
        )
        foreach ($r in $rows) {
            $listening = netstat -ano | Select-String ":$($r.Port)\s" | Select-String 'LISTENING'
            $label = $r.Label
            $port  = $r.Port
            if ($listening) {
                Write-Host ("  [UP]   {0,-22} :{1}" -f $label, $port) -ForegroundColor Green
            } else {
                Write-Host ("  [down] {0,-22} :{1}" -f $label, $port) -ForegroundColor DarkGray
            }
        }

        if (Get-Command docker -ErrorAction SilentlyContinue) {
            Write-Host ""
            Write-Host "Docker:"
            docker ps --format "  {{.Names}}: {{.Status}}" 2>$null
        }
    }
}
