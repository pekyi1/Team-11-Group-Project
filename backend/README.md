# ServiceHub Backend — Developer Guide

Spring Boot 3.2 REST API with Java 17. This guide covers local development setup, project structure, and common tasks.

## Prerequisites

- **Java 17** (OpenJDK or Eclipse Temurin)
- **Docker & Docker Compose** (for PostgreSQL and MailHog)
- Maven wrapper is included (`./mvnw`) — no separate Maven install needed

## Local Development Setup

The recommended workflow is to run the API locally while using Docker only for dependencies (database + mail server). This avoids rebuilding the Docker image on every code change.

### 1. Start dependencies

From the **project root** (`4-ServiceHub/`):

```bash
docker compose up postgres mailhog -d
```

This starts:
- **PostgreSQL** on `localhost:5432`
- **MailHog** SMTP on `localhost:1025`, Web UI on `http://localhost:8025`

### 2. Run the API

From the `backend/` directory:

```bash
SPRING_DATASOURCE_PASSWORD=changeme_use_strong_password \
JWT_SECRET=changeme_generate_a_256bit_secret_key_here_minimum_32_characters \
FILE_UPLOAD_DIR=/tmp/servicehub-uploads \
MAIL_HOST=localhost \
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. Flyway migrations run automatically on startup.

### 3. Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Login as admin (use a file to avoid shell escaping issues with '!')
echo '{"email":"admin@servicehub.local","password":"Admin@Sh2026!"}' > /tmp/login.json
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' -d @/tmp/login.json | python3 -m json.tool
```

### Environment Variables

All config is externalized. Defaults work for local dev with the Docker Compose setup.

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_PASSWORD` | *(none)* | Database password (required) |
| `JWT_SECRET` | *(none)* | JWT signing key, min 32 chars (required) |
| `MAIL_HOST` | `localhost` | SMTP host (`localhost` for local dev, `mailhog` in Docker) |
| `FILE_UPLOAD_DIR` | `/data/uploads` | File storage path (use `/tmp/servicehub-uploads` locally) |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile |

See the root `.env.example` for the full list.

## Project Structure

```
src/main/java/com/servicehub/
├── ServiceHubApplication.java       # Entry point (@EnableAsync, @EnableScheduling)
├── config/
│   ├── SecurityConfig.java          # Spring Security + @EnableMethodSecurity
│   ├── JwtService.java              # JWT generation/validation (access + refresh tokens)
│   ├── JwtAuthFilter.java           # JWT authentication filter
│   ├── CorsConfig.java              # CORS configuration
│   └── CorrelationIdFilter.java     # X-Correlation-ID header propagation
├── controller/                      # REST controllers (11 controllers)
│   ├── AuthController.java          # /auth/login, /auth/register, /auth/refresh
│   ├── UserController.java          # /users, /users/me, /users/me/notification-preferences
│   ├── ServiceRequestController.java # /requests (CRUD, status, transfer, assign, history)
│   ├── AttachmentController.java    # /requests/{id}/attachments
│   ├── DepartmentController.java    # /departments (+ agent management)
│   ├── CategoryController.java      # /categories
│   ├── LocationController.java      # /locations
│   ├── SlaPolicyController.java     # /admin/sla-policies
│   ├── DashboardController.java     # /dashboard/summary
│   ├── AuditLogController.java      # /audit-logs
│   └── NotificationLogController.java # /admin/notification-logs
├── dto/
│   ├── request/                     # 15 request DTOs (validated with Jakarta Bean Validation)
│   └── response/                    # 14 response DTOs
├── event/
│   └── ServiceRequestEvent.java     # Domain event for async notifications
├── exception/
│   ├── GlobalExceptionHandler.java  # Centralized error handling (includes correlationId)
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   ├── DuplicateResourceException.java
│   └── InvalidStatusTransitionException.java
├── model/                           # 10 JPA entities
│   ├── enums/                       # Role, RequestStatus, Priority
│   └── *.java
├── repository/                      # 11 Spring Data JPA repositories
│   ├── specification/
│   │   └── ServiceRequestSpecification.java  # Dynamic query filters
│   └── *.java
└── service/                         # 12 service classes
    ├── AuthService.java             # Auth, user management, notification prefs
    ├── ServiceRequestService.java   # Core ticket logic, auto-routing, status transitions
    ├── NotificationService.java     # Async email notifications (event-driven)
    ├── NotificationLogService.java  # Admin notification log queries + retry
    ├── NotificationRetryService.java # Scheduled retry of failed emails
    ├── SlaMonitoringService.java    # Scheduled SLA breach/warning detection
    ├── DashboardService.java        # Dashboard aggregations
    ├── AttachmentService.java       # File upload/download
    └── *.java                       # CRUD services for departments, categories, etc.
```

## Key Patterns

### Authentication & Authorization

- **JWT access tokens** (1 hour expiry) + **refresh tokens** (7 day expiry)
- Refresh tokens carry a `"type": "refresh"` claim to distinguish them from access tokens
- `@PreAuthorize("hasRole('ADMIN')")` on admin-only endpoints (method-level RBAC)
- Security filter chain in `SecurityConfig.java` — public: `/auth/**`, `/actuator/health`; admin: `/admin/**`, `/audit-logs/**`

### Event-Driven Email Notifications

Emails are sent asynchronously using Spring Application Events:

1. Service methods publish `ServiceRequestEvent` via `ApplicationEventPublisher`
2. `NotificationService` listens with `@Async @TransactionalEventListener`
3. Events are processed after the transaction commits, in a separate thread
4. Each notification is logged to `notification_logs` with idempotency via `event_id`
5. Failed notifications are retried by `NotificationRetryService` (every 5 min, max 3 retries)

Event types: `REQUEST_CREATED`, `REQUEST_ASSIGNED`, `STATUS_IN_PROGRESS`, `STATUS_RESOLVED`, `STATUS_CLOSED`, `TICKET_TRANSFERRED`, `REQUEST_REOPENED`, `SLA_WARNING`, `SLA_BREACHED`

### SLA Monitoring

`SlaMonitoringService` runs every 5 minutes (`@Scheduled(fixedRate = 300000)`):
- Detects **SLA breaches** (deadline passed without response/resolution)
- Detects **75% SLA warnings** (75% of allotted time consumed)
- Publishes events that trigger email alerts to assigned agents
- Idempotent — won't send duplicate warnings via `event_id` check

### Request Filtering (JPA Specifications)

`GET /api/v1/requests` supports query parameters that are combined dynamically:

```
?categoryId=1&priority=HIGH&status=ASSIGNED&locationId=1&requesterId=<uuid>&assignedAgentId=<uuid>
```

Implemented via `ServiceRequestSpecification` with composable `Specification<ServiceRequest>` predicates.

### Correlation ID

Every request gets a `X-Correlation-ID` header (generated or propagated from client). It's included in:
- All error responses (`correlationId` field)
- MDC for log correlation

## Database

### Migrations

Flyway migrations live in `src/main/resources/db/migration/`:

| File | Description |
|------|-------------|
| `V1__create_schema.sql` | All tables, indexes, constraints |
| `V2__seed_data.sql` | Seed data (locations, departments, categories, SLA policies, users) |
| `V3__fix_seed_passwords.sql` | Corrected BCrypt hashes for seed users |

Flyway runs automatically on startup. To add a new migration, create `V4__description.sql` in the migration directory.

### Reset Database

```bash
docker compose down -v    # removes the volume
docker compose up postgres mailhog -d
# Restart the API — Flyway will recreate everything
```

### Connect Directly

```bash
docker exec -it servicehub-db psql -U servicehub_app -d servicehub
```

## Common Tasks

### Compile Without Running

```bash
./mvnw compile
```

### Run Tests

```bash
./mvnw test
```

### Build the Docker Image

From the `backend/` directory:

```bash
docker build -t servicehub-api .
```

### Add a New Entity

1. Create the entity in `model/`
2. Create a repository in `repository/`
3. Create request/response DTOs in `dto/`
4. Create a service in `service/`
5. Create a controller in `controller/`
6. Add a Flyway migration if the table doesn't exist yet (`VN__description.sql`)

### Add a New Endpoint

1. Add the DTO(s) in `dto/request/` and `dto/response/`
2. Add the service method
3. Add the controller method with appropriate `@PreAuthorize` if admin-only
4. If it's a new admin path under `/api/v1/admin/**`, it's automatically restricted by the security filter chain

### Trigger Email Notifications

Emails are captured by MailHog in dev. View them at `http://localhost:8025`.

To test manually: create a service request (triggers `REQUEST_CREATED` + `REQUEST_ASSIGNED` emails), then update its status.

## Seed Users

| Email | Password | Role |
|-------|----------|------|
| `admin@servicehub.local` | `Admin@Sh2026!` | ADMIN |
| `it.agent.accra@servicehub.local` | `Agent@Sh2026!` | AGENT |
| `it.agent.takoradi@servicehub.local` | `Agent@Sh2026!` | AGENT |
| `facilities.agent@servicehub.local` | `Agent@Sh2026!` | AGENT |
| `hr.agent@servicehub.local` | `Agent@Sh2026!` | AGENT |
| `hr.agent.kumasi@servicehub.local` | `Agent@Sh2026!` | AGENT |
| `user.accra@servicehub.local` | `User@Sh2026!` | USER |
| `user.takoradi@servicehub.local` | `User@Sh2026!` | USER |

## Troubleshooting

### `mvn: command not found`

Use the Maven wrapper included in this directory: `./mvnw` instead of `mvn`.

### Port 8080 already in use

```bash
lsof -i :8080    # find the process
kill <PID>        # stop it
```

### LazyInitializationException

All service methods that read data must be annotated with `@Transactional(readOnly = true)`. If you access a lazy-loaded relationship (e.g., `request.getCategory().getName()`) outside a transaction, you'll get this error.

### Shell escaping issues with passwords

The seed passwords contain `!` which bash interprets as history expansion. Use a file:

```bash
echo '{"email":"admin@servicehub.local","password":"Admin@Sh2026!"}' > /tmp/login.json
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' -d @/tmp/login.json
```

### Flyway migration conflicts

If two developers create migrations with the same version number (e.g., both create `V4__...`), one must be renumbered. Check `flyway_schema_history` table if migrations get stuck.

### Emails not appearing in MailHog

- Verify MailHog is running: `docker ps | grep mailhog`
- Verify `MAIL_HOST=localhost` when running locally (not `mailhog`)
- Check notification logs: `GET /api/v1/admin/notification-logs` (as admin)
- Failed notifications are retried automatically every 5 minutes (up to 3 times)
