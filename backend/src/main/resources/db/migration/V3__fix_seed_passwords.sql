-- ---------------------------------------------------------------------------
-- V3: Fix seed user password hashes to match documented plaintext passwords
--
-- Plaintext passwords (unchanged):
--   ADMIN:  Admin@Sh2026!
--   AGENTS: Agent@Sh2026!
--   USERS:  User@Sh2026!
-- ---------------------------------------------------------------------------

-- Admin password hash for: Admin@Sh2026!
UPDATE users SET password_hash = '$2b$12$wi1081ayrRoLhyLwaF4y9OFhqmjWf0Jl67iMdKIlnocBvzXAjrrUS'
WHERE email = 'admin@servicehub.local';

-- Agent password hash for: Agent@Sh2026!
UPDATE users SET password_hash = '$2b$12$LBDo73ChJu7gKcbNIGXPv.EsTyvxFUJ1W58KqOCftG7RbJHe4G/zm'
WHERE role = 'AGENT';

-- User password hash for: User@Sh2026!
UPDATE users SET password_hash = '$2b$12$J..bb0722nX2lklC4IGZPup3Aqg4YixinT0G7XW/9V8BKEQ/kOg5m'
WHERE role = 'USER';
