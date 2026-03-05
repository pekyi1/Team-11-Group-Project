-- =============================================================================
-- ServiceHub — V1: Core Schema
-- =============================================================================
-- Flyway migration: creates all tables, indexes, and constraints.
-- Tables: locations, departments, categories, users, service_requests,
--         attachments, status_history, sla_policies, audit_logs, notification_logs
--
-- Enum values stored as VARCHAR with CHECK constraints (JPA compatible).
-- =============================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- 1. locations
-- =============================================================================
CREATE TABLE locations (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    address         TEXT,
    city            VARCHAR(100),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_locations_name UNIQUE (name)
);

-- =============================================================================
-- 2. departments
-- =============================================================================
CREATE TABLE departments (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    description     TEXT,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_departments_name UNIQUE (name)
);

-- =============================================================================
-- 3. categories
-- =============================================================================
CREATE TABLE categories (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    key             VARCHAR(50)     NOT NULL,
    description     TEXT,
    department_id   BIGINT          NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT uq_categories_key  UNIQUE (key),
    CONSTRAINT fk_categories_department FOREIGN KEY (department_id) REFERENCES departments (id)
);

-- =============================================================================
-- 4. users
-- =============================================================================
CREATE TABLE users (
    id                          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    email                       VARCHAR(255)    NOT NULL,
    password_hash               VARCHAR(255)    NOT NULL,
    full_name                   VARCHAR(255)    NOT NULL,
    role                        VARCHAR(20)     NOT NULL DEFAULT 'USER',
    department_id               BIGINT,
    location_id                 BIGINT          NOT NULL,
    notification_preferences    JSONB           NOT NULL DEFAULT '{}',
    is_active                   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_users_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'AGENT', 'USER'))
);

-- =============================================================================
-- 5. service_requests
-- =============================================================================
CREATE TABLE service_requests (
    id                          BIGSERIAL       PRIMARY KEY,
    reference_number            VARCHAR(20)     NOT NULL,
    title                       VARCHAR(200)    NOT NULL,
    description                 TEXT            NOT NULL,
    category_id                 BIGINT          NOT NULL,
    priority                    VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    status                      VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    department_id               BIGINT,
    location_id                 BIGINT          NOT NULL,
    requester_id                UUID            NOT NULL,
    assigned_agent_id           UUID,
    response_sla_deadline       TIMESTAMPTZ,
    resolution_sla_deadline     TIMESTAMPTZ,
    response_sla_met            BOOLEAN,
    resolution_sla_met          BOOLEAN,
    responded_at                TIMESTAMPTZ,
    resolved_at                 TIMESTAMPTZ,
    closed_at                   TIMESTAMPTZ,
    is_deleted                  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_service_requests_ref UNIQUE (reference_number),
    CONSTRAINT fk_requests_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_requests_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_requests_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_requests_requester FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_requests_agent FOREIGN KEY (assigned_agent_id) REFERENCES users (id),
    CONSTRAINT chk_description_length CHECK (char_length(description) <= 5000),
    CONSTRAINT chk_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_status CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
);

-- =============================================================================
-- 6. attachments
-- =============================================================================
CREATE TABLE attachments (
    id                  BIGSERIAL       PRIMARY KEY,
    request_id          BIGINT          NOT NULL,
    file_name           VARCHAR(255)    NOT NULL,
    stored_file_name    VARCHAR(255)    NOT NULL,
    file_path           VARCHAR(500)    NOT NULL,
    content_type        VARCHAR(100)    NOT NULL,
    file_size_bytes     BIGINT          NOT NULL,
    checksum            VARCHAR(64)     NOT NULL,
    uploaded_by         UUID            NOT NULL,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_attachments_request FOREIGN KEY (request_id) REFERENCES service_requests (id),
    CONSTRAINT fk_attachments_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id),
    CONSTRAINT chk_file_size CHECK (file_size_bytes > 0 AND file_size_bytes <= 5242880)
);

-- =============================================================================
-- 7. status_history
-- =============================================================================
CREATE TABLE status_history (
    id              BIGSERIAL       PRIMARY KEY,
    request_id      BIGINT          NOT NULL,
    from_status     VARCHAR(20),
    to_status       VARCHAR(20)     NOT NULL,
    changed_by      UUID            NOT NULL,
    from_agent_id   UUID,
    to_agent_id     UUID,
    comment         TEXT,
    changed_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_status_history_request FOREIGN KEY (request_id) REFERENCES service_requests (id),
    CONSTRAINT fk_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id),
    CONSTRAINT fk_status_history_from_agent FOREIGN KEY (from_agent_id) REFERENCES users (id),
    CONSTRAINT fk_status_history_to_agent FOREIGN KEY (to_agent_id) REFERENCES users (id)
);

-- =============================================================================
-- 8. sla_policies
-- =============================================================================
CREATE TABLE sla_policies (
    id                          SERIAL          PRIMARY KEY,
    category_id                 BIGINT          NOT NULL,
    priority                    VARCHAR(20)     NOT NULL,
    response_time_minutes       INT             NOT NULL,
    resolution_time_minutes     INT             NOT NULL,
    is_active                   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sla_policies_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT uq_sla_category_priority UNIQUE (category_id, priority),
    CONSTRAINT chk_sla_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_response_time CHECK (response_time_minutes > 0),
    CONSTRAINT chk_resolution_time CHECK (resolution_time_minutes > 0),
    CONSTRAINT chk_resolution_gte_response CHECK (resolution_time_minutes >= response_time_minutes)
);

-- =============================================================================
-- 9. audit_logs (APPEND-ONLY)
-- =============================================================================
CREATE TABLE audit_logs (
    id              BIGSERIAL       PRIMARY KEY,
    action          VARCHAR(50)     NOT NULL,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       VARCHAR(255)    NOT NULL,
    actor_id        UUID,
    actor_role      VARCHAR(20),
    actor_location  VARCHAR(100),
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    correlation_id  VARCHAR(100),
    description     TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);

-- =============================================================================
-- 10. notification_logs
-- =============================================================================
CREATE TABLE notification_logs (
    id                      BIGSERIAL       PRIMARY KEY,
    event_type              VARCHAR(50)     NOT NULL,
    event_id                VARCHAR(255)    NOT NULL,
    recipient_id            UUID,
    recipient_email         VARCHAR(255)    NOT NULL,
    subject                 VARCHAR(500)    NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    retry_count             INT             NOT NULL DEFAULT 0,
    last_error              TEXT,
    related_entity_type     VARCHAR(50),
    related_entity_id       VARCHAR(255),
    sent_at                 TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_event_id UNIQUE (event_id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY'))
);

-- =============================================================================
-- INDEXES
-- =============================================================================
CREATE INDEX idx_categories_department_id ON categories (department_id);

CREATE INDEX idx_users_department_role ON users (department_id, role);
CREATE INDEX idx_users_department_location_role ON users (department_id, location_id, role);
CREATE INDEX idx_users_location ON users (location_id);

CREATE INDEX idx_requests_category ON service_requests (category_id);
CREATE INDEX idx_requests_department_status ON service_requests (department_id, status);
CREATE INDEX idx_requests_department_location_status ON service_requests (department_id, location_id, status);
CREATE INDEX idx_requests_location ON service_requests (location_id);
CREATE INDEX idx_requests_agent_status ON service_requests (assigned_agent_id, status);
CREATE INDEX idx_requests_requester ON service_requests (requester_id);
CREATE INDEX idx_requests_created_at ON service_requests (created_at);
CREATE INDEX idx_requests_sla_compliance ON service_requests (response_sla_met, resolution_sla_met);

CREATE INDEX idx_attachments_request ON attachments (request_id);
CREATE INDEX idx_status_history_request ON status_history (request_id, changed_at);
CREATE INDEX idx_sla_category_priority ON sla_policies (category_id, priority);

CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_created_at ON audit_logs (created_at);

CREATE INDEX idx_notification_event_id ON notification_logs (event_id);
CREATE INDEX idx_notification_status_retry ON notification_logs (status, retry_count);
CREATE INDEX idx_notification_recipient ON notification_logs (recipient_id, event_type);
CREATE INDEX idx_notification_related_entity ON notification_logs (related_entity_type, related_entity_id);
CREATE INDEX idx_notification_created_at ON notification_logs (created_at);

-- =============================================================================
-- SECURITY: Revoke UPDATE/DELETE on audit_logs (append-only enforcement)
-- =============================================================================
REVOKE UPDATE, DELETE ON audit_logs FROM PUBLIC;
