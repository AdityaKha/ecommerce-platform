# ecommerce-platform

[![Build](https://github.com/AdityaKha/ecommerce-platform/actions/workflows/build.yml/badge.svg)](https://github.com/AdityaKha/ecommerce-platform/actions/workflows/build.yml)
[![Angular](https://github.com/AdityaKha/ecommerce-platform/actions/workflows/lint.yml/badge.svg)](https://github.com/AdityaKha/ecommerce-platform/actions/workflows/lint.yml)

A microservices-based e-commerce platform built with Java Spring Boot, Angular, Apache Kafka, and PostgreSQL.

See [ARCHITECTURE.md](ARCHITECTURE.md) for a full architectural overview and [CHANGELOG.md](CHANGELOG.md) for version history.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.5.12, Spring Cloud 2025.0.0 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Auth | JWT (JJWT 0.12.6) |
| Messaging | Apache Kafka |
| Database | PostgreSQL |
| Frontend | Angular |
| Build | Maven |

---

## Quick Start (Docker Compose)

The fastest way to run the whole platform — no local JDK, Postgres, Kafka, or Node required, only [Docker](https://www.docker.com/):

```bash
cp .env.example .env   # then fill in JWT_SECRET, INTERNAL_TOKEN, DB_PASSWORD
docker compose up --build
```

Secrets have no checked-in defaults: Compose refuses to start until the three
values in `.env` are set (the file is gitignored, so they never end up in the
repo).

First build takes a while (Maven dependencies are downloaded into a build cache); subsequent builds are incremental. Once up:

| URL | What |
|---|---|
| http://localhost:4200 | Angular UI |
| http://localhost:8080 | API gateway (REST entry point) |
| http://localhost:8761 | Eureka dashboard |
| http://localhost:8025 | Mailpit — order-confirmation emails land here |
| http://localhost:16686 | Jaeger — distributed traces |

Postgres is published on host port `15432` and Kafka on `29092` so the stack can run alongside locally installed instances on the default ports. Databases are created automatically ([docker/postgres-init.sql](docker/postgres-init.sql)) and each service applies its own Flyway migrations on startup.

---

## Prerequisites (running services natively)

Install and have running before starting any service outside Docker:

- [JDK 25](https://openjdk.org/)
- [Maven 3.9+](https://maven.apache.org/)
- [PostgreSQL 15+](https://www.postgresql.org/) — running on `localhost:5432`
- [Apache Kafka](https://kafka.apache.org/) — running on `localhost:9092`
- [Node.js + Angular CLI](https://angular.io/cli) — for the frontend (optional)

---

## Database Setup

Create one database per service in PostgreSQL. The username defaults to `postgres` (override with `DB_USERNAME`); the password has no default and must be supplied via `DB_PASSWORD`.

```sql
CREATE DATABASE authdb;
CREATE DATABASE productdb;
CREATE DATABASE orderdb;
CREATE DATABASE inventorydb;
```

Schema is managed by Flyway migrations (`V1__init.sql` per service), applied automatically on first startup. (When running via Docker Compose, database creation is handled for you.)

---

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | **Yes** | *(none — fails fast if unset)* | JWT signing secret (min 32 bytes) shared by `api-gateway` and `auth-service` |
| `INTERNAL_TOKEN` | **Yes** | *(none — fails fast if unset)* | Service-to-service shared secret: the gateway stamps it on proxied requests as `X-Internal-Token`, and auth/product/order/inventory reject requests without it |
| `DB_PASSWORD` | **Yes** | *(none — fails fast if unset)* | PostgreSQL password for the four DB-backed services |
| `DB_HOST` / `DB_PORT` | No | `localhost` / `5432` | PostgreSQL host and port (Compose sets `DB_HOST=postgres`) |
| `DB_USERNAME` | No | `postgres` | PostgreSQL username |
| `KAFKA_BOOTSTRAP_SERVERS` | No | `localhost:9092` | Kafka bootstrap servers (Compose sets `kafka:9092`) |
| `EUREKA_SERVER_URL` | No | `http://localhost:8761/eureka/` | Eureka registry URL (Compose sets `http://discovery-server:8761/eureka/`) |
| `MAIL_HOST` | No | `localhost` | SMTP host used by `notification-service` for order confirmation emails |
| `MAIL_PORT` | No | `1025` | SMTP port — defaults to a local dev SMTP catcher (e.g. [MailHog](https://github.com/mailhog/MailHog)/[Mailpit](https://github.com/axllent/mailpit)) so you can inspect sent mail without a real provider |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | No | *(empty)* | SMTP credentials — set these along with `MAIL_HOST`/`MAIL_PORT` when pointing at a real provider (SendGrid, SES, etc.) |
| `NOTIFICATION_CHANNEL` | No | `EMAIL` | Which `NotificationStrategy` (`EMAIL`, `SMS`, `PUSH`) handles order confirmations |
| `OTLP_TRACING_ENDPOINT` | No | `http://localhost:4318/v1/traces` | OTLP/HTTP endpoint traces are exported to (e.g. a Jaeger collector) |
| `TRACING_SAMPLING_PROBABILITY` | No | `1.0` | Fraction of requests traced — keep `1.0` locally, lower it in production |
| `SPRING_PROFILES_ACTIVE` | No | *(none)* | Set to `json-logs` for structured ECS-JSON console logging instead of human-readable output |

When running services natively (outside Compose), export the required secrets first — services fail fast at startup if they are missing:

```bash
# Linux / macOS
export JWT_SECRET=your-secure-random-secret-min-32-characters
export INTERNAL_TOKEN=your-secure-random-internal-token
export DB_PASSWORD=your-postgres-password

# Windows (PowerShell)
$env:JWT_SECRET = "your-secure-random-secret-min-32-characters"
$env:INTERNAL_TOKEN = "your-secure-random-internal-token"
$env:DB_PASSWORD = "your-postgres-password"
```

---

## Build

Build all services from the repository root:

```bash
mvn -DskipTests package
```

Build a single service:

```bash
mvn -DskipTests package -pl auth-service
```

Run tests:

```bash
mvn test
```

---

## Running the Services

Use the VS Code tasks defined in `.vscode/tasks.json`. Open the Command Palette (`Ctrl+Shift+P`) → **Tasks: Run Task** and choose one of:

| Shortcut | Task | Description |
|---|---|---|
| `Ctrl+Shift+B` | **Start All Services** | Starts all services in the correct dependency order, then launches the Angular UI |
| `Ctrl+Alt+S` | **Stop All Services** | Kills all running Java and Angular processes |
| `Ctrl+Alt+R` | **Restart All Services** | Runs Stop then Start in sequence |

---

## Service Ports

| Service | Port | Notes |
|---|---|---|
| `discovery-server` | 8761 | Eureka dashboard at `/` |
| `api-gateway` | 8080 | All client traffic enters here |
| `auth-service` | 8081 | |
| `product-service` | 8082 | |
| `order-service` | 8083 | Kafka producer |
| `inventory-service` | 8084 | Kafka consumer |
| `notification-service` | 8085 | Kafka consumer |

---

## API Routes

All requests go through the gateway at `http://localhost:8080`:

| Path prefix | Routed to |
|---|---|
| `/api/auth/**` | `auth-service` |
| `/api/products/**` | `product-service` |
| `/api/orders/**` | `order-service` |
| `/api/inventory/**` | `inventory-service` |

---

## Observability

### Health & info endpoints

Every service exposes `GET /actuator/health` and `GET /actuator/info` on its own
port (they are not routed through the gateway), e.g.
`http://localhost:8083/actuator/health` for `order-service`. Health aggregates
each service's real dependencies — DB, mail server — so a service reports
`DOWN` (HTTP 503) when a dependency it needs is unreachable.

### Structured JSON logging

By default services log human-readable lines that include
`[application,traceId,spanId]` correlation. For machine-ingestible output
(one [ECS](https://www.elastic.co/guide/en/ecs/current/index.html) JSON
document per line, with `traceId`/`spanId` as fields), activate the
`json-logs` profile:

```bash
SPRING_PROFILES_ACTIVE=json-logs mvn spring-boot:run -pl order-service
```

### Distributed tracing (OpenTelemetry → Jaeger)

All request-path services (gateway, auth, product, order, inventory,
notification) trace with Micrometer Tracing over the OpenTelemetry bridge and
export spans via OTLP/HTTP. Traces propagate end-to-end: gateway → service
HTTP hops (W3C `traceparent` header) and across Kafka (`order.created` →
inventory/notification consumers). `discovery-server` is deliberately not
traced — registry heartbeats would only add noise.

Start Jaeger locally (UI on `http://localhost:16686`, OTLP on `4318`):

```bash
docker run --rm -d --name jaeger -p 16686:16686 -p 4318:4318 jaegertracing/all-in-one:1.57
```

No configuration needed — services export to `localhost:4318` by default
(override with `OTLP_TRACING_ENDPOINT`). Every request is sampled by default;
tune with `TRACING_SAMPLING_PROBABILITY`. If no collector is running, services
work normally and just log periodic span-export warnings.
