# config-repo

This directory is the **Config Server backend** in the local profile. Spring Cloud Config Server (the `config-service` module) reads YAML files from here and serves them to client services over HTTP.

## File naming convention

Spring Cloud Config resolves config files in this order, **most-specific wins**:

```
{application}-{profile}.yml      ← service-specific + profile-specific
{application}.yml                ← service-specific (any profile)
application-{profile}.yml        ← profile-specific defaults for ALL services
application.yml                  ← global defaults for ALL services
```

Where:
- `{application}` = the calling service's `spring.application.name` (e.g. `auth-service`)
- `{profile}` = the active Spring profile sent by the client (e.g. `local`, `aws`)

### Examples

| File | Served to |
|---|---|
| `application.yml` | every service in every profile |
| `application-local.yml` | every service when active profile = `local` |
| `auth-service.yml` | `auth-service` in every profile |
| `auth-service-local.yml` | `auth-service` only when active profile = `local` |

## Inspecting served config (sanity check)

With config-service running on 8888, hit the REST API directly:

```bash
# Show what would be served to auth-service with profile=local
curl http://localhost:8888/auth-service/local

# Show the raw YAML for an app+profile
curl http://localhost:8888/auth-service-local.yml

# Show the global defaults
curl http://localhost:8888/application/default
```

## Phase 2 migration to Git

Today the Config Server uses the **native** (filesystem) backend pointing at this folder. In Phase 2 we'll switch the `aws` profile to use the **git** backend:

```yaml
# In config-service/application-aws.yml (Phase 2)
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-org/photoconnect-config
          search-paths: '*'
          default-label: main
```

Same Config Server, same client wire-up — you'd just publish this directory to its own Git repo and update one URL. Zero application code changes.

## What lives here

- `application.yml` — defaults inherited by every PhotoConnect service (actuator, Eureka client, graceful shutdown, etc.).
- (Files for each service get added as we build them in Steps 3–6.)
