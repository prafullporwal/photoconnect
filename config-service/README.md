# config-service (Spring Cloud Config Server)

Centralized configuration for every PhotoConnect client service. Reads YAML files from a backend (filesystem in `local`, Git in `aws`) and serves them over HTTP.

## Responsibility

| Responsibility | Detail |
|---|---|
| Resolve config per client | `GET /{application}/{profile}` returns the merged config for a service |
| Serve rendered config | `GET /{application}-{profile}.properties` returns properties; `.yml` and `.json` views also work |
| Source of truth | All env-specific values for all services live in `config-repo/` |
| Hot reload | Clients hit `POST /actuator/refresh` to re-bind `@RefreshScope` beans |

## Endpoints (Config Server exposes these automatically)

| URL | Returns |
|---|---|
| `GET http://localhost:8888/{app}/{profile}[/label]` | JSON view of resolved property sources for that app + profile |
| `GET http://localhost:8888/{app}-{profile}.properties` | raw merged config rendered as .properties |
| `GET http://localhost:8888/{app}-{profile}.yml` | same content rendered as YAML (source format is independent of view format) |
| `GET http://localhost:8888/actuator/health` | health |
| `GET http://localhost:8888/actuator/info` | build info |

`label` defaults to `main` for Git backends, ignored for native.

## Run it locally

```powershell
# From repo root:
.\pc.ps1 config-run
# or:
mvn -pl config-service spring-boot:run
```

## Smoke tests

```bash
# 1. Health probe
curl http://localhost:8888/actuator/health
# → {"status":"UP"}

# 2. Fetch the global defaults (config-repo/application.properties)
curl http://localhost:8888/application/default | jq .
# → JSON with "propertySources" containing your application.properties entries

# 3. Same content but rendered as raw .properties
curl http://localhost:8888/application-default.properties

# 4. Fetch what auth-service would receive in the local profile (will only
#    show the global application.properties until we add
#    auth-service.properties in Step 4)
curl http://localhost:8888/auth-service/local
```

## Tests

```bash
mvn -pl config-service test
```

Two test methods:
- `contextLoads()` — auto-config smoke test
- `servesApplicationDefaults()` — actually hits the Config Server's REST API and asserts it returns the shared `application.properties`. Catches backend-misconfiguration bugs (wrong path, missing file).

## How clients will plug in (preview of Step 3+)

```properties
# Inside any future service's application.properties:
# (spring.application.name = what Config Server uses to resolve files)
spring.application.name=api-gateway
spring.config.import=optional:configserver:http://localhost:8888

# The client tells Config Server which profile to resolve:
spring.cloud.config.profile=${spring.profiles.active:local}
```

The `optional:` prefix means "if Config Server isn't reachable, keep booting from local YAML." Drop the prefix in `aws` profile if you want fail-fast behavior in production.

## Phase 2 checklist

1. Switch `application-git.properties` to point at the real git repo (URL + creds).
2. Add `spring-cloud-aws-starter-secrets-manager` for secret resolution.
3. Front the Config Server with HTTP basic auth or internal ALB + IAM.
4. Add `spring-cloud-bus-amqp` (or SQS) so a single `/actuator/busrefresh` cascades to every instance.
