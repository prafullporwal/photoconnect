# reviews-service

The 5th bounded context in PhotoConnect. Owns customer-authored reviews and
star ratings for photographers.

| Concern | This service |
|---|---|
| Port | 8084 |
| DB | PostgreSQL · `reviews_db` |
| Reads | `GET /api/v1/reviews/photographer/{profileId}` and `/summary/{profileId}` are public; `GET /api/v1/reviews/mine` requires a CUSTOMER token |
| Writes | `POST /api/v1/reviews` requires a CUSTOMER token AND a COMPLETED inquiry between the customer and photographer |
| Outbound | `photographer-service` (validate photographer), `customer-service` (verify completed booking), `auth-service` (mint service JWT) |

## The "completed booking" rule

There is no booking-service in MVP. Reviews-service proxies the rule onto the
existing `inquiries` table: an inquiry whose status is `COMPLETED` is the
authorisation gate. The check is enforced via a Feign call to customer-service's
internal endpoint `GET /internal/v1/inquiries/completed?customerId=&photographerProfileId=`,
which returns the inquiry's id (stamped onto the review row as an audit trail)
or 404 if no completed engagement exists.

When Phase 2 introduces a real `booking-service`, **only the predicate
endpoint changes** — reviews-service still calls "is there a completed
engagement?" and stamps an audit pointer.

## Endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST   | `/api/v1/reviews`                          | CUSTOMER | Creates a review (one per photographer per customer) |
| GET    | `/api/v1/reviews/mine`                     | CUSTOMER | Reviews authored by the caller |
| GET    | `/api/v1/reviews/photographer/{profileId}` | anonymous OK | All reviews for a photographer, newest first |
| GET    | `/api/v1/reviews/summary/{profileId}`      | anonymous OK | `{ averageRating, reviewCount }` |

Error contract:

- `400 Bad Request`  — DTO validation failure or photographer-not-found.
- `403 Forbidden`    — caller hasn't completed a booking with this photographer.
- `409 Conflict`     — caller already left a review for this photographer.
- `503 Service Unavailable` — a downstream Feign call (photographer- or customer-service) failed after retries.

## Run it locally

```powershell
# Prereqs (each in its own terminal — see top-level README for order):
#   discovery-service · config-service · api-gateway · auth-service
#   photographer-service · customer-service

# Then start reviews-service:
.\pc.ps1 reviews-run
# or, plain Maven:
mvn -pl reviews-service spring-boot:run
```

Verify it registered with Eureka: http://localhost:8761 should now list
`REVIEWS-SERVICE` alongside the others.

## Try it (PowerShell)

```powershell
# 1. Get a CUSTOMER access token (replace email/password with your test user)
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login `
  -ContentType 'application/json' `
  -Body (@{ email = 'alice@example.com'; password = 'pw' } | ConvertTo-Json)
$token = $login.accessToken
$headers = @{ Authorization = "Bearer $token" }

# 2. Set up the precondition: mark an existing inquiry COMPLETED.
#    (You need to have already created an inquiry as this customer against
#     the target photographer.)
$inquiryId = '<inquiry-uuid-here>'
Invoke-RestMethod -Method Patch `
  -Uri "http://localhost:8080/api/v1/inquiries/$inquiryId/status" `
  -Headers $headers `
  -ContentType 'application/json' `
  -Body (@{ status = 'COMPLETED' } | ConvertTo-Json)

# 3. Create the review.
$photographerProfileId = '<photographer-profile-uuid-here>'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/reviews `
  -Headers $headers `
  -ContentType 'application/json' `
  -Body (@{
      photographerProfileId = $photographerProfileId
      rating                = 5
      body                  = 'Beautiful wedding shoot, would book again.'
    } | ConvertTo-Json)

# 4. Read it back — anyone can.
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/v1/reviews/photographer/$photographerProfileId"

Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/v1/reviews/summary/$photographerProfileId"
```

Negative-path smoke test:

```powershell
# Customer with no COMPLETED inquiry → 403
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/reviews `
  -Headers $headers -ContentType 'application/json' `
  -Body (@{ photographerProfileId = '00000000-0000-0000-0000-000000000000'
            rating = 3; body = 'hi' } | ConvertTo-Json)
# → 403 with "no completed booking" message

# Rating 6 → 400 (DTO validation)
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/reviews `
  -Headers $headers -ContentType 'application/json' `
  -Body (@{ photographerProfileId = $photographerProfileId
            rating = 6; body = 'nope' } | ConvertTo-Json)
```

## Tests

```powershell
mvn -pl reviews-service test
```

Two suites:

- `ReviewsServiceApplicationTests` — context-loads smoke test under H2.
- `ReviewServiceTest` — service-layer unit tests covering the duplicate-review
  short-circuit, the "no completed booking" path (→ 403), the
  photographer-not-found path (→ 400), both downstream-503 cases, and the
  summary aggregate with the "no reviews" zero-flatten.
