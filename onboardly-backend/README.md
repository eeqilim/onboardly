# onboardly-backend

Backend for **Onboardly**, an employee onboarding, visa, and housing management
platform. It's a Spring Boot microservices system built on Spring Cloud
(Eureka service discovery and an API Gateway), with MySQL, MongoDB, and Kafka
as the backing infrastructure.

## Tech stack

- **Java 21**, **Spring Boot 3.5.x**, **Maven** (each service has its own wrapper)
- **Spring Cloud**: Netflix Eureka, Gateway (WebFlux), OpenFeign
- **Data**: MySQL, MongoDB
- **Messaging**: Apache Kafka (+ Zookeeper)
- **Auth**: JWT (shared signing key across services)
- **Storage**: AWS S3 (employee documents)
- **Docs**: springdoc OpenAPI / Swagger UI

## Services

| Service               | Port | Responsibility                                     | Backing stores                |
| --------------------- | ---- | -------------------------------------------------- | ----------------------------- |
| `discovery`           | 8761 | Eureka service registry                            | —                             |
| `api-gateway`         | 8080 | Edge routing, CORS, single entry point for clients | —                             |
| `auth-server`         | 8081 | Login, registration, JWT issuance                  | MySQL, Kafka (producer)       |
| `employee-service`    | 8082 | Employee profiles, onboarding, document upload     | MongoDB, S3, Kafka (producer) |
| `application-service` | 8083 | Onboarding & visa-status applications, HR review   | MySQL, Kafka (prod/consumer)  |
| `housing-service`     | 8084 | Houses, facilities, facility reports, landlords    | MySQL                         |
| `email-service`       | 8085 | Sends emails in response to Kafka events           | Kafka (consumer), SMTP        |

### Gateway routes

The gateway is the only service browser clients talk to. It proxies:

| Path prefix       | Target service                                   |
| ----------------- | ------------------------------------------------ |
| `/auth/**`        | auth-server (`http://localhost:8081`)            |
| `/employee/**`    | employee-service (`lb://EMPLOYEE-SERVICE`)       |
| `/application/**` | application-service (`lb://APPLICATION-SERVICE`) |
| `/housing/**`     | housing-service (`lb://HOUSING-SERVICE`)         |

CORS is configured for the frontend at `http://localhost:5173` and
`http://localhost:3000`.

## Architecture

```
                       ┌──────────────┐
  Browser  ──────────► │ api-gateway  │ :8080
  (:5173 / :3000)      └──────┬───────┘
                              │  (discovery via Eureka :8761)
        ┌─────────────┬───────┼────────────┬──────────────┐
        ▼             ▼       ▼            ▼              ▼
   auth-server  employee-svc  application-svc  housing-svc
     :8081        :8082          :8083           :8084
       │            │              │
       │            │ (S3)         │
       └────────────┴───── Kafka ──┴────────► email-service :8085
                            │                       (SMTP)
        MySQL          MongoDB    Kafka topics:
                                  email-topic, visa-workflow-events
```

## Prerequisites

- JDK 21
- Docker + Docker Compose (for Kafka, Zookeeper, MongoDB)
- A reachable MySQL database (defaults to `localhost`; set `DB_URL` to point elsewhere)
- AWS credentials with access to the S3 documents bucket

## Running locally

### 1. Start infrastructure

```bash
docker compose up -d   # Zookeeper, Kafka, MongoDB
```

### 2. Start the services

Start them in dependency order, each from its own module. For example:

```bash
cd discovery       && ./mvnw spring-boot:run   # :8761  (start first)
cd api-gateway     && ./mvnw spring-boot:run   # :8080
cd auth-server     && ./mvnw spring-boot:run   # :8081
cd employee-service     && ./mvnw spring-boot:run   # :8082
cd application-service  && ./mvnw spring-boot:run   # :8083
cd housing-service      && ./mvnw spring-boot:run   # :8084
cd email-service        && ./mvnw spring-boot:run   # :8085
```

> Start `discovery` first so the other services can register with Eureka.
> The gateway and downstream services discover each other through the
> registry, so they can be started in any order once Eureka is up.

### 3. Build / test a single service

```bash
cd <service>
./mvnw clean package      # build (runs tests)
./mvnw test               # tests only
```

## API documentation

Each web-facing service exposes Swagger UI and the OpenAPI spec:

- Swagger UI: `http://localhost:<port>/swagger-ui.html`
- OpenAPI JSON: `http://localhost:<port>/v3/api-docs`

## Configuration & secrets

Service config lives in each module's
`src/main/resources/application.properties`. Secrets are read from
**environment variables** using the `${VAR:default}` form — the committed
defaults target local development (localhost database, empty credentials, a
placeholder JWT key). Override them via env vars in any shared/production
environment, and never commit real credentials.

Environment variables by service:

```bash
# Database (MySQL) — auth-server, application-service, housing-service
export DB_URL="jdbc:mysql://<host>:3306/onboardly?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/New_York"
export DB_USERNAME="..."
export DB_PASSWORD="..."

# JWT — must be identical across all services
export JWT_SECRET_KEY="..."

# AWS / S3 — employee-service
export AWS_REGION="us-east-1"
export AWS_ACCESS_KEY="..."
export AWS_SECRET_KEY="..."
export S3_BUCKET="onboardly-employee-documents-dev"
export SSN_ENCRYPTION_KEY="..."      # 32-byte Base64 key

# Email / SMTP — email-service
export MAIL_USERNAME="..."
export MAIL_PASSWORD="..."           # SMTP app password
```

The JWT signing key must be identical across all services for tokens issued by
`auth-server` to validate elsewhere.

## Repository layout

```
onboardly-backend/
├── discovery/            # Eureka server
├── api-gateway/          # Spring Cloud Gateway
├── auth-server/          # Authentication & JWT
├── employee-service/     # Employee profiles, documents
├── application-service/  # Onboarding & visa applications
├── housing-service/      # Housing, facilities, reports
├── email-service/        # Kafka-driven email sender
└── docker-compose.yml    # Kafka, Zookeeper, MongoDB
```
