# ecommerce-platform

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

## Prerequisites

Install and have running before starting any service:

- [JDK 25](https://openjdk.org/)
- [Maven 3.9+](https://maven.apache.org/)
- [PostgreSQL 15+](https://www.postgresql.org/) — running on `localhost:5432`
- [Apache Kafka](https://kafka.apache.org/) — running on `localhost:9092`
- [Node.js + Angular CLI](https://angular.io/cli) — for the frontend (optional)

---

## Database Setup

Create one database per service in PostgreSQL. The services use `postgres` / `postgres` as the default credentials (override via environment variables in production).

```sql
CREATE DATABASE authdb;
CREATE DATABASE productdb;
CREATE DATABASE orderdb;
CREATE DATABASE inventorydb;
```

Schema is managed automatically by Hibernate (`ddl-auto: update`) on first startup.

---

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | Yes | `change-this-to-a-long-random-secret-key-min-32-bytes` | Shared JWT signing secret — **must be overridden in production** |
| `SPRING_DATASOURCE_USERNAME` | No | `postgres` | PostgreSQL username |
| `SPRING_DATASOURCE_PASSWORD` | No | `postgres` | PostgreSQL password |
| `MAIL_HOST` | No | `localhost` | SMTP host used by `notification-service` for order confirmation emails |
| `MAIL_PORT` | No | `1025` | SMTP port — defaults to a local dev SMTP catcher (e.g. [MailHog](https://github.com/mailhog/MailHog)/[Mailpit](https://github.com/axllent/mailpit)) so you can inspect sent mail without a real provider |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | No | *(empty)* | SMTP credentials — set these along with `MAIL_HOST`/`MAIL_PORT` when pointing at a real provider (SendGrid, SES, etc.) |
| `NOTIFICATION_CHANNEL` | No | `EMAIL` | Which `NotificationStrategy` (`EMAIL`, `SMS`, `PUSH`) handles order confirmations |

Set `JWT_SECRET` before starting `api-gateway` and `auth-service`:

```bash
# Linux / macOS
export JWT_SECRET=your-secure-random-secret-min-32-characters

# Windows (PowerShell)
$env:JWT_SECRET = "your-secure-random-secret-min-32-characters"
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
