# PhotoConnect

A microservices marketplace where photographers showcase their work and customers discover and contact them. Built as a learning project for modern Spring Cloud patterns, designed to run fully **local-first** and migrate to AWS later with config-only changes.

## Architecture (MVP)

```
                                  ┌─────────────────────┐
                                  │     React SPA       │
                                  │  (Vite, Tailwind)   │
                                  └──────────┬──────────┘
                                             │
                                             ▼
                              ┌────────────────────────────┐
                              │      API Gateway           │
                              │  Spring Cloud Gateway      │
                              │  • routing                 │
                              │  • JWT validation          │
                              │  • correlation-id          │
                              │  • CORS                    │
                              └─────┬──────────┬───────┬───┘
                                    │          │       │
                  ┌─────────────────┘          │       └────────────────┐
                  ▼                            ▼                        ▼
        ┌──────────────────┐        ┌──────────────────┐      ┌──────────────────┐
        │  auth-service    │        │ photographer-svc │      │ customer-service │
        │  (PostgreSQL)    │        │  (PostgreSQL)    │      │    (MySQL)       │
        │  • register      │        │  • profile       │      │  • profile       │
        │  • login         │        │  • content meta  │      │  • inquiries     │
        │  • JWT issuance  │        │  • MinIO/S3      │      │                  │
        └──────────────────┘        └──────────────────┘      └──────────────────┘
                  ▲                            ▲                        ▲
                  │                            │                        │
                  └────────────┬───────────────┴────────────┬───────────┘
                               │                            │
                               ▼                            ▼
                    ┌──────────────────┐         ┌──────────────────┐
                    │ discovery (Eureka)│         │  config-service  │
                    │   port 8761       │         │   port 8888      │
                    └──────────────────┘         └──────────────────┘

   Infra (docker compose): postgres • mysql • redis • minio • zipkin
```

## Module layout

```
PhotoConnect/
├── pom.xml                      ← parent POM (dependency mgmt only)
├── docker-compose.yml           ← local infra
├── Makefile                     ← linux/mac convenience
├── pc.ps1                       ← Windows PowerShell convenience
├── infra/
│   └── postgres/init-multiple-dbs.sh
├── discovery-service/           ← Netflix Eureka server  (port 8761)  ✓ Step 1
├── config-service/              ← Spring Cloud Config    (port 8888)    Step 2
├── api-gateway/                 ← Spring Cloud Gateway   (port 8080)    Step 3
├── auth-service/                ← Auth + JWT             (port 8081)    Step 4
├── photographer-service/        ← Profiles + media       (port 8082)    Step 5
├── customer-service/            ← Inquiries              (port 8083)    Step 6
└── frontend/                    ← React 18 + Vite                       Step 7
```

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 (LTS) | Use Temurin or Liberica |
| Maven | 3.9+ | `mvn -v` |
| Docker Desktop | latest | Must support `docker compose` v2 |
| Node | 20+ | Only needed for the React frontend later |
| make | optional | Windows: `choco install make` or just use `.\pc.ps1` |

## Quick start

```bash
# 1. Bring up the local infra (postgres, mysql, redis, minio, zipkin)
make up          # or:   .\pc.ps1 up   on Windows

# 2. Verify it's healthy
docker compose ps

# 3. Build everything
make build       # or:   .\pc.ps1 build

# 4. Start the Eureka discovery server (Step 1)
make discovery-run     # or:  .\pc.ps1 discovery-run

# Open the Eureka dashboard
open http://localhost:8761
```

Tear-down: `make down` (keeps DB data) or `make down-clean` (wipes volumes).

## Spring profiles

Every service has two profiles:

- **`local`** (default) — uses the docker-compose infra above.
- **`aws`** — placeholder for Phase 2. Reads from AWS Secrets Manager, RDS, S3, ElastiCache. No code changes needed to switch.

The active profile is controlled by `SPRING_PROFILES_ACTIVE`:

```bash
export SPRING_PROFILES_ACTIVE=local    # default in dev
export SPRING_PROFILES_ACTIVE=aws      # for Phase 2 EKS deploys
```

## Port map

| Component | Port | URL |
|---|---|---|
| Eureka (discovery) | 8761 | http://localhost:8761 |
| Config | 8888 | http://localhost:8888 |
| API Gateway | 8080 | http://localhost:8080 |
| auth-service | 8081 | http://localhost:8081 |
| photographer-service | 8082 | http://localhost:8082 |
| customer-service | 8083 | http://localhost:8083 |
| Frontend (Vite dev) | 5173 | http://localhost:5173 |
| PostgreSQL | 5432 | jdbc:postgresql://localhost:5432/ |
| MySQL | 3306 | jdbc:mysql://localhost:3306/customer_db |
| Redis | 6379 | redis://localhost:6379 |
| MinIO API | 9000 | http://localhost:9000 |
| MinIO Console | 9001 | http://localhost:9001 (minioadmin/minioadmin) |
| Zipkin | 9411 | http://localhost:9411 |

## Roadmap

- **MVP (in progress):** discovery → config → api-gateway → auth → photographer → customer → frontend.
- **Phase 2:** Saga, CQRS, Kafka event-driven, Outbox pattern, AWS migration (RDS, S3, ElastiCache, EKS, Secrets Manager, CloudWatch).

Each service has its own README with endpoints and run instructions.
