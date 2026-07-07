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

### Changed
- `order-service`: downstream inventory failures during order creation now return 503 with a "retry later" message instead of propagating a 500.

### Planned
- Inter-service Kafka/RabbitMQ event bus wiring.
- Docker Compose setup for local development.
- JWT validation filter in `api-gateway`.

---

## [0.1.0] - 2026-06-27

### Added
- Initial monorepo scaffold with seven Maven modules: `api-gateway`, `auth-service`, `discovery-server`, `inventory-service`, `notification-service`, `order-service`, `product-service`.
- Basic Spring Boot configuration and parent POM.
- `ARCHITECTURE.md` and `CLAUDE.md` documentation.
