# Architecture

## Overview

Java-based microservices e-commerce platform built with Spring Boot and Maven. Each service is independently deployable, owns its own data, and exposes REST endpoints.

**Services:** `api-gateway`, `auth-service`, `discovery-server`, `inventory-service`, `notification-service`, `order-service`, `product-service`

**Frontend:** `angular-ui`

---

## Services

### API Gateway
- Single entry point for all external clients.
- Handles request routing, JWT validation (via filter), rate limiting, and CORS.
- Delegates authentication decisions to `auth-service`.

### Discovery Server
- Service registry built on Eureka.
- All services register on startup; gateway uses it to resolve downstream URLs.

### Auth Service
- Issues and validates JWT tokens.
- Manages users, credentials, and roles.
- Exposes `/auth/login`, `/auth/register`, `/auth/validate` endpoints.

### Product Service
- Product catalog: creation, updates, search, and pricing.

### Inventory Service
- Tracks stock levels and availability per product/SKU.
- Emits `inventory.updated` events when stock changes.

### Order Service
- Manages the full order lifecycle (created → confirmed → shipped → delivered).
- Publishes `order.created` events consumed by Inventory and Notification services.

### Notification Service
- Consumes async events and sends email, SMS, or push notifications.

### Angular UI
- Single-page client (Angular, standalone components + Material) served on port 4200.
- Talks only to `api-gateway`; stores the JWT in `localStorage` and attaches it to requests via an HTTP interceptor.
- Implemented so far: login, registration, and a basic product listing page (`/products`) reading from `product-service`.

---

## Inter-Service Communication

| Pattern | Used For |
|---|---|
| Synchronous REST | Query-style calls between services (e.g., Order → Inventory check) |
| Async messaging (Kafka / RabbitMQ) | Events: `order.created`, `inventory.updated`, `notification.send` |

---

## Data & Persistence

- Each service owns its own database/schema (no shared tables).
- Schema changes managed via Flyway or Liquibase migrations.
- Recommended databases: PostgreSQL for transactional services, Redis for caching/sessions.

---

## Security

- JWTs issued by `auth-service`; validated at the gateway via a Spring Cloud Gateway filter. The gateway strips client-supplied identity headers and forwards the verified subject/roles as `X-Auth-Subject`/`X-Auth-Roles`.
- Service-to-service calls authenticated with a shared secret: the gateway (and internal clients like order→inventory) stamp `X-Internal-Token` on every call, and each service rejects requests without it, so services cannot be reached by bypassing the gateway. Upgrade path for production is mutual TLS or a service mesh (Istio/Linkerd).
- Secrets managed via environment variables or a secrets manager (Vault / AWS Secrets Manager); `JWT_SECRET`, `INTERNAL_TOKEN`, and `DB_PASSWORD` have no checked-in defaults — services fail fast when they are missing.

---

## Observability

| Concern | Tooling |
|---|---|
| Centralized logging | ELK / EFK stack |
| Metrics | Prometheus + Grafana |
| Distributed tracing | OpenTelemetry + Jaeger |
| Health checks | Spring Actuator `/actuator/health` |

---

## Deployment

- Each service is containerized with Docker.
- Orchestrated via Kubernetes (or equivalent container platform).
- Environment-specific config externalized via Spring Cloud Config Server or K8s ConfigMaps/Secrets.

---

## References

- [CLAUDE.md](CLAUDE.md) — build commands and contribution guide
- [CHANGELOG.md](CHANGELOG.md) — version history
