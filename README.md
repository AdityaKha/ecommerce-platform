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

Services must be started in this order due to Eureka registration dependencies. Run each in a **separate terminal**.

> **Note:** `mvn` is not on the system PATH. Use the full path to `mvn.cmd` as shown below.

### 1. Discovery Server (Eureka) — port `8761`
```powershell
cd D:\Code\ecommerce-platform\discovery-server
& "C:\Users\khand\.maven\maven-3.9.16\bin\mvn.cmd" spring-boot:run
```
Dashboard: http://localhost:8761

### 2. Auth Service — port `8081`
```powershell
cd D:\Code\ecommerce-platform\auth-service
& "C:\Users\khand\.maven\maven-3.9.16\bin\mvn.cmd" spring-boot:run
```

### 3. API Gateway — port `8080`
```powershell
cd D:\Code\ecommerce-platform\api-gateway
& "C:\Users\khand\.maven\maven-3.9.16\bin\mvn.cmd" spring-boot:run
```

### 4. Domain Services

Start these in any order after the gateway is up:

```powershell
cd D:\Code\ecommerce-platform\product-service
& "C:\Users\khand\.maven\maven-3.9.16\bin\mvn.cmd" spring-boot:run
```
```powershell
cd D:\Code\ecommerce-platform\order-service
& "C:\Users\khand\.maven\maven-3.9.16\bin\mvn.cmd" spring-boot:run
```
```powershell
cd D:\Code\ecommerce-platform\inventory-service
& "C:\Users\khand\.maven\maven-3.9.16\bin\mvn.cmd" spring-boot:run
```
```powershell
cd D:\Code\ecommerce-platform\notification-service
& "C:\Users\khand\.maven\maven-3.9.16\bin\mvn.cmd" spring-boot:run
```

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
