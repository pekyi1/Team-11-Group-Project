# ServiceHub — Internal Service Request & Ticketing System

An internal service request management platform with intelligent routing, SLA tracking, and workflow automation. Built with Spring Boot (backend API) and Next.js (frontend).

## Tech Stack

| Layer      | Technology                                  |
|------------|---------------------------------------------|
| Backend    | Java 17, Spring Boot 3.2, Spring Security   |
| Frontend   | Next.js 14, React 18, TypeScript            |
| Database   | PostgreSQL 16                               |
| Auth       | JWT (HS512) with role-based access control   |
| Migrations | Flyway (versioned SQL)                      |
| Mail       | MailHog (dev SMTP trap)                     |
| Containers | Docker Compose                              |

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│  Next.js     │────▶│  Spring Boot │────▶│  PostgreSQL  │
│  Frontend    │     │  REST API    │     │  Database    │
│  :3000       │     │  :8080       │     │  :5432       │
└─────────────┘     └──────┬──────┘     └──────────────┘
                           │
                    ┌──────▼──────┐
                    │  MailHog     │
                    │  SMTP :1025  │
                    │  UI   :8025  │
                    └─────────────┘
```

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Git

### 1. Clone and configure

```bash
git clone <repository-url>
cd 4-ServiceHub
cp .env.example .env
# Edit .env to set JWT_SECRET and DB_PASSWORD (or use defaults for dev)
```

### 2. Start all services

```bash
docker compose up --build
```

This starts 4 containers:

| Service    | URL                          | Description               |
|------------|------------------------------|---------------------------|
| Frontend   | http://localhost:3000         | Next.js web app           |
| API        | http://localhost:8080         | Spring Boot REST API      |
| MailHog    | http://localhost:8025         | Email inbox (dev only)    |
| PostgreSQL | localhost:5432               | Database                  |

### 3. Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Login as admin
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@servicehub.local","password":"Admin@Sh2026!"}'
```

## Default Users

All seed users are created by Flyway migration `V2__seed_data.sql`.

| Email                              | Password        | Role  | Department             | Location         |
|------------------------------------|-----------------|-------|------------------------|------------------|
| admin@servicehub.local             | Admin@Sh2026!   | ADMIN | —                      | Accra HQ         |
| it.agent.accra@servicehub.local    | Agent@Sh2026!   | AGENT | IT Department          | Accra HQ         |
| it.agent.takoradi@servicehub.local | Agent@Sh2026!   | AGENT | IT Department          | Takoradi Branch  |
| facilities.agent@servicehub.local  | Agent@Sh2026!   | AGENT | Facilities Management  | Accra HQ         |
| hr.agent@servicehub.local          | Agent@Sh2026!   | AGENT | Human Resources        | Accra HQ         |
| hr.agent.kumasi@servicehub.local   | Agent@Sh2026!   | AGENT | Human Resources        | Kumasi Branch    |
| user.accra@servicehub.local        | User@Sh2026!    | USER  | —                      | Accra HQ         |
| user.takoradi@servicehub.local     | User@Sh2026!    | USER  | —                      | Takoradi Branch  |

## Database Schema

Managed by Flyway migrations in `backend/src/main/resources/db/migration/`.

### Tables

| Table               | Description                                          |
|---------------------|------------------------------------------------------|
| locations           | Office locations (Accra HQ, Takoradi, Kumasi)        |
| departments         | Service departments (IT, Facilities, HR)             |
| categories          | Request categories per department (9 seeded)         |
| users               | All users (UUID primary key, role-based)             |
| service_requests    | Core tickets with SLA deadlines and soft-delete      |
| attachments         | File attachments per request (SHA-256 checksum)      |
| status_history      | Append-only status change log with transfer tracking |
| sla_policies        | Response/resolution time targets per category+priority|
| audit_logs          | Immutable audit trail (UPDATE/DELETE revoked at DB)  |
| notification_logs   | Email notification tracking with retry support       |

### Migrations

| File                          | Description                           |
|-------------------------------|---------------------------------------|
| V1__create_schema.sql         | All tables, indexes, and constraints  |
| V2__seed_data.sql             | Seed locations, departments, categories, SLA policies, users |
| V3__fix_seed_passwords.sql    | Corrected BCrypt hashes for seed users |

### Key Design Decisions

- **VARCHAR + CHECK constraints** instead of PostgreSQL custom enum types — ensures JPA `@Enumerated(EnumType.STRING)` compatibility
- **UUID primary keys** for users table (other tables use BIGSERIAL)
- **JSONB** for user notification preferences
- **Soft-delete** pattern on service_requests, attachments, departments, categories
- **Append-only audit_logs** enforced at database level (`REVOKE UPDATE, DELETE`)
- **25 indexes** covering auto-routing, workload queries, SLA compliance, and audit lookups

## API Endpoints

All endpoints are prefixed with `/api/v1`. Protected endpoints require a `Bearer` token in the `Authorization` header.

### Authentication

| Method | Endpoint               | Auth     | Description                          |
|--------|------------------------|----------|--------------------------------------|
| POST   | /api/v1/auth/register  | Public   | Register a new user                  |
| POST   | /api/v1/auth/login     | Public   | Login, returns access + refresh token|
| POST   | /api/v1/auth/refresh   | Public   | Refresh token rotation               |
| GET    | /api/v1/auth/me        | Required | Get current user profile             |

### Users

| Method | Endpoint                                     | Auth     | Description                    |
|--------|----------------------------------------------|----------|--------------------------------|
| GET    | /api/v1/users                                | ADMIN    | List all users (paginated)     |
| GET    | /api/v1/users/me                             | Required | Get current user profile       |
| GET    | /api/v1/users/me/notification-preferences    | Required | Get notification preferences   |
| PUT    | /api/v1/users/me/notification-preferences    | Required | Update notification preferences|

### Locations

| Method | Endpoint                  | Auth  | Description          |
|--------|---------------------------|-------|----------------------|
| GET    | /api/v1/locations         | Required | List all locations |
| GET    | /api/v1/locations/{id}    | Required | Get location by ID |
| POST   | /api/v1/locations         | ADMIN | Create location      |
| PUT    | /api/v1/locations/{id}    | ADMIN | Update location      |
| DELETE | /api/v1/locations/{id}    | ADMIN | Deactivate location  |

### Departments

| Method | Endpoint                                       | Auth  | Description              |
|--------|-------------------------------------------------|-------|--------------------------|
| GET    | /api/v1/departments                             | Required | List all departments  |
| GET    | /api/v1/departments/{id}                        | Required | Get department by ID  |
| POST   | /api/v1/departments                             | ADMIN | Create department      |
| PUT    | /api/v1/departments/{id}                        | ADMIN | Update department      |
| DELETE | /api/v1/departments/{id}                        | ADMIN | Delete department      |
| GET    | /api/v1/departments/{id}/agents                 | Required | List agents in dept   |
| POST   | /api/v1/departments/{id}/agents/{agentId}       | ADMIN | Assign agent to dept   |
| DELETE | /api/v1/departments/{id}/agents/{agentId}       | ADMIN | Remove agent from dept |

### Categories

| Method | Endpoint                    | Auth  | Description            |
|--------|-----------------------------|-------|------------------------|
| GET    | /api/v1/categories          | Required | List categories (optional ?departmentId=) |
| GET    | /api/v1/categories/{id}     | Required | Get category by ID     |
| POST   | /api/v1/categories          | ADMIN | Create category        |
| PUT    | /api/v1/categories/{id}     | ADMIN | Update category        |
| DELETE | /api/v1/categories/{id}     | ADMIN | Deactivate category    |

### Service Requests

| Method | Endpoint                           | Auth     | Description                    |
|--------|------------------------------------|----------|--------------------------------|
| POST   | /api/v1/requests                   | Required | Create a service request       |
| GET    | /api/v1/requests                   | Required | List requests (paginated)      |
| GET    | /api/v1/requests/{id}              | Required | Get request by ID              |
| PATCH  | /api/v1/requests/{id}/status       | Required | Update request status          |
| POST   | /api/v1/requests/{id}/transfer     | Required | Transfer to another agent      |
| PATCH  | /api/v1/requests/{id}/assign       | ADMIN    | Assign agent to request        |
| GET    | /api/v1/requests/{id}/history      | Required | Get status change history      |

### Attachments

| Method | Endpoint                                        | Auth     | Description         |
|--------|-------------------------------------------------|----------|---------------------|
| POST   | /api/v1/requests/{requestId}/attachments        | Required | Upload a file       |
| GET    | /api/v1/requests/{requestId}/attachments        | Required | List attachments    |
| GET    | /api/v1/requests/{requestId}/attachments/{id}   | Required | Download file       |
| DELETE | /api/v1/requests/{requestId}/attachments/{id}   | Required | Soft-delete file    |

### SLA Policies (Admin)

| Method | Endpoint                       | Auth  | Description          |
|--------|--------------------------------|-------|----------------------|
| GET    | /api/v1/admin/sla-policies     | ADMIN | List all policies    |
| POST   | /api/v1/admin/sla-policies     | ADMIN | Create SLA policy    |
| PUT    | /api/v1/admin/sla-policies/{id}| ADMIN | Update SLA policy    |

### Dashboard

| Method | Endpoint                       | Auth     | Description                |
|--------|--------------------------------|----------|----------------------------|
| GET    | /api/v1/dashboard/summary      | Required | Counts, SLA compliance     |

### Notification Logs (Admin)

| Method | Endpoint                                     | Auth  | Description                |
|--------|----------------------------------------------|-------|----------------------------|
| GET    | /api/v1/admin/notification-logs              | ADMIN | Paginated notification logs|
| POST   | /api/v1/admin/notification-logs/{id}/retry   | ADMIN | Retry a failed notification|

### Audit Logs (Admin)

| Method | Endpoint                       | Auth  | Description               |
|--------|--------------------------------|-------|---------------------------|
| GET    | /api/v1/audit-logs             | ADMIN | Paginated audit log query |

## Service Request Workflow

```
  ┌──────┐    auto-assign     ┌──────────┐    agent starts    ┌─────────────┐
  │ OPEN │ ─────────────────▶ │ ASSIGNED │ ────────────────▶ │ IN_PROGRESS │
  └──┬───┘                    └────┬─────┘                    └──────┬──────┘
     │                              │                                 │
     │  close (no agent)            │  unassign                      │  resolve
     ▼                              ▼                                 ▼
  ┌────────┐                    ┌──────┐      reopen          ┌──────────┐
  │ CLOSED │ ◀──────────────── │ OPEN │ ◀─────────────────── │ RESOLVED │
  └────────┘                    └──────┘                      └────┬─────┘
                                                                    │
                                                              close │
                                                                    ▼
                                                              ┌────────┐
                                                              │ CLOSED │
                                                              └────────┘
```

### Status Transitions

| From        | Allowed To                |
|-------------|---------------------------|
| OPEN        | ASSIGNED, CLOSED          |
| ASSIGNED    | IN_PROGRESS, OPEN         |
| IN_PROGRESS | RESOLVED                  |
| RESOLVED    | CLOSED, OPEN (reopen)     |
| CLOSED      | (terminal)                |

### Auto-Routing

When a request is created, the system automatically assigns an agent:
1. Find active agents in the category's department
2. Prefer agents at the **same location** as the requester
3. Among matches, pick the agent with the **fewest active tickets**
4. If no same-location agent exists, pick the least-loaded agent in the department

### SLA Tracking

Each request gets response and resolution deadlines based on its category + priority:
- **Response SLA** — met when status moves to `IN_PROGRESS`
- **Resolution SLA** — met when status moves to `RESOLVED`
- Reopening a request (`RESOLVED → OPEN`) resets SLA deadlines
- **Scheduled monitoring** (every 5 min) detects breaches and sends 75% warnings
- Email alerts sent to assigned agents on SLA warning/breach

## Project Structure

```
4-ServiceHub/
├── backend/
│   ├── src/main/java/com/servicehub/
│   │   ├── config/          # Security, JWT, CORS, CorrelationId filter
│   │   ├── controller/      # REST controllers (11 controllers)
│   │   ├── dto/
│   │   │   ├── request/     # Request DTOs (15 files)
│   │   │   └── response/    # Response DTOs (14 files)
│   │   ├── event/           # Domain events (ServiceRequestEvent)
│   │   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   │   ├── model/           # JPA entities (10 entities)
│   │   │   └── enums/       # Role, RequestStatus, Priority
│   │   ├── repository/      # Spring Data JPA repositories (11 repos)
│   │   │   └── specification/  # JPA Specifications for dynamic filtering
│   │   └── service/         # Business logic (12 services)
│   ├── src/main/resources/
│   │   ├── db/migration/    # Flyway SQL migrations (V1, V2, V3)
│   │   └── application.yml  # Main config (externalized secrets)
│   ├── mvnw                 # Maven wrapper (no Maven install needed)
│   ├── Dockerfile
│   ├── pom.xml
│   └── README.md            # Backend developer guide
├── frontend/
│   ├── src/app/             # Next.js App Router pages
│   ├── Dockerfile
│   └── package.json
├── devops/
│   └── scripts/
│       ├── init-db.sql      # PostgreSQL initialization
│       └── deploy.sh        # Deployment script
├── docker-compose.yml
├── .env.example
└── README.md
```

## Configuration

All sensitive values are externalized via environment variables. See `.env.example` for the full list.

Key variables:

| Variable                | Default                          | Description                |
|-------------------------|----------------------------------|----------------------------|
| DB_NAME                 | servicehub                       | PostgreSQL database name   |
| DB_USERNAME             | servicehub_app                   | Database user              |
| DB_PASSWORD             | changeme_use_strong_password     | Database password          |
| JWT_SECRET              | (none — must be set)             | JWT signing key            |
| JWT_EXPIRATION_MS       | 3600000 (1 hour)                 | Access token expiry        |
| JWT_REFRESH_EXPIRATION_MS | 604800000 (7 days)             | Refresh token expiry       |
| CORS_ALLOWED_ORIGINS    | http://localhost:3000            | Allowed CORS origins       |
| MAIL_HOST               | mailhog                          | SMTP server                |
| MAIL_PORT               | 1025                             | SMTP port                  |
| FILE_UPLOAD_DIR         | /data/uploads                    | Attachment storage path    |
| FILE_MAX_SIZE_MB        | 5                                | Max file upload size       |
| NEXT_PUBLIC_API_URL     | http://localhost:8080/api/v1     | API URL for frontend       |

## Docker Notes

- Images use **Debian-based** base images (not Alpine) for cross-platform compatibility with both **amd64** (Linux) and **arm64** (Apple Silicon Mac)
  - Backend: `eclipse-temurin:17-jre-jammy`
  - Frontend: `node:20-slim`
- The database volume (`servicehub-pgdata`) persists across restarts
- To reset the database: `docker compose down -v && docker compose up --build`
- Flyway runs automatically on API startup — no manual migration needed

### Local Development (Recommended for Backend)

Instead of rebuilding Docker on every change, run the API locally and only Dockerize dependencies:

```bash
# Start only Postgres + MailHog
docker compose up postgres mailhog -d

# Run API locally (from backend/ directory)
cd backend
SPRING_DATASOURCE_PASSWORD=changeme_use_strong_password \
JWT_SECRET=changeme_generate_a_256bit_secret_key_here_minimum_32_characters \
FILE_UPLOAD_DIR=/tmp/servicehub-uploads \
MAIL_HOST=localhost \
./mvnw spring-boot:run
```

See `backend/README.md` for the full backend developer guide.

## Roles & Permissions

| Role  | Capabilities                                                  |
|-------|---------------------------------------------------------------|
| USER  | Create requests, view own requests, upload attachments        |
| AGENT | All USER capabilities + update status, transfer requests      |
| ADMIN | Full access — manage departments, categories, users, SLA policies, view audit logs |

## Implementation Status

### Implemented

| Feature | Details |
|---------|---------|
| Core CRUD API | Service requests, departments, categories, locations, SLA policies, attachments, audit logs |
| JWT Authentication | Access tokens (1h) + refresh tokens (7d) with `POST /api/v1/auth/refresh` |
| User Management API | `GET /api/v1/users` (admin), `GET /api/v1/users/me` |
| Notification Preferences | `GET/PUT /api/v1/users/me/notification-preferences` |
| Email Notifications | Async event-driven emails via Spring Events, 9 event types, idempotent via `event_id` |
| Notification Logs API | `GET /api/v1/admin/notification-logs`, `POST .../retry` |
| Notification Retry Job | Scheduled (every 5 min), max 3 retries for failed emails |
| SLA Breach Detection | Scheduled job (every 5 min) detects breached SLA deadlines |
| SLA Warning Notifications | Alerts at 75% SLA consumption |
| Fine-grained RBAC | `@PreAuthorize` on admin endpoints, method-level security |
| Correlation ID | `X-Correlation-ID` header on all requests, included in error responses |
| Request Filtering | JPA Specifications: `?categoryId=&priority=&status=&locationId=&requesterId=&assignedAgentId=` |
| Auto-Routing | Intelligent agent assignment by department, location preference, and workload |
| Service Request Workflow | Full status machine: OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED |
| Dashboard Summary | `GET /api/v1/dashboard/summary` — counts and SLA compliance |

### Not Yet Implemented

| Feature | Owner | Details |
|---------|-------|---------|
| Dashboard: SLA Compliance | Backend C | `GET /api/v1/dashboard/sla-compliance` — compliance rates by category/priority/agent |
| Dashboard: Volume | Backend C | `GET /api/v1/dashboard/volume` — request volumes over time |
| Dashboard: Resolution Times | Backend C | `GET /api/v1/dashboard/resolution-times` — avg/median/P95 resolution times |
| Next.js Frontend | All Backend | Full frontend application (login, request forms, dashboards, admin pages, etc.) |
| Analytics Pipeline | Data Eng | Python ETL, sample data generator, trend analysis |
| Playwright/REST Assured Tests | QA | API integration tests and UI end-to-end tests |
