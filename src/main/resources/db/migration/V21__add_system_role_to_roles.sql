-- Add system_role flag to roles table.
-- System roles (OWNER, ADMIN, LECTOR) cannot be modified, renamed, or deleted.
ALTER TABLE roles ADD COLUMN system_role BOOLEAN NOT NULL DEFAULT false;

-- Mark existing system roles
UPDATE roles SET system_role = true
WHERE UPPER(name) IN ('OWNER', 'ADMIN', 'LECTOR');
