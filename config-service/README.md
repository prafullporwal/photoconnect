# config-service (Spring Cloud Config Server)

Centralized configuration for every PhotoConnect client service. Reads YAML files from a backend (filesystem in `local`, Git in `aws`) and serves them over HTTP.

## Responsibility

| Responsibility | Detail |
|---|---|
| Resolve config per client | `GET /{application}/{profile}` returns the merged config for a service |
| Serve raw YAML | `GET /{application}-{profile}.yml` returns YAML directly |
| Source of truth | All env-specific values for all services live in `config-repo/` |
| Hot reload | Clients hit `POST /actuator/refresh` to re-bind `@RefreshScope` beans |

## Endpoints (Config Server exposes these automatically)

| URL | Returns |
|---|---|
| `GET http://localhost:8888/{app}/{profile}[/label]` | JSON view of resolved property sources for that app + profile |
| `GET http://localhost:8888/{app}-{profile}.yml` | raw merged YAML for that app + profile |
| `GET http://localhost:8888/{app}-{profile}.properties` | same as .properties format |
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

# 2. Fetch the global defaults (config-repo/application.yml)
curl http://localhost:8888/application/default | jq .
# → JSON with "propertySources" containing your application.yml entries

# 3. Same content but as raw YAML
curl http://localhost:8888/application-default.yml

# 4. Fetch what auth-service would receive in the local profile (will only
#    show the global application.yml until we add auth-service.yml in Step 4)
curl http://localhost:8888/auth-service/local
```

## Tests

```bash
mvn -pl config-service test
```

Two test methods:
- `contextLoads()` — auto-config smoke test
- `servesApplicationDefaults()` — actually hits the Config Server's REST API and asserts it returns the shared `application.yml`. Catches backend-misconfiguration bugs (wrong path, missing file).

## How clients will plug in (preview of Step 3+)

```yaml
# Inside any future service's application.yml:
spring:
  application:
    name: api-gateway          # ← what Config Server uses to resolve files
  config:
    import: "optional:configserver:http://localhost:8888"

# In application-local.yml the client tells Config Server which profile
# to resolve for it:
spring:
  cloud:
    config:
      profile: local
```

The `optional:` prefix means "if Config Server isn't reachable, keep booting from local YAML." Drop the prefix in `aws` profile if you want fail-fast behavior in production.

## Phase 2 checklist

1. Switch `application-aws.yml` to the git backend (URL + creds).
2. Add `spring-cloud-aws-starter-secrets-manager` for secret resolution.
3. Front the Config Server with HTTP basic auth or internal ALB + IAM.
4. Add `spring-cloud-bus-amqp` (or SQS) so a single `/actuator/busrefresh` cascades to every instance.
