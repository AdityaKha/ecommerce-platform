# Changelog

All notable changes to this project will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Product listing page in `angular-ui` (`/products`), fetching from `product-service` via the gateway and rendering a basic table (name, SKU, category, price).
- "View Products" link on the home screen.
- `api-gateway`: in-memory per-IP rate limiting (`RateLimitingFilter`) and a centralized JSON error response for downstream 5xx/timeouts (`GlobalErrorHandlingFilter`); gateway now forwards authenticated roles via `X-Auth-Roles`.
- `auth-service`: role-based authorization (`@PreAuthorize`) protecting a new admin-only `GET /api/auth/users`, plus `POST /api/auth/validate` for token validation.
- `product-service`: `GET /api/products/search` supporting name/category/price-range filtering via JPA Specifications.
- `order-service`: order status lifecycle (`CREATED → CONFIRMED → SHIPPED → DELIVERED`, `CANCELLED`) with `PATCH /api/orders/{id}/status`, and a synchronous inventory-availability check against `inventory-service` before an order is created.
- `inventory-service`: idempotent Kafka consumption via `ProcessedOrderEvent` tracking, and stock reservation now rejects (409) instead of silently clamping when insufficient.
- `notification-service`: real Strategy pattern (`NotificationStrategy` + `EmailNotificationStrategy`/`SmsNotificationStrategy`/`PushNotificationStrategy`) replacing the log-only stub, selected at runtime via `notification.channel`; `EmailNotificationStrategy` sends real SMTP mail through `JavaMailSender`, configurable via `MAIL_HOST`/`MAIL_PORT`/`MAIL_USERNAME`/`MAIL_PASSWORD`.
- `order-service`: `OrderRequest`/`Order`/`OrderResponse` and the `order.created` event now carry an optional `customerEmail`, giving notification-service a real recipient address (additive, non-breaking change to the event contract).
- `angular-ui`: functional `authGuard` protecting `/home`, `/products`, `/cart`, `/checkout`, and `/orders` — unauthenticated users are redirected to `/auth/login`.
- `angular-ui`: signal-based `CartService` (add/update-quantity/remove/clear, local in-memory state) and a `CartComponent` (`/cart`) with an "Add to Cart" action wired into `ProductListComponent`.
- `angular-ui`: `CheckoutComponent` (`/checkout`) and `OrderService`, submitting cart contents as an `OrderRequest` to `POST /api/orders`; clears the cart and redirects to order history on success.
- `angular-ui`: `OrderHistoryComponent` (`/orders`) listing the signed-in user's past orders via `GET /api/orders`.
- Flyway database migrations for `auth-service`, `product-service`, `order-service`, and `inventory-service`: baseline `V1__init.sql` per service capturing the current Hibernate-generated schema (`users`/`user_roles`, `products`, `orders`/`order_items`, `inventory_items`/`processed_order_events`); `spring.jpa.hibernate.ddl-auto` flipped from `update` to `validate` in all four, with `spring.flyway.baseline-on-migrate: true` so existing dev databases (already schema'd via `ddl-auto: update`) baseline at `V1` instead of failing.
- JUnit5/Mockito unit tests for domain/service logic across all 7 backend services (auth, product, order, inventory, notification, gateway filters, and a discovery-server context-load smoke test).
- Testcontainers-backed integration tests (`*IT.java`, run via `mvn verify`, Failsafe-bound) for the Postgres-backed repository layers in `auth-service`, `product-service`, `order-service`, and `inventory-service`, plus real Kafka producer/consumer coverage for the `order-events` topic (`order-service` → `inventory-service`).
- Angular unit tests (Jasmine/Karma) for `AuthService`, `ProductService`, `CartService`, `CartComponent`, `CheckoutComponent`, and `OrderHistoryComponent`.
- Local `pre-push` git hook (`.githooks/pre-push`, enabled via `git config core.hooksPath .githooks`) running `mvn test` and `ng test` before every push.
- Spring Cloud Contract tests for the `order-service` ↔ `inventory-service` HTTP contract (`GET /api/inventory/{productId}`, 200 and 404 cases): producer-side verification tests generated from YAML contracts in `inventory-service/src/test/resources/contracts`, and a consumer-side stub-runner test in `order-service` running `InventoryClient` against WireMock stubs generated from those same contracts at runtime (no stub jar installation needed).
- Resilience4j circuit breakers on all four `api-gateway` routes (`CircuitBreaker` filter per route, 50% failure rate over a 10-call window opens the circuit for 10s) with a shared `/fallback` endpoint returning a uniform 503 JSON body.
- `order-service`: the synchronous inventory-availability check in `InventoryClient` is now wrapped in a Resilience4j circuit breaker (`inventory`); when inventory-service is unreachable or the circuit is open, order creation fails fast with 503 (`InventoryUnavailableException`) instead of 500. The inventory base URL is now externalized as `inventory.service.url`.
- Kafka consumer retry with exponential backoff (1s initial, ×2, max 10s, 4 retries) in `inventory-service` and `notification-service` via a `DefaultErrorHandler` bean; non-recoverable business failures (insufficient stock, missing inventory record) are dropped after a single attempt instead of being redelivered.
- Actuator `GET /actuator/health` and `GET /actuator/info` exposed on all 7 services, with an `info.app` payload (name + description) per service.
- Structured JSON logging via Spring Boot's native ECS format, activated by the new `json-logs` profile (`SPRING_PROFILES_ACTIVE=json-logs`); default console output now carries `[application,traceId,spanId]` correlation on every line.
- Distributed tracing with Micrometer Tracing + OpenTelemetry (`micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`) on the 6 request-path services, exporting OTLP/HTTP to Jaeger (`OTLP_TRACING_ENDPOINT`, default `http://localhost:4318/v1/traces`; sampling via `TRACING_SAMPLING_PROBABILITY`, default 1.0). Traces propagate gateway → services over W3C `traceparent` and across Kafka via producer/listener observations (`spring.kafka.template.observation-enabled` in `order-service`, `spring.kafka.listener.observation-enabled` in `inventory-service`/`notification-service`). `discovery-server` is intentionally untraced to avoid heartbeat span noise.

- Dockerfiles for all 7 backend services: multi-stage builds (`maven:3.9-eclipse-temurin-25` → `eclipse-temurin:25-jre`) run from the repo root so the parent POM is in context, use a BuildKit `/root/.m2` cache mount, and run as a non-root user.
- Dockerfile for `angular-ui`: `node:22-alpine` production build served by `nginx:1.27-alpine` with SPA-fallback routing (`nginx.conf`).
- Root `docker-compose.yml` running the full stack: Postgres 16 (one database per service, created by `docker/postgres-init.sql`), Kafka 3.9 in single-node KRaft mode, Mailpit (SMTP catcher, UI on `:8025`), Jaeger (UI on `:16686`), all 7 services, and the Angular UI on `:4200`. Infra host ports are shifted (`15432`, `29092`) so the stack coexists with locally installed Postgres/Kafka; startup is ordered via healthchecks (`pg_isready`, Kafka API probe, TCP probe on Eureka).
- `spring-boot-maven-plugin` added to every service module — `mvn package` now produces executable (repackaged) fat jars, which the Dockerfiles rely on; previously only `spring-boot:run` worked.
- GitHub Actions CI: `.github/workflows/build.yml` runs `mvn test` for each of the 7 services in a build matrix (Temurin 25, Maven dependency cache), and `.github/workflows/lint.yml` runs `ng lint` + headless `ng test` for `angular-ui` — both on every PR and on pushes to `main`; build-status badges added to `README.md`. Testcontainers integration tests (`*IT.java`) remain local-only under `mvn verify`.
- `angular-ui`: ESLint via `angular-eslint` (`ng add` scaffold: `eslint.config.js`, `lint` architect target, `npm run lint` script) so the UI has an enforceable lint gate in CI.
- Demo seed data via Flyway: `product-service` `V2__seed_products.sql` (12 products across Audio/Electronics/Accessories/Home & Kitchen/Fitness, explicit ids 1–12 with sequence advance) and `inventory-service` `V2__seed_inventory.sql` (matching stock levels per product id).
- `angular-ui`: shared app shell in `AppComponent` — sticky "Emporia" toolbar with Products/Orders nav, live cart badge (`MatBadge`), signed-in username, and sign-out; shown only when authenticated.

### Changed
- Hardcoded `localhost` infrastructure hosts in each service's `application.yml` are now environment-variable overrides with unchanged local defaults: `DB_HOST`/`DB_PORT`/`DB_USERNAME`/`DB_PASSWORD` (auth/product/order/inventory), `KAFKA_BOOTSTRAP_SERVERS` (order/inventory/notification), and `EUREKA_SERVER_URL` (all 6 Eureka clients) — required for Docker Compose networking.
- `order-service`: downstream inventory failures during order creation now return 503 with a "retry later" message instead of propagating a 500.
- `angular-ui`: constructor parameter injection replaced with `inject()` across services and components (Angular's `@angular/core:inject` migration schematic), satisfying the `@angular-eslint/prefer-inject` rule now enforced in CI.
- `angular-ui`: product list redesigned from a plain table to a responsive card grid with per-category gradient banners/icons, category chips, description clamps, and an "Add to cart" snackbar with a View Cart action.
- `angular-ui`: home page redesigned as a hero banner plus quick-link cards (products / cart with live count / orders); cart, checkout, and order-history pages restyled with Material cards, status chips, empty states, and loading spinners; login/register pages get a branded gradient backdrop.
- `inventory-service`: `OrderEventConsumerIT` now clears `inventory_items`/`processed_order_events` before seeding its fixture, since the V2 seed migration also inserts `product_id` 1.

### Security
- Removed all checked-in secret defaults: `JWT_SECRET`, `INTERNAL_TOKEN`, and `DB_PASSWORD` no longer have fallback values in any `application.yml` or in `docker-compose.yml`. Services (and Compose, via `${VAR:?...}`) now fail fast at startup when a secret is missing. Added `.env.example` (gitignored `.env` for local/Compose use), and updated the README/ARCHITECTURE env-var docs accordingly. Test-only placeholder values live in each service's `src/test/resources/application.properties`.
- Service-to-service authentication via a shared `INTERNAL_TOKEN`: `api-gateway`'s new `InternalTokenRelayFilter` (a) strips any client-supplied `X-Auth-Subject`/`X-Auth-Roles`/`X-Internal-Token` headers before the JWT filter stamps verified ones — closing an identity-spoofing hole where a client could forge roles — and (b) stamps `X-Internal-Token` on every proxied request. `auth`/`product`/`order`/`inventory` each add an `InternalTokenFilter` (constant-time compare, actuator exempt) that rejects any request not carrying the token, so the services can no longer be reached by bypassing the gateway. `order-service`'s `InventoryClient` sends the token on its outbound availability check.
- **Broken access control fixes** (found by the Day-15 security review; all three resource services authenticated requests but never authorized them):
  - `order-service`: `GET /api/orders` previously returned **every** customer's orders (including other customers' email/PII); `GET /api/orders/{id}` and `PATCH /api/orders/{id}/status` acted on any order; `POST /api/orders` trusted the `customerUsername` in the request body. Now the caller is derived from the gateway-verified `X-Auth-Subject`: list is scoped to the caller (admins see all via `X-Auth-Roles`), single-order read/status-change require ownership or `ROLE_ADMIN` (else 403), and created orders are always attributed to the authenticated user regardless of the body.
  - `product-service`: `POST`/`PUT`/`DELETE /api/products/**` were reachable by any authenticated customer; they now require `ROLE_ADMIN`. `GET`s remain open to authenticated users.
  - `inventory-service`: `PUT /api/inventory/{productId}/adjust` (arbitrary stock mutation) now requires `ROLE_ADMIN`; `GET` remains open (order-service's availability check needs it).

### Fixed
- Cross-service Kafka deserialization failure surfaced by the first real containerized run: `order-service` publishes `order.created` with a `__TypeId__` header naming *its* event class, which `inventory-service`/`notification-service` (each with their own event DTO and `spring.json.trusted.packages` limited to their own package) rejected — and since the plain `JsonDeserializer` fails inside `poll()`, the container looped on the record forever without committing (poison pill; stock never reserved, no email sent). Consumers now use `ErrorHandlingDeserializer` → `JsonDeserializer` with `spring.json.use.type.headers: false` and `spring.json.value.default.type` pinned to the local event class, so payloads bind by schema rather than by producer class name and undeserializable records are logged and dropped by the existing `DefaultErrorHandler` instead of wedging the consumer. (The Day-10 Kafka integration test missed this because it produced the event using the consumer's own class, making the type header trusted by construction.)
- `order-service`: the load-balanced `RestClient.Builder` is now built through `RestClientBuilderConfigurer`, so Boot's observation instrumentation applies to `InventoryClient` calls — without this, order → inventory HTTP hops would neither create client spans nor propagate the trace context.

### Planned
- Inter-service Kafka/RabbitMQ event bus wiring.
- JWT validation filter in `api-gateway`.

---

## [0.1.0] - 2026-06-27

### Added
- Initial monorepo scaffold with seven Maven modules: `api-gateway`, `auth-service`, `discovery-server`, `inventory-service`, `notification-service`, `order-service`, `product-service`.
- Basic Spring Boot configuration and parent POM.
- `ARCHITECTURE.md` and `CLAUDE.md` documentation.
