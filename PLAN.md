# Build Plan

Day-wise plan to take `ecommerce-platform` from its current state to a
deployable, production-quality system. Status reflects a repo audit on
2026-07-02: core CRUD/eventing paths exist across all seven services and the
Angular UI, but there are no tests, no DB migrations, no Dockerfiles, and no
CI anywhere in the repo. Checkboxes marked `[x]` are already done.

See [ARCHITECTURE.md](ARCHITECTURE.md) for service responsibilities and
[CLAUDE.md](CLAUDE.md) for build commands and conventions. Update
[CHANGELOG.md](CHANGELOG.md) at the end of each day.

---

## Phase 1 — Core Services (Days 1–8)

### Day 1 — Scaffolding & Discovery Server
- [x] Monorepo Maven parent POM + 7 service modules
- [x] `discovery-server`: Eureka registry, port 8761
- [x] `ARCHITECTURE.md`, `CLAUDE.md`, base `README.md`

### Day 2 — Auth Service
- [x] `User`/`Role` entities, `UserRepository`
- [x] `POST /api/auth/register`, `POST /api/auth/login`
- [x] JWT issuance (`JwtUtil`), `SecurityConfig`, `GlobalExceptionHandler`
- [x] Role-based authorization checks (admin vs customer) on protected endpoints — gateway forwards roles via `X-Auth-Roles`, `HeaderAuthenticationFilter` + `@PreAuthorize("hasRole('ADMIN')")` guard the new admin-only `GET /api/auth/users`
- [x] `POST /api/auth/validate` endpoint (token-refresh not built — `/validate` covers the ARCHITECTURE.md-documented use case)

### Day 3 — API Gateway
- [x] Spring Cloud Gateway routes to auth/product/order/inventory services
- [x] `JwtAuthenticationFilter` validating tokens at the edge
- [x] CORS config for `localhost:4200`
- [x] Rate limiting — in-memory fixed-window limiter (`RateLimitingFilter`, 60 req/min per client IP); note this is per-instance, swap for Redis-backed if the gateway is ever scaled horizontally
- [x] Centralized error responses for downstream 5xx/timeouts — `GlobalErrorHandlingFilter` returns uniform JSON on unreachable/slow downstreams; `httpclient.connect-timeout`/`response-timeout` configured

### Day 4 — Product Service
- [x] `Product` entity, `ProductRepository`, `ProductService`
- [x] Full CRUD under `/api/products` with request/response DTOs
- [x] Search/filter endpoint — `GET /api/products/search?name=&category=&minPrice=&maxPrice=` via JPA `Specification`s
- [x] Input validation (`@Valid`) on create/update DTOs

### Day 5 — Order Service
- [x] `Order`/`OrderItem`/`OrderStatus` model, `OrderRepository`
- [x] `POST/GET /api/orders`, Kafka producer emitting `order.created`
- [x] Order status transitions (`CREATED → CONFIRMED → SHIPPED → DELIVERED`, `CANCELLED`) + `PATCH /api/orders/{id}/status` validating the transition graph
- [x] Synchronous inventory-availability check at order creation — `InventoryClient` (load-balanced `RestClient` via Eureka) rejects the order with 409 if any item is out of stock

### Day 6 — Inventory Service
- [x] `InventoryItem` entity, `InventoryRepository`
- [x] `GET/PUT /api/inventory/{productId}`
- [x] Kafka consumer reacting to `order.created`, emits `inventory.updated`
- [x] Idempotent consumption — `ProcessedOrderEvent` (unique `orderId`) tracked per event; duplicate deliveries are skipped
- [x] Low-stock / out-of-stock guard — `reserveStock` now throws `InsufficientStockException` (mapped to 409) instead of silently clamping to zero; consumer logs and drops rather than redelivering forever

### Day 7 — Notification Service
- [x] Kafka consumer for `order.created`
- [ ] Replace log-only stub (`// TODO: integrate with an email/SMS/push provider`) with real **Strategy pattern**: `NotificationStrategy` interface + `EmailStrategy`/`SmsStrategy`/`PushStrategy`, selected at runtime
- [ ] Wire an actual email provider (e.g. SMTP/SendGrid sandbox) for at least one strategy end-to-end

### Day 8 — Angular UI: Cart & Checkout
- [x] Standalone components: `HomeComponent`, `LoginComponent`, `RegisterComponent`, `ProductListComponent`
- [x] `AuthService`, `ProductService`, JWT `auth.interceptor.ts`
- [ ] Route guards on `/products`, `/orders`, `/home` (currently unauthenticated users can hit protected routes client-side)
- [ ] Cart component (add/remove/update quantity, local state)
- [ ] Checkout flow calling `POST /api/orders`
- [ ] Order history page calling `GET /api/orders`

---

## Phase 2 — Data & Quality (Days 9–11)

### Day 9 — Database Migrations
- [ ] Add Flyway to all 4 DB-backed services (auth, product, order, inventory)
- [ ] Write baseline `V1__init.sql` per service capturing the current Hibernate-generated schema
- [ ] Flip `ddl-auto` from `update` to `validate` once migrations are in place

### Day 10 — Testing: Unit & Integration
- [ ] Unit tests for service/domain logic in each of the 7 services (JUnit 5 + Mockito)
- [ ] Testcontainers-based integration tests for repository layers (Postgres) and Kafka producers/consumers
- [ ] Angular unit tests for `AuthService`, `ProductService`, and the new cart/checkout components
- [ ] Wire `mvn test` and `ng test` into a local pre-push check

### Day 11 — Contract Tests & Resilience
- [ ] Spring Cloud Contract (or equivalent) between order-service ↔ inventory-service
- [ ] Resilience4j circuit breakers on gateway→service and order→inventory calls
- [ ] Retry/backoff policy for Kafka consumers on transient failures

---

## Phase 3 — Operability (Days 12–14)

### Day 12 — Observability
- [ ] Enable `/actuator/health` and `/actuator/info` on all 7 services (none currently expose Actuator)
- [ ] Structured JSON logging (Logback) with `traceId`/`spanId`
- [ ] OpenTelemetry + Jaeger wiring for distributed tracing across gateway → services

### Day 13 — Containerization
- [ ] Dockerfile per service (7 total) — none exist today
- [ ] Root `docker-compose.yml`: Postgres, Kafka (+ Zookeeper/KRaft), all 7 services, Angular UI
- [ ] Replace hardcoded `localhost` DB/Kafka hosts in each `application.yml` with env vars for compose networking

### Day 14 — CI Pipeline
- [ ] `.github/workflows/build.yml`: matrix build + `mvn test` per service on PR
- [ ] `.github/workflows/lint.yml`: Angular lint/test on PR
- [ ] Fail PRs on test failure; publish build status badge in `README.md`

---

## Phase 4 — Hardening & Release (Days 15–16)

### Day 15 — Security Review
- [ ] Externalize `JWT_SECRET` and DB credentials fully out of `application.yml` defaults (currently placeholder values checked in per README)
- [ ] Run `/security-review` skill against the full diff since Day 1
- [ ] Service-to-service auth (mTLS or shared secret) between internal calls, per ARCHITECTURE.md security section

### Day 16 — Deployment
- [ ] Kubernetes manifests (or chosen platform equivalent) per service, ConfigMaps/Secrets for env config
- [ ] Smoke-test full flow in a staging-like environment: register → login → browse products → add to cart → checkout → receive notification
- [ ] Finalize `CHANGELOG.md` for `1.0.0` release, tag the release

---

## Notes
- Days assume ~1 focused day per row; compress or parallelize across services if multiple people are working simultaneously (e.g., Days 4–7 are independent of each other).
- Re-check this plan's checkboxes against actual repo state before each day's work — it reflects a point-in-time audit, not a live status board.
