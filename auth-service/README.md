# auth-service

Owns `auth_db` on PostgreSQL. Issues RS256-signed JWT access + refresh tokens. Validates tokens (defense in depth alongside the gateway). Blacklists revoked access tokens in Redis until expiry.

## Endpoints

| Method | URL | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | none | Create a new photographer/customer; returns access + refresh |
| POST | `/api/v1/auth/login` | none | Authenticate by email + password; returns access + refresh |
| POST | `/api/v1/auth/refresh` | none (refresh body) | Rotate refresh token; returns new access + refresh pair |
| POST | `/api/v1/auth/logout` | bearer access | Revoke all refresh tokens + blacklist current access jti |
| GET  | `/api/v1/auth/me` | bearer access | Return the authenticated user's profile |
| GET  | `/actuator/health` | none | Health probe |
| GET  | `/swagger-ui.html` | none | API docs UI |

## Token shape

**Access** (15 min):
```
{
  "iss":"photoconnect", "aud":["photoconnect-api"],
  "sub":"<userId>", "jti":"<uuid>",
  "iat":..., "exp":...,
  "typ":"access", "role":"PHOTOGRAPHER", "email":"alice@example.com"
}
```

**Refresh** (7 days):
```
{ "iss":"photoconnect", "aud":["photoconnect-api"],
  "sub":"<userId>", "jti":"<uuid>",
  "iat":..., "exp":..., "typ":"refresh" }
```

The `typ` claim lets us reject "access tokens passed as refresh tokens" and vice versa.

## First-time setup

```powershell
# 1. Bring up Postgres + Redis (auth-service needs both)
.\pc.ps1 up

# 2. Generate the RSA key pair (one-time)
.\auth-service\scripts\generate-keys.ps1
# (or:  bash auth-service/scripts/generate-keys.sh  on Linux/Mac/WSL)

# 3. Run auth-service
.\pc.ps1 auth-run
```

You'll see Flyway run `V1__create_users_table.sql` and `V2__create_refresh_tokens_table.sql` on first startup; subsequent runs just print "already applied".

## Smoke tests (direct to :8081, bypassing gateway)

```bash
# Register
curl -sX POST http://localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"supersecret","role":"PHOTOGRAPHER"}' | jq .

# Login
curl -sX POST http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"supersecret"}' | jq .

# Save the accessToken from the response
ACCESS=...

# /me (protected — needs Authorization header)
curl -s http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer $ACCESS" | jq .

# Refresh (use the refreshToken from login)
curl -sX POST http://localhost:8081/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<paste here>"}' | jq .

# Logout
curl -sX POST http://localhost:8081/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS"
```

Open <http://localhost:8081/swagger-ui.html> to interact with these endpoints visually. Click "Authorize" and paste the access token to call protected endpoints from the UI.

## Tests

```bash
mvn -pl auth-service test
```

| Test class | Slice | What it proves |
|---|---|---|
| `JwtServiceTest` | unit | RS256 sign/verify round-trip; tamper detection; expired/invalid rejected |
| `UserRepositoryTest` | `@DataJpaTest` + Testcontainers Postgres | Flyway applies cleanly; soft-delete queries work |
| `AuthControllerTest` | `@WebMvcTest` | Request validation, status codes, error envelope shape |
| `AuthServiceApplicationTests` | `@SpringBootTest` + Testcontainers | Full context loads against real Postgres |

## Phase 2 migration checklist

1. **Keys → AWS Secrets Manager**: replace `PemKeyLoader.loadXxx(Path)` with a `SecretsManagerKeyLoader` that pulls the same PEMs from Secrets Manager. The `JwtService` interface stays unchanged.
2. **DB → RDS**: change `spring.datasource.url` in `application-aws.properties`. No code change.
3. **Redis → ElastiCache**: change `spring.data.redis.host`. No code change.
4. **Key rotation**: support multiple public keys via JWK Set endpoint. Add a `kid` header to outgoing tokens.
5. **Idempotency-Key**: add header support to `/register` for safe POST retries from mobile clients on flaky networks.
