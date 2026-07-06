# CLAUDE.md

Guidance for Claude (or any AI assistant) when working with this repository.

## Repository Overview

Monorepo-style Java microservices built with Spring Boot and Maven:

| Service | Responsibility |
|---|---|
| `api-gateway` | Single entry point; routing, auth delegation, rate limiting |
| `auth-service` | JWT issuance and validation, user/role management |
| `discovery-server` | Service registry (Eureka) for discovery and health checks |
| `product-service` | Product catalog and pricing |
| `inventory-service` | Stock levels and availability |
| `order-service` | Order lifecycle management |
| `notification-service` | Async notifications (email, SMS, push) |

- Full architecture details are documented in [ARCHITECTURE.md](ARCHITECTURE.md).
- All changes, releases, and version history are tracked in [CHANGELOG.md](CHANGELOG.md).

## Build & Test Commands

```bash
# Build all services (skip tests for speed)
mvn -DskipTests package

# Build a single service
mvn -DskipTests package -pl auth-service

# Run tests
mvn test

# Run tests for one service
mvn test -pl auth-service -am

# Run Testcontainers-backed integration tests (requires Docker running)
mvn verify

# Run Angular unit tests
cd angular-ui && npm test -- --watch=false --browsers=ChromeHeadless

# Run a single service locally
mvn spring-boot:run -pl auth-service
```

`mvn test` runs only fast unit tests (Surefire, `*Test.java`, no Docker needed).
Testcontainers-based repository/Kafka integration tests are named `*IT.java`
and only run under `mvn verify` (Failsafe), since they need Docker.

A local pre-push hook runs both `mvn test` and the Angular test suite. Enable
it once per clone with:

```bash
git config core.hooksPath .githooks
```

## Safety & Constraints

- Do not modify production credentials or secrets — use non-sensitive placeholder values only.
- Prefer minimal, focused edits over broad refactors.
- Do not introduce breaking API changes without a migration plan.
- Update [CHANGELOG.md](CHANGELOG.md) when adding, changing, or removing functionality.

## Best Practices

### Design Patterns
This codebase applies design patterns purposefully and efficiently:
- **Gateway Pattern** — `api-gateway` is the single entry point; all cross-cutting concerns (auth, rate limiting, CORS) live here, not in individual services.
- **Service Registry / Client-Side Discovery** — services register with `discovery-server` (Eureka); consumers resolve addresses dynamically rather than using hardcoded URLs.
- **Chain of Responsibility** — Spring Security filter chains handle authentication and authorization in ordered, composable stages.
- **Strategy Pattern** — notification delivery (email, SMS, push) is abstracted behind a common interface; the correct strategy is selected at runtime.
- **Event-Driven / Observer Pattern** — domain events (`order.created`, `inventory.updated`) decouple producers from consumers via a message broker.
- **Repository Pattern** — each service uses Spring Data repositories to isolate domain logic from persistence concerns.
- **DTO Pattern** — request/response objects are separate from domain entities; no JPA entities leak across service boundaries.
- **Circuit Breaker Pattern** — use Resilience4j to prevent cascading failures on downstream service calls.

### Spring Boot Microservices Best Practices
- **Single Responsibility** — each service owns exactly one bounded context and its own database schema.
- **API-first design** — define contracts (OpenAPI/Swagger) before implementing endpoints.
- **Externalized configuration** — no hardcoded values; use `application.yml` with Spring Cloud Config or environment variables for secrets.
- **Health & readiness probes** — expose `/actuator/health` and `/actuator/info` on every service.
- **Structured logging** — use SLF4J + Logback with JSON output and include `traceId`/`spanId` for distributed tracing correlation.
- **Idempotent consumers** — message consumers must handle duplicate delivery gracefully (store processed event IDs).
- **Versioned APIs** — prefix REST endpoints with `/api/v1/...` to allow non-breaking evolution.
- **Database migrations** — all schema changes go through Flyway or Liquibase; never mutate the schema manually.
- **Fail fast** — validate inputs at service boundaries using Bean Validation (`@Valid`); never pass invalid state deeper.
- **Test pyramid** — unit tests for domain logic, integration tests with Testcontainers for persistence, contract tests (Spring Cloud Contract) for inter-service APIs.

## Useful Prompts

- "Summarize the architecture and main modules of this repo."
- "Find where user authentication is implemented and list relevant classes."
- "Create a migration plan to upgrade Spring Boot to 3.2 with minimal breaking changes."
- "Where is JWT validation performed and which filter handles it?"
- "Which design patterns are used in the order-service?"
