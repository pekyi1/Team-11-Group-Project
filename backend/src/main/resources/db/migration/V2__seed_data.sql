-- =============================================================================
-- ServiceHub — V2: Seed Data (Development & Testing)
-- =============================================================================
-- Bootstraps the system with locations, departments, categories, SLA policies,
-- and test users so the team can begin development immediately.
--
-- IMPORTANT: These are dev/test seed values only.
-- In production, ADMIN creates all entities via the admin UI.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Seed Locations
-- ---------------------------------------------------------------------------
INSERT INTO locations (name, address, city) VALUES
    ('Accra HQ',         'Independence Ave, Accra',    'Accra'),
    ('Takoradi Branch',  'Market Circle, Takoradi',    'Takoradi'),
    ('Kumasi Branch',    'Adum Road, Kumasi',          'Kumasi');

-- ---------------------------------------------------------------------------
-- Seed Departments
-- ---------------------------------------------------------------------------
INSERT INTO departments (name, description) VALUES
    ('IT Department',           'Handles all IT infrastructure, software, and hardware requests'),
    ('Facilities Management',   'Manages building, office space, and physical infrastructure requests'),
    ('Human Resources',         'Processes HR-related requests including policies, benefits, and onboarding');

-- ---------------------------------------------------------------------------
-- Seed Categories (many-to-one with departments)
-- ---------------------------------------------------------------------------
INSERT INTO categories (name, key, description, department_id) VALUES
    -- IT Department
    ('Network Issues',          'NETWORK_ISSUES',       'Wi-Fi, LAN, VPN, and connectivity problems',
        (SELECT id FROM departments WHERE name = 'IT Department')),
    ('Software Installation',   'SOFTWARE_INSTALLATION','Request new software or updates',
        (SELECT id FROM departments WHERE name = 'IT Department')),
    ('Hardware Repair',         'HARDWARE_REPAIR',      'Laptops, monitors, printers, and peripherals',
        (SELECT id FROM departments WHERE name = 'IT Department')),
    -- Facilities Management
    ('Office Maintenance',      'OFFICE_MAINTENANCE',   'Furniture, lighting, plumbing, HVAC issues',
        (SELECT id FROM departments WHERE name = 'Facilities Management')),
    ('Space Booking',           'SPACE_BOOKING',        'Meeting rooms and shared space reservations',
        (SELECT id FROM departments WHERE name = 'Facilities Management')),
    ('Access Cards',            'ACCESS_CARDS',         'New, replacement, or deactivation of access cards',
        (SELECT id FROM departments WHERE name = 'Facilities Management')),
    -- Human Resources
    ('Leave Request',           'LEAVE_REQUEST',        'Annual, sick, and special leave applications',
        (SELECT id FROM departments WHERE name = 'Human Resources')),
    ('Onboarding',              'ONBOARDING',           'New employee setup and orientation',
        (SELECT id FROM departments WHERE name = 'Human Resources')),
    ('Benefits Query',          'BENEFITS_QUERY',       'Health insurance, pension, and allowance queries',
        (SELECT id FROM departments WHERE name = 'Human Resources'));

-- ---------------------------------------------------------------------------
-- Seed SLA Policies (9 categories x 4 priorities = 36 policies)
-- ---------------------------------------------------------------------------
-- Response = time until first agent response; Resolution = time to close.
-- Values in minutes. Tuned per category — Admin can adjust at any time.

-- Helper: insert SLA policies for a category across all priorities
-- IT Department categories (technical — tighter SLAs)
INSERT INTO sla_policies (category_id, priority, response_time_minutes, resolution_time_minutes) VALUES
    -- Network Issues
    ((SELECT id FROM categories WHERE key = 'NETWORK_ISSUES'), 'CRITICAL',  15,    120),
    ((SELECT id FROM categories WHERE key = 'NETWORK_ISSUES'), 'HIGH',      30,    240),
    ((SELECT id FROM categories WHERE key = 'NETWORK_ISSUES'), 'MEDIUM',    60,    480),
    ((SELECT id FROM categories WHERE key = 'NETWORK_ISSUES'), 'LOW',       120,   1440),
    -- Software Installation
    ((SELECT id FROM categories WHERE key = 'SOFTWARE_INSTALLATION'), 'CRITICAL',  30,    240),
    ((SELECT id FROM categories WHERE key = 'SOFTWARE_INSTALLATION'), 'HIGH',      60,    480),
    ((SELECT id FROM categories WHERE key = 'SOFTWARE_INSTALLATION'), 'MEDIUM',    120,   1440),
    ((SELECT id FROM categories WHERE key = 'SOFTWARE_INSTALLATION'), 'LOW',       240,   2880),
    -- Hardware Repair
    ((SELECT id FROM categories WHERE key = 'HARDWARE_REPAIR'), 'CRITICAL',  30,    480),
    ((SELECT id FROM categories WHERE key = 'HARDWARE_REPAIR'), 'HIGH',      60,    960),
    ((SELECT id FROM categories WHERE key = 'HARDWARE_REPAIR'), 'MEDIUM',    120,   1440),
    ((SELECT id FROM categories WHERE key = 'HARDWARE_REPAIR'), 'LOW',       240,   2880),
    -- Office Maintenance
    ((SELECT id FROM categories WHERE key = 'OFFICE_MAINTENANCE'), 'CRITICAL',  30,    240),
    ((SELECT id FROM categories WHERE key = 'OFFICE_MAINTENANCE'), 'HIGH',      60,    480),
    ((SELECT id FROM categories WHERE key = 'OFFICE_MAINTENANCE'), 'MEDIUM',    120,   1440),
    ((SELECT id FROM categories WHERE key = 'OFFICE_MAINTENANCE'), 'LOW',       240,   2880),
    -- Space Booking
    ((SELECT id FROM categories WHERE key = 'SPACE_BOOKING'), 'CRITICAL',  15,    60),
    ((SELECT id FROM categories WHERE key = 'SPACE_BOOKING'), 'HIGH',      30,    120),
    ((SELECT id FROM categories WHERE key = 'SPACE_BOOKING'), 'MEDIUM',    60,    240),
    ((SELECT id FROM categories WHERE key = 'SPACE_BOOKING'), 'LOW',       120,   480),
    -- Access Cards
    ((SELECT id FROM categories WHERE key = 'ACCESS_CARDS'), 'CRITICAL',  15,    120),
    ((SELECT id FROM categories WHERE key = 'ACCESS_CARDS'), 'HIGH',      30,    240),
    ((SELECT id FROM categories WHERE key = 'ACCESS_CARDS'), 'MEDIUM',    60,    480),
    ((SELECT id FROM categories WHERE key = 'ACCESS_CARDS'), 'LOW',       120,   1440),
    -- Leave Request
    ((SELECT id FROM categories WHERE key = 'LEAVE_REQUEST'), 'CRITICAL',  30,    240),
    ((SELECT id FROM categories WHERE key = 'LEAVE_REQUEST'), 'HIGH',      60,    480),
    ((SELECT id FROM categories WHERE key = 'LEAVE_REQUEST'), 'MEDIUM',    120,   1440),
    ((SELECT id FROM categories WHERE key = 'LEAVE_REQUEST'), 'LOW',       240,   2880),
    -- Onboarding
    ((SELECT id FROM categories WHERE key = 'ONBOARDING'), 'CRITICAL',  30,    480),
    ((SELECT id FROM categories WHERE key = 'ONBOARDING'), 'HIGH',      60,    960),
    ((SELECT id FROM categories WHERE key = 'ONBOARDING'), 'MEDIUM',    120,   1440),
    ((SELECT id FROM categories WHERE key = 'ONBOARDING'), 'LOW',       240,   2880),
    -- Benefits Query
    ((SELECT id FROM categories WHERE key = 'BENEFITS_QUERY'), 'CRITICAL',  30,    240),
    ((SELECT id FROM categories WHERE key = 'BENEFITS_QUERY'), 'HIGH',      60,    480),
    ((SELECT id FROM categories WHERE key = 'BENEFITS_QUERY'), 'MEDIUM',    120,   1440),
    ((SELECT id FROM categories WHERE key = 'BENEFITS_QUERY'), 'LOW',       240,   2880);

-- ---------------------------------------------------------------------------
-- Seed Users
-- ---------------------------------------------------------------------------
-- Passwords are BCrypt-hashed. The team should use these for development only.
--
-- Plaintext passwords:
--   ADMIN:  Admin@Sh2026!
--   AGENTS: Agent@Sh2026!
--   USERS:  User@Sh2026!
--
-- BCrypt hash (cost 12) — generate with: htpasswd -nbBC 12 "" "password" | cut -d: -f2
-- These are pre-computed hashes for the seed passwords above.
-- ---------------------------------------------------------------------------

-- ADMIN
INSERT INTO users (email, password_hash, full_name, role, location_id) VALUES
    ('admin@servicehub.local',
     '$2a$12$LJ3m4yst.SJmBguSmFEXxO5dSJxRnGMRFf3rXwYIfkhJOz6AVpzCq',
     'System Admin', 'ADMIN',
     (SELECT id FROM locations WHERE name = 'Accra HQ'));

-- AGENTS
INSERT INTO users (email, password_hash, full_name, role, department_id, location_id) VALUES
    ('it.agent.accra@servicehub.local',
     '$2a$12$LJ3m4yst.SJmBguSmFEXxO5dSJxRnGMRFf3rXwYIfkhJOz6AVpzCq',
     'IT Agent (Accra)', 'AGENT',
     (SELECT id FROM departments WHERE name = 'IT Department'),
     (SELECT id FROM locations WHERE name = 'Accra HQ')),

    ('it.agent.takoradi@servicehub.local',
     '$2a$12$LJ3m4yst.SJmBguSmFEXxO5dSJxRnGMRFf3rXwYIfkhJOz6AVpzCq',
     'IT Agent (Takoradi)', 'AGENT',
     (SELECT id FROM departments WHERE name = 'IT Department'),
     (SELECT id FROM locations WHERE name = 'Takoradi Branch')),

    ('facilities.agent@servicehub.local',
     '$2a$12$LJ3m4yst.SJmBguSmFEXxO5dSJxRnGMRFf3rXwYIfkhJOz6AVpzCq',
     'Facilities Agent (Accra)', 'AGENT',
     (SELECT id FROM departments WHERE name = 'Facilities Management'),
     (SELECT id FROM locations WHERE name = 'Accra HQ')),

    ('hr.agent@servicehub.local',
     '$2a$12$LJ3m4yst.SJmBguSmFEXxO5dSJxRnGMRFf3rXwYIfkhJOz6AVpzCq',
     'HR Agent (Accra)', 'AGENT',
     (SELECT id FROM departments WHERE name = 'Human Resources'),
     (SELECT id FROM locations WHERE name = 'Accra HQ')),

    ('hr.agent.kumasi@servicehub.local',
     '$2a$12$LJ3m4yst.SJmBguSmFEXxO5dSJxRnGMRFf3rXwYIfkhJOz6AVpzCq',
     'HR Agent (Kumasi)', 'AGENT',
     (SELECT id FROM departments WHERE name = 'Human Resources'),
     (SELECT id FROM locations WHERE name = 'Kumasi Branch'));

-- REGULAR USERS
INSERT INTO users (email, password_hash, full_name, role, location_id) VALUES
    ('user.accra@servicehub.local',
     '$2a$12$LJ3m4yst.SJmBguSmFEXxO5dSJxRnGMRFf3rXwYIfkhJOz6AVpzCq',
     'Employee (Accra)', 'USER',
     (SELECT id FROM locations WHERE name = 'Accra HQ')),

    ('user.takoradi@servicehub.local',
     '$2a$12$LJ3m4yst.SJmBguSmFEXxO5dSJxRnGMRFf3rXwYIfkhJOz6AVpzCq',
     'Employee (Takoradi)', 'USER',
     (SELECT id FROM locations WHERE name = 'Takoradi Branch'));
