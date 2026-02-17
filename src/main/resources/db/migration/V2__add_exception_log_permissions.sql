-- ============================================
-- Flyway Migration V2
-- Add Exception Log Permissions
-- ============================================
-- Author: Maximo Andriola
-- Date: 2026-02-17
-- Description: Creates EXCEPTION_LOG_READ and EXCEPTION_LOG_WRITE permissions
-- Note: Uses only basic columns for compatibility with existing schema
-- ============================================

-- Insert EXCEPTION_LOG_READ permission if not exists
INSERT INTO permissions (
    name,
    description,
    module,
    spanish_translation
)
SELECT
    'EXCEPTION_LOG_READ',
    'Allows viewing exception logs for system monitoring and debugging.',
    'System',
    'Sistema'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE name = 'EXCEPTION_LOG_READ'
);

-- Insert EXCEPTION_LOG_WRITE permission if not exists
INSERT INTO permissions (
    name,
    description,
    module,
    spanish_translation
)
SELECT
    'EXCEPTION_LOG_WRITE',
    'Allows managing exception logs (cleanup, deletion).',
    'System',
    'Sistema'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE name = 'EXCEPTION_LOG_WRITE'
);

-- Verify permissions were created
SELECT
    id,
    name,
    module,
    spanish_translation
FROM permissions
WHERE name IN ('EXCEPTION_LOG_READ', 'EXCEPTION_LOG_WRITE')
ORDER BY name;


