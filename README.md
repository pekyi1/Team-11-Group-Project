# ServiceHub - Internal Service Request System

An internal service request management platform with intelligent routing, SLA tracking, and workflow automation.

## Architecture
- Spring Boot 3.2 + PostgreSQL + Thymeleaf
- JWT authentication, Role-based access control
- Port 8080 (backend), Port 5432 (postgres)

## Domain Split
### Dev A: Request Management (RequestService, RequestController)
### Dev B: Workflow/SLA (WorkflowService, SlaService, AssignmentService)
### Dev C: Auth/Dashboard (AuthService, DashboardService)

## Quick Start: docker-compose up --build

Default Users: emp@servicehub.com, agent@servicehub.com, mgr@servicehub.com (password123)
Swagger: http://localhost:8080/swagger-ui.html
