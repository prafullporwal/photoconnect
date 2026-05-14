# =============================================================================
# PhotoConnect convenience Makefile
# =============================================================================
# Windows: install `make` via Chocolatey (`choco install make`) or use Git Bash.
# Alternatively, run the equivalent docker/maven commands directly, or use
# the PowerShell helper at .\pc.ps1.
# =============================================================================

.PHONY: help up down restart logs ps clean build test \
        discovery-run config-run gateway-run

help:
	@echo "PhotoConnect targets:"
	@echo "  make up             - start all local infrastructure (postgres, mysql, redis, minio, zipkin)"
	@echo "  make down           - stop infrastructure (keep volumes)"
	@echo "  make down-clean     - stop infrastructure AND delete all data volumes"
	@echo "  make restart        - restart all infrastructure"
	@echo "  make logs           - tail all infra logs"
	@echo "  make ps             - list running containers"
	@echo "  make build          - mvn clean install across all modules"
	@echo "  make test           - run all tests across all modules"
	@echo "  make discovery-run  - start the Eureka server (port 8761)"
	@echo "  make config-run     - start the Config Server (port 8888)"
	@echo "  make gateway-run    - start the API Gateway (port 8080)"

up:
	docker compose up -d
	@echo ""
	@echo "Infrastructure is up. Useful URLs:"
	@echo "  Postgres : localhost:5432  (postgres/postgres)"
	@echo "  MySQL    : localhost:3306  (root/root or customer_user/customer_pass)"
	@echo "  Redis    : localhost:6379"
	@echo "  MinIO API: http://localhost:9000  (minioadmin/minioadmin)"
	@echo "  MinIO UI : http://localhost:9001"
	@echo "  Zipkin   : http://localhost:9411"

down:
	docker compose down

down-clean:
	docker compose down -v

restart:
	docker compose restart

logs:
	docker compose logs -f --tail=100

ps:
	docker compose ps

build:
	mvn -B clean install -DskipTests

test:
	mvn -B test

discovery-run:
	mvn -pl discovery-service spring-boot:run

config-run:
	mvn -pl config-service spring-boot:run

gateway-run:
	mvn -pl api-gateway spring-boot:run
