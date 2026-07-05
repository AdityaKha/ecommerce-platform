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

### Planned
- Inter-service Kafka/RabbitMQ event bus wiring.
- Docker Compose setup for local development.
- JWT validation filter in `api-gateway`.
- Unit and integration test suites per service.

---

## [0.1.0] - 2026-06-27

### Added
- Initial monorepo scaffold with seven Maven modules: `api-gateway`, `auth-service`, `discovery-server`, `inventory-service`, `notification-service`, `order-service`, `product-service`.
- Basic Spring Boot configuration and parent POM.
- `ARCHITECTURE.md` and `CLAUDE.md` documentation.
