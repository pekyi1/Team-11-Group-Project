-- =============================================================================
-- ServiceHub — Database Initialization Script
-- =============================================================================
-- Runs automatically on first PostgreSQL container startup via
-- docker-entrypoint-initdb.d. Idempotent — safe to re-run.
-- =============================================================================

-- Create analytics read-only user (for data engineering pipeline)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'analytics_readonly') THEN
        CREATE ROLE analytics_readonly WITH LOGIN PASSWORD 'changeme_analytics_readonly';
    END IF;
END
$$;

-- Grant read-only access on all current and future tables
GRANT CONNECT ON DATABASE servicehub TO analytics_readonly;
GRANT USAGE ON SCHEMA public TO analytics_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO analytics_readonly;

-- Ensure UUID extension is available (used for correlation IDs, etc.)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
