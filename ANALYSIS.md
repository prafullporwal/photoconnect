# PhotoConnect — Application Analysis & Roadmap

_Last updated: 2026-05-21_

A holistic review of the current state of PhotoConnect and a prioritised roadmap
for where to take it next.

---

## 1. What's built today

### Backend — 6 services (Java 21 / Spring Boot 3.4 / Spring Cloud 2024.0)

| Service                | Responsibility                                                                                          |
| ---------------------- | ------------------------------------------------------------------------------------------------------- |
| `discovery-service`    | Eureka service registry                                                                                 |
| `config-service`       | Spring Cloud Config (native + git-backed `config-repo/`)                                                |
| `api-gateway`          | WebFlux gateway, RS256 JWT validation as a `GlobalFilter`, configurable public paths                    |
| `auth-service`         | RS256 JWTs, BCrypt-12, refresh-token rotation, Redis blacklist, Postgres + Flyway, Testcontainers tests |
| `photographer-service` | Profile CRUD, portfolio uploads to S3/MinIO, public feed + per-photographer portfolio, role-guarded     |
| `customer-service`     | Customer profile, inquiries to photographers via Feign, MySQL with UUID-as-CHAR fix                     |

### Frontend (React 19 + Vite + TS + Tailwind 4)

- **Browse** — gradient hero, sticky search/filter bar, CSS-columns masonry, lightbox, click-through to photographer profile
- **Photographer detail** — gradient banner, deterministic avatar, gallery, "Send inquiry" CTA
- **Login / Register** — role-aware redirect
- **My profile** — role-dispatched, summary + edit toggle, portfolio gallery embedded for photographers
- **Portfolio upload** — React Hook Form + Zod 4
- **Inbox** (photographer) / **Inquiries** (customer)
- **Header** — frosted-glass, sticky, animated active underline, role-aware nav

### Infrastructure & tooling

- Docker Compose for Postgres, MySQL, Redis, MinIO
- `pc.ps1` orchestrator — `up-all` / `down-all` / `status` / per-service runners
- `config-repo/` checked into git as the config source

---

## 2. Architecture analysis

### Strengths

- **Service boundaries align with bounded contexts.** Auth, photographer, and customer are independently deployable and own their data.
- **Defense in depth.** Gateway validates the JWT; services re-validate via `@PreAuthorize`. Even if the gateway is misconfigured, photographers still can't browse competitors.
- **Feed denormalisation.** `GET /feed` joins portfolio items with photographer metadata server-side; the frontend gets one query per page render.
- **OpenFeign + Eureka.** Service discovery is opaque to the calling code. Adding a second photographer-service instance just works.
- **Lightbox + masonry pattern** is reused on Browse and Portfolio — UI is cohesive without a heavy design system.
- **Role-aware routing** is enforced both on the backend and in `Header.tsx` / `useAuth` — no flicker, no leaked links.

### Weaknesses / risks

- **Test coverage falls off after Step 4.** Auth-service has Testcontainers tests; photographer-service and customer-service have very little. Hard to refactor confidently.
- **No circuit breaker.** If `photographer-service` is slow, every customer inquiry call blocks. Add Resilience4j on the Feign client.
- **Single Eureka node, no clustering** — fine for local, but a SPOF in production.
- **Storage cleanup is best-effort.** Deleting a portfolio item deletes the DB row; S3 object cleanup isn't transactional. Orphans will accumulate.
- **Hardcoded MinIO credentials** in `application.properties`. Needs to move to env / secrets manager.
- **No service-to-service auth.** customer-service → photographer-service Feign calls carry no token; anyone inside the cluster can call internal endpoints.
- **No pagination on `/feed` or `/portfolio`.** Will OOM the browser once a photographer has 500 items.
- **No transactional outbox** on the inquiry flow — if customer-service writes the inquiry and the Feign call to photographer-service then fails, the customer thinks it was sent.
- **No real-time updates.** Photographers must refresh to see new inquiries.
- **Single AZ / single region** assumption baked into config — fine for learning, would need rework on AWS.

---

## 3. Roadmap — what to build next

Five tracks. Pick a track, finish one slice end-to-end, then jump tracks.

### Track A — Hardening (low-glamour, high-leverage)

1. **Pagination** on `/feed` and `/portfolio` (`Pageable`, infinite scroll on the frontend).
2. **Resilience4j circuit breaker + retry** on `PhotographerClient` Feign calls.
3. **Integration tests** for photographer-service (Testcontainers Postgres + MinIO).
4. **Image thumbnails** — generate 400px webp on upload, serve thumbs in the feed, full image only in the lightbox.
5. **S3 cleanup job** — soft-delete portfolio rows; scheduled sweeper GCs orphans.

### Track B — High-impact business features

6. **Booking + availability calendar** — photographers mark dates available; customers pick a slot when sending an inquiry.
7. **Messaging thread** — replace one-shot inquiries with a back-and-forth conversation (`messages` table keyed on `inquiry_id`).
8. **Reviews + ratings** — a `reviews-service` (5th bounded context); 1–5 stars + text; only allowed after a completed booking.
9. **Favorites / "save photographer"** — customer-side bookmarking.
10. **Photographer response & status workflow** — `PENDING → ACCEPTED → DECLINED → COMPLETED`, with notifications.

### Track C — Discovery & UX polish

11. **Location filter** — distance from customer city; geo on PostGIS.
12. **Price-range filter** — photographers add starting price.
13. **Sort options** — most recent, top-rated, lowest-price.
14. **Profile photo + cover image upload** — currently we deterministically generate avatars; real photos belong here.
15. **Dark mode** + accessibility pass (focus rings; lightbox already has ARIA — audit the rest).
16. **Mobile pass** — masonry already adapts, but the sticky filter bar wraps awkwardly on phones.

### Track D — Platform & observability

17. **Centralised logging** — Loki + correlation-id (you already have the filter on the gateway — propagate it through Feign).
18. **Metrics dashboard** — Prometheus + Grafana; instrument auth flows and the feed endpoint.
19. **CI/CD** — GitHub Actions: build all modules, run tests, build Docker images.
20. **OpenSearch / Elasticsearch** for fuzzy search across photographer name + category — current `LIKE` query won't scale.
21. **Notification service** — async via Kafka or RabbitMQ; emit events from photographer-service & customer-service, fan out to email / in-app.

### Track E — Production-readiness (the Phase 2 AWS pivot)

22. **Real S3** instead of MinIO.
23. **AWS Secrets Manager** for DB creds and JWT keys.
24. **JWT for service-to-service** — short-lived internal tokens minted by auth-service, validated on every internal endpoint.
25. **Multi-instance Eureka / Spring Cloud Kubernetes** — or replace Eureka entirely with K8s service discovery once on EKS.
26. **Environment-driven config** — move `config-repo/` properties to a real git remote (CodeCommit / GitHub) with a `prod` profile.

---

## 4. Suggested starting order

If this were my project, the order would be:

1. **Pagination** (~1 day — unblocks everything else).
2. **Image thumbnails** (~1 day — the feed will get unusable without them).
3. **Booking + reviews + photographer status workflow** (the killer feature loop — without it this is a directory, not a marketplace).
4. **Real-time inbox via SSE or WebSockets** (the "wow" moment for photographers).

Everything else (auth hardening, AWS pivot, observability) is real work but doesn't change what the product _is_. The booking + review loop does.

---

## 5. Known open issues

- **Postgres port 5432 conflict on Windows.** Windows-installed `postgresql-x64-18` service competes with the Dockerised Postgres for photographer-service. Resolution:
  ```powershell
  Stop-Service postgresql-x64-18 -Force          # admin
  Set-Service postgresql-x64-18 -StartupType Manual   # admin, prevents auto-start
  ```

