# discovery-service (Eureka Server)

The **service registry** for PhotoConnect. Every other backend service will register itself here on startup and use this registry to look up its peers by logical name (`AUTH-SERVICE`, `PHOTOGRAPHER-SERVICE`, …).

## Responsibility

| Responsibility | Detail |
|---|---|
| Accept registrations | Eureka clients POST `/eureka/apps/{APP_NAME}` on startup |
| Accept heartbeats | Clients send PUT every 30s; missing heartbeats evict the instance |
| Serve the registry | Clients GET the list to do client-side load balancing |
| Dashboard | `http://localhost:8761` — visual list of registered services |

## Endpoints

| URL | Purpose |
|---|---|
| `GET http://localhost:8761/` | Eureka dashboard (HTML) |
| `GET http://localhost:8761/eureka/apps` | All registered apps (XML or JSON via `Accept`) |
| `GET http://localhost:8761/actuator/health` | Spring health probe |
| `GET http://localhost:8761/actuator/info` | Build/info |

## Run it locally

From the **repo root**:

```bash
# Option A: Maven (recommended for active development)
mvn -pl discovery-service spring-boot:run
# or:  make discovery-run

# Option B: built JAR
mvn -pl discovery-service clean package -DskipTests
java -jar discovery-service/target/discovery-service-0.0.1-SNAPSHOT.jar

# Option C: Docker
docker build -t photoconnect/discovery-service:dev -f discovery-service/Dockerfile .
docker run --rm -p 8761:8761 photoconnect/discovery-service:dev
```

Then open <http://localhost:8761>. The "Instances currently registered with Eureka" table will be empty until you start other services in later steps — that's expected.

## Test it

```bash
# Health probe
curl http://localhost:8761/actuator/health
# Expected: {"status":"UP"}

# Registry JSON
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
# Expected:  {"applications":{"versions__delta":"1","apps__hashcode":"","application":[]}}
```

## Run the tests

```bash
mvn -pl discovery-service test
```

The included `DiscoveryServiceApplicationTests.contextLoads()` is a smoke test — it boots the Spring context against a random port and asserts nothing throws. Fast and catches most configuration drift.

## Configuration reference

| Property | Where | Why |
|---|---|---|
| `eureka.client.register-with-eureka=false` | application.yml | Single-node server should not register with itself |
| `eureka.client.fetch-registry=false` | application.yml | Same — no peer to pull from |
| `eureka.server.enable-self-preservation=false` | local profile | Local dev wants stale instances evicted fast; prod wants the safety net |
| `eureka.server.eviction-interval-timer-in-ms=5000` | application.yml | Faster eviction sweep during dev |
| `server.shutdown=graceful` | application.yml | Lets in-flight requests finish during SIGTERM (matters in K8s) |

## Phase 2 (AWS / EKS) checklist

When we migrate:

1. Run 2–3 replicas in different AZs; configure peer URLs so they share state.
2. Enable self-preservation (`application-aws.yml`).
3. Decide whether to keep Eureka or switch to K8s native discovery — likely keep for portability, but K8s `Service` DNS becomes an option.
4. Place behind a private ALB; never expose to the internet.
