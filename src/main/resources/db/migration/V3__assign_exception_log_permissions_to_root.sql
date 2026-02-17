-- ============================================
-- Flyway Migration V3
-- Assign Exception Log Permissions to ROOT Role Only
-- ============================================
-- Author: Maximo Andriola
-- Date: 2026-02-17
-- Description: Assigns EXCEPTION_LOG permissions ONLY to ROOT role
--              These are developer-only permissions
-- ============================================

-- Step 1: Remove any existing EXCEPTION_LOG permissions from all roles (cleanup)
DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT id FROM permissions WHERE name IN ('EXCEPTION_LOG_READ', 'EXCEPTION_LOG_WRITE')
);

-- Step 2: Assign EXCEPTION_LOG_READ to ROOT role only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.active = true
  AND r.deleted = false
  AND UPPER(r.name) = 'ROOT'
  AND p.name = 'EXCEPTION_LOG_READ'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp2
    WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
  );

-- Step 3: Assign EXCEPTION_LOG_WRITE to ROOT role only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.active = true
  AND r.deleted = false
  AND UPPER(r.name) = 'ROOT'
  AND p.name = 'EXCEPTION_LOG_WRITE'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp2
    WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
  );

-- Step 4: Verify - Show which roles have EXCEPTION_LOG permissions (should only be ROOT)
SELECT
    r.name as role_name,
    p.name as permission_name,
    COUNT(ur.user_id) as user_count
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
LEFT JOIN user_roles ur ON r.id = ur.role_id
WHERE p.name IN ('EXCEPTION_LOG_READ', 'EXCEPTION_LOG_WRITE')
GROUP BY r.name, p.name
ORDER BY r.name, p.name;

