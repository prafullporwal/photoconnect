# config-repo

This directory is the **Config Server backend** in the local profile. Spring Cloud Config Server (the `config-service` module) reads property files from here and serves them to client services over HTTP.

> **Format note:** Spring Cloud Config supports `.properties`, `.yml`, and `.yaml` files interchangeably in the same backend — the file extension picks the parser. PhotoConnect uses `.properties` consistently. If you ever need a particular file in YAML, drop a `.yml` here and it will be picked up alongside.

## File naming convention

Spring Cloud Config resolves files in this order, **most-specific wins**:

```
{application}-{profile}.properties      ← service-specific + profile-specific
{application}.properties                ← service-specific (any profile)
application-{profile}.properties        ← profile-specific defaults for ALL services
application.properties                  ← global defaults for ALL services
```

Where:
- `{application}` = the calling service's `spring.application.name` (e.g. `auth-service`)
- `{profile}` = the active Spring profile sent by the client (e.g. `local`, `aws`)

### Examples

| File | Served to |
|---|---|
| `application.properties` | every service in every profile |
| `application-local.properties` | every service when active profile = `local` |
| `auth-service.properties` | `auth-service` in every profile |
| `auth-service-local.properties` | `auth-service` only when active profile = `local` |

## Inspecting served config (sanity check)

With config-service running on 8888, hit the REST API directly. The **view format** is decoupled from the file extension on disk — `.properties`, `.yml`, and `.json` extensions on the URL each return the same data rendered in that format:

```bash
# JSON view (shows the merged propertySources Config Server assembled)
curl http://localhost:8888/auth-service/local

# Rendered as .properties (matches the on-disk format)
curl http://localhost:8888/auth-service-local.properties

# Same content rendered as YAML (works even when source is .properties)
curl http://localhost:8888/auth-service-local.yml

# The global defaults
curl http://localhost:8888/application/default
```

## Phase 2 migration to Git

Today the Config Server uses the **native** (filesystem) backend pointing at this folder. In Phase 2 we'll switch the `aws` profile to use the **git** backend. The settings already exist at `config-service/src/main/resources/application-git.properties`:

```properties
# In config-service/src/main/resources/application-git.properties (Phase 2)
spring.cloud.config.server.git.uri=https://github.com/your-org/photoconnect-config
spring.cloud.config.server.git.default-label=main
```

Same Config Server, same client wire-up — you'd just publish this directory to its own Git repo and update one URL. Zero application code changes.

## What lives here

- `application.properties` — defaults inherited by every PhotoConnect service (actuator, Eureka client, graceful shutdown, tracing endpoint).
- `api-gateway.properties` — routes, CORS, Swagger aggregation.
- (Files for each service get added as we build them in Steps 4–6.)
