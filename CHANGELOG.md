# Changelog

All notable changes to this project will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Product listing page in `angular-ui` (`/products`), fetching from `product-service` via the gateway and rendering a basic table (name, SKU, category, price).
- "View Products" link on the home screen.

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
