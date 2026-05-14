# =============================================================================
# PhotoConnect PowerShell helper (Windows-friendly equivalent of the Makefile)
# =============================================================================
# Usage:   .\pc.ps1 <command>
# Example: .\pc.ps1 up
# =============================================================================

param(
    [Parameter(Position=0)]
    [ValidateSet('help','up','down','down-clean','restart','logs','ps','build','test','discovery-run')]
    [string]$Command = 'help'
)

$ErrorActionPreference = 'Stop'

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
    # docker CLI exists; make sure the daemon is actually reachable
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

switch ($Command) {
    'help' {
        Write-Host "PhotoConnect commands:"
        Write-Host "  .\pc.ps1 up             Start all local infrastructure"
        Write-Host "  .\pc.ps1 down           Stop infrastructure (keep volumes)"
        Write-Host "  .\pc.ps1 down-clean     Stop infrastructure AND delete data"
        Write-Host "  .\pc.ps1 restart        Restart all infrastructure"
        Write-Host "  .\pc.ps1 logs           Tail all infra logs"
        Write-Host "  .\pc.ps1 ps             List running containers"
        Write-Host "  .\pc.ps1 build          mvn clean install across all modules"
        Write-Host "  .\pc.ps1 test           Run all tests"
        Write-Host "  .\pc.ps1 discovery-run  Start the Eureka server (port 8761)"
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
    'discovery-run' { mvn -pl discovery-service spring-boot:run }
}
