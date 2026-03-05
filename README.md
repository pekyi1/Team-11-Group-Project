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

| Method | Endpoint               | Auth     | Description              |
|--------|------------------------|----------|--------------------------|
| POST   | /api/v1/auth/register  | Public   | Register a new user      |
| POST   | /api/v1/auth/login     | Public   | Login, returns JWT token |
| GET    | /api/v1/auth/me        | Required | Get current user profile |

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

## Project Structure

```
4-ServiceHub/
├── backend/
│   ├── src/main/java/com/servicehub/
│   │   ├── config/          # Security, JWT, CORS configuration
│   │   ├── controller/      # REST controllers (9 controllers)
│   │   ├── dto/
│   │   │   ├── request/     # Request DTOs (14 files)
│   │   │   └── response/    # Response DTOs (13 files)
│   │   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   │   ├── model/           # JPA entities (10 entities)
│   │   │   └── enums/       # Role, RequestStatus, Priority
│   │   ├── repository/      # Spring Data JPA repositories (10 repos)
│   │   └── service/         # Business logic (9 services)
│   ├── src/main/resources/
│   │   ├── db/migration/    # Flyway SQL migrations (V1, V2, V3)
│   │   ├── application.yml  # Main config (externalized secrets)
│   │   └── application-{dev,test,prod}.yml
│   ├── Dockerfile
│   └── pom.xml
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
| JWT_EXPIRATION_MS       | 3600000 (1 hour)                 | Token expiry               |
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

## Roles & Permissions

| Role  | Capabilities                                                  |
|-------|---------------------------------------------------------------|
| USER  | Create requests, view own requests, upload attachments        |
| AGENT | All USER capabilities + update status, transfer requests      |
| ADMIN | Full access — manage departments, categories, users, SLA policies, view audit logs |

## Implementation Status

The backend CRUD layer and core workflow are implemented. The following items from the project instructions are **not yet implemented** and are expected to be built by the respective team members:

### Not Yet Implemented

| Feature | Owner | Details |
|---------|-------|---------|
| JWT Refresh Token | Backend C | `POST /api/v1/auth/refresh` — refresh token rotation; currently only access tokens are issued |
| User Management API | Backend C | `GET /api/v1/users` (list users, ADMIN only); `GET /api/v1/users/me` (currently at `/api/v1/auth/me`) |
| Notification Preferences API | Backend C | `GET/PUT /api/v1/users/me/notification-preferences` — manage per-user email opt-out settings |
| Notification Logs API | Backend C | `GET /api/v1/admin/notification-logs`, `POST .../retry` — admin email delivery monitoring |
| Email Notification Service | Backend B | Async event-driven emails (Spring Events + `@Async @EventListener`); HTML templates; retry with exponential backoff; idempotent via `event_id` |
| SLA Breach Detection Job | Backend B | Scheduled job (every 5 min) to detect breached SLA deadlines and trigger alerts |
| SLA Warning Notifications | Backend B | Alert at 75% SLA consumption |
| Dashboard: SLA Compliance | Backend C | `GET /api/v1/dashboard/sla-compliance` — compliance rates by category/priority/agent |
| Dashboard: Volume | Backend C | `GET /api/v1/dashboard/volume` — request volumes over time |
| Dashboard: Resolution Times | Backend C | `GET /api/v1/dashboard/resolution-times` — avg/median/P95 resolution times |
| Fine-grained RBAC | Backend C | `@PreAuthorize` annotations — users see own requests only, agents see department only, etc. |
| Correlation ID | Backend C | Add `correlationId` to all error responses and log entries for request tracing |
| Request Filtering | Backend A | JPA Specifications for filtering requests by categoryId, priority, status, locationId, requesterId, assignedAgentId |
| Next.js Frontend | All Backend | Full frontend application (login, request forms, dashboards, admin pages, etc.) |
| Analytics Pipeline | Data Eng | Python ETL, sample data generator, trend analysis |
| Playwright/REST Assured Tests | QA | API integration tests and UI end-to-end tests |
