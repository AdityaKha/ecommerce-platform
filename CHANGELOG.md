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

### Planned
- Inter-service Kafka/RabbitMQ event bus wiring.
- Flyway/Liquibase migration scripts per service.
- Docker Compose setup for local development.
- JWT validation filter in `api-gateway`.
- Unit and integration test suites per service.

---

## [0.1.0] - 2026-06-27

### Added
- Initial monorepo scaffold with seven Maven modules: `api-gateway`, `auth-service`, `discovery-server`, `inventory-service`, `notification-service`, `order-service`, `product-service`.
- Basic Spring Boot configuration and parent POM.
- `ARCHITECTURE.md` and `CLAUDE.md` documentation.
