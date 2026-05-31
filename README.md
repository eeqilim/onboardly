# Onboardly

Onboardly is an employee onboarding, visa, and housing management platform. It
guides new hires from an HR invitation through registration, onboarding forms
and document uploads, HR review, and ongoing tasks such as STEM-OPT visa
workflows, housing assignments, and facility reports.

This repository is a monorepo with two parts:

| Directory                                    | What it is                                         |
| -------------------------------------------- | -------------------------------------------------- |
| [`onboardly-backend/`](onboardly-backend/)   | Spring Boot / Spring Cloud microservices and infra |
| [`onboardly-frontend/`](onboardly-frontend/) | React + Vite employee and HR portal                |

## Architecture

```
   ┌─────────────────────────────┐
   │  onboardly-frontend         │   React + Vite, :5173
   │  (employee & HR portal)     │
   └──────────────┬──────────────┘
                  │  HTTP + JWT
                  ▼
   ┌─────────────────────────────┐
   │  onboardly-backend          │   Spring Cloud microservices
   │    api-gateway (:8080)      │   → auth, employee, application,
   │    single entry point       │     housing, email, discovery
   └──────────────┬──────────────┘
                  │
        MySQL · MongoDB · S3 · Kafka
```

The browser only ever talks to the API Gateway, which routes to the backend
microservices.

See the [backend README](onboardly-backend/README.md) for the
full service fan-out, Eureka discovery, and Kafka topics.

## Tech stack

**Backend** — Java 21, Spring Boot 3.5, Spring Cloud (Eureka, Gateway,
OpenFeign), MySQL, MongoDB, Apache Kafka, AWS S3, JWT auth, springdoc/Swagger.

**Frontend** — React 18, Vite, React Router v7, Ant Design v6, Axios.

## Prerequisites

- JDK 21
- Node.js 20.19+ (Vite 8) and npm
- Docker + Docker Compose (Kafka, Zookeeper, MongoDB)
- A reachable MySQL instance
- AWS S3 credentials (for document upload)
- SMTP credentials, e.g. a Gmail app password (for email)

## Quick start

### 1. Database setup

The MySQL services (`auth-server`, `application-service`, `housing-service`) do
not create their tables automatically, since they run with `ddl-auto=none` and
no Flyway. You must load the schema yourself before starting them. You also need
your own running MySQL instance, because the `docker compose` setup below starts
only Kafka, Zookeeper, and MongoDB.

- Run [`AuthServer.sql`](onboardly-backend/auth-server/AuthServer.sql) to create
  the auth tables and seed the dev accounts below.
- MongoDB (`employee_db`, used by `employee-service`) is created automatically.

> **Caveat:** `AuthServer.sql` creates a database named `auth_db`, while the
> services default `DB_URL` to a database named `onboardly`. These names must
> match, so either run the script against `onboardly` or set `DB_URL` to point
> at `auth_db`.

### 2. Backend

```bash
cd onboardly-backend

# Start infrastructure: Zookeeper, Kafka, MongoDB
docker compose up -d

# Then start each service (discovery first, so the rest can register with Eureka).
```

See the [backend README](onboardly-backend/README.md) for the full per-service
startup commands, ports, service breakdown, gateway routes, and required
environment variables (database, JWT key, AWS/S3, SMTP).

### 3. Frontend

```bash
cd onboardly-frontend
npm install
npm run dev      # http://localhost:5173
```

The frontend's Axios client calls the API Gateway at `http://localhost:8080`.

See the [frontend README](onboardly-frontend/README.md) for routes, user flows,
and the auth lifecycle.

## Roles & dev login

The app has two roles, `ROLE_HR` and `ROLE_EMPLOYEE`, which gate the routes a
user can reach. `AuthServer.sql` seeds the following **local-only** accounts (do
not use these outside development):

| Username                  | Password   | Role                   |
| ------------------------- | ---------- | ---------------------- |
| `hradmin`                 | `admin123` | HR (also has employee) |
| `alice`, `bob`, `charlie` | `123456`   | Employee               |

## How it fits together

1. **HR invites an employee** from the portal; the backend emails a registration
   link.
2. **The employee registers and logs in**, then completes onboarding forms and
   uploads required documents (W-4, I-9, etc.) stored in S3.
3. **HR reviews and approves** the application.
4. **Approved employees** access ongoing features: profile management, the
   STEM-OPT visa workflow, housing assignments, and facility reports.

## Configuration & secrets

Each backend module reads config from `src/main/resources/application.properties`,
with secrets pulled from environment variables (`${VAR:default}`). The committed
defaults are for local development only. Override them in any shared or
production environment, and never commit real credentials. The JWT signing key
must be identical across all services.

See the [backend README](onboardly-backend/README.md#configuration--secrets)
for the full list.

## API documentation

Each web-facing backend service exposes Swagger UI and an OpenAPI spec:

- Swagger UI: `http://localhost:<port>/swagger-ui.html`
- OpenAPI JSON: `http://localhost:<port>/v3/api-docs`
