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
- [x] Replace log-only stub with real **Strategy pattern**: `NotificationStrategy` interface + `EmailNotificationStrategy`/`SmsNotificationStrategy`/`PushNotificationStrategy`, selected at runtime via `notification.channel` config (defaults to `EMAIL`)
- [x] Wire an actual email provider end-to-end — `EmailNotificationStrategy` sends real SMTP mail via `JavaMailSender` (`spring-boot-starter-mail`), config'd via `MAIL_HOST`/`MAIL_PORT`/`MAIL_USERNAME`/`MAIL_PASSWORD` env vars (defaults to a local dev SMTP catcher on `localhost:1025`); `order-service` now carries an optional `customerEmail` through `OrderRequest` → `Order` → `order.created` event so notification-service has a real recipient. SMS/Push strategies remain log-only stubs behind the same interface. Verified with a GreenMail-backed test that asserts a real message is delivered over SMTP.

### Day 8 — Angular UI: Cart & Checkout
- [x] Standalone components: `HomeComponent`, `LoginComponent`, `RegisterComponent`, `ProductListComponent`
- [x] `AuthService`, `ProductService`, JWT `auth.interceptor.ts`
- [x] Route guards on `/products`, `/orders`, `/home` (and `/cart`, `/checkout`) — functional `authGuard` redirects unauthenticated users to `/auth/login`
- [x] Cart component (add/remove/update quantity, local state) — signal-based `CartService`, wired into `ProductListComponent` ("Add to Cart") and a new `CartComponent`
- [x] Checkout flow calling `POST /api/orders` — `CheckoutComponent` + `OrderService`, builds `OrderRequest` from cart state, clears cart and redirects to `/orders` on success
- [x] Order history page calling `GET /api/orders` — `OrderHistoryComponent`, filters the full order list client-side by the signed-in username (no user-scoped query param exists on the backend yet)

---

## Phase 2 — Data & Quality (Days 9–11)

### Day 9 — Database Migrations
- [x] Add Flyway to all 4 DB-backed services (auth, product, order, inventory) — `flyway-core` + `flyway-database-postgresql`
- [x] Write baseline `V1__init.sql` per service capturing the current Hibernate-generated schema
- [x] Flip `ddl-auto` from `update` to `validate` once migrations are in place — `spring.flyway.baseline-on-migrate: true` added so pre-existing local dev DBs baseline at `V1` rather than failing on already-present tables

### Day 10 — Testing: Unit & Integration
- [x] Unit tests for service/domain logic in each of the 7 services (JUnit 5 + Mockito) — plain Mockito, no Spring context, matching the Day-7 `NotificationServiceTest` pattern; `discovery-server` gets a context-load smoke test since it has no domain logic of its own
- [x] Testcontainers-based integration tests for repository layers (Postgres) and Kafka producers/consumers — `*IT.java` (Failsafe, `mvn verify`) for `auth-service`/`product-service`/`order-service`/`inventory-service` repositories, plus a real producer→consumer Kafka path (`order-service` → `inventory-service`) over Testcontainers Kafka
- [x] Angular unit tests for `AuthService`, `ProductService`, and the new cart/checkout components — plus `CartService` and `OrderHistoryComponent`
- [x] Wire `mvn test` and `ng test` into a local pre-push check — `.githooks/pre-push`, enabled via `git config core.hooksPath .githooks`

### Day 11 — Contract Tests & Resilience
- [x] Spring Cloud Contract (or equivalent) between order-service ↔ inventory-service — YAML contracts in `inventory-service/src/test/resources/contracts` drive both plugin-generated producer verification tests (inventory-service) and a consumer-side stub-runner test (`InventoryClientContractTest`, order-service) that generates WireMock stubs from the contracts at runtime, so `mvn test` covers both sides without installing stub jars
- [x] Resilience4j circuit breakers on gateway→service and order→inventory calls — per-route `CircuitBreaker` filters in api-gateway with a shared 503 `/fallback`, and a Spring Cloud CircuitBreaker wrapper in `InventoryClient` mapping open-circuit/unreachable to 503 `InventoryUnavailableException`
- [x] Retry/backoff policy for Kafka consumers on transient failures — `DefaultErrorHandler` with `ExponentialBackOffWithMaxRetries` (1s ×2 up to 10s, 4 retries) in inventory- and notification-service; insufficient-stock / missing-record failures classified not-retryable (log and drop, preserving Day-6 semantics)

---

## Phase 3 — Operability (Days 12–14)

### Day 12 — Observability
- [x] Enable `/actuator/health` and `/actuator/info` on all 7 services (the starter was already on every classpath; exposure config + `info.app` payload added per service)
- [x] Structured JSON logging (Logback) with `traceId`/`spanId` — Boot 3.5 native ECS structured logging behind a `json-logs` profile; default console output keeps human-readable lines with `[app,traceId,spanId]` correlation
- [x] OpenTelemetry + Jaeger wiring for distributed tracing across gateway → services — `micrometer-tracing-bridge-otel` + OTLP/HTTP exporter on the 6 request-path services (discovery-server intentionally untraced), Kafka hops included via producer/listener observations; order-service's `RestClient.Builder` fixed to go through `RestClientBuilderConfigurer` so order→inventory calls propagate context

### Day 13 — Containerization
- [x] Dockerfile per service (7 total) — multi-stage (`maven:3.9-eclipse-temurin-25` build → `eclipse-temurin:25-jre` runtime, non-root user, BuildKit `.m2` cache mount), built from the repo root so the parent POM is in context; plus an 8th for `angular-ui` (`node:22-alpine` build → nginx with SPA fallback). Required adding `spring-boot-maven-plugin` to every module — `mvn package` previously produced non-executable thin jars
- [x] Root `docker-compose.yml`: Postgres 16 (one DB per service via `docker/postgres-init.sql`), Kafka 3.9 single-node KRaft, all 7 services, Angular UI on `:4200` — plus Mailpit (`:8025`) so order emails are inspectable and Jaeger (`:16686`) for the Day-12 tracing; infra host ports shifted (15432/29092) to coexist with native local installs
- [x] Replace hardcoded `localhost` DB/Kafka hosts in each `application.yml` with env vars for compose networking — `DB_HOST`/`DB_PORT`/`DB_USERNAME`/`DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, and `EUREKA_SERVER_URL`, all defaulting to the previous localhost values
- [x] *(found during compose smoke test)* Fixed cross-service Kafka poison pill: consumers rejected order-service's `__TypeId__` header and looped forever on the record — consumers now use `ErrorHandlingDeserializer` + `spring.json.use.type.headers: false` + local `default.type` (see CHANGELOG). End-to-end verified in the compose stack: register → login → create/stock product → order → stock reserved via Kafka → confirmation email in Mailpit → traces for all 6 services in Jaeger

### Day 14 — CI Pipeline
- [x] `.github/workflows/build.yml`: matrix build + `mvn test` per service on PR — 7-way service matrix (`mvn -B test -pl <service> -am`, Temurin 25, Maven cache); unit tests only, Testcontainers ITs stay local behind `mvn verify` since they need Docker
- [x] `.github/workflows/lint.yml`: Angular lint/test on PR — required adding lint to the UI first: `ng add angular-eslint` + the `@angular/core:inject` migration schematic to fix the 21 resulting `prefer-inject` errors (all 46 UI tests still green)
- [x] Fail PRs on test failure; publish build status badge in `README.md` — both workflows fail their check on any test/lint failure and also run on `main` pushes so the two README badges stay live; to make the checks merge-blocking, enable branch protection on `main` requiring the `Build` and `Angular` checks (GitHub settings, can't be done from the repo)

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
