-- V23: Reclassify USER_MANAGEMENT and ROLE_MANAGEMENT permissions
-- Move them out of the "System" module so they appear in the role form
-- Internal system permissions (AUDIT, EXCEPTION_LOG, etc.) stay in "System" and remain hidden

UPDATE permissions
SET module              = 'Users',
    spanish_translation = 'Usuarios - Gestión',
    spanish_description = 'Permite gestionar usuarios del sistema'
WHERE name = 'USER_MANAGEMENT';

UPDATE permissions
SET module              = 'Roles',
    spanish_translation = 'Roles - Gestión',
    spanish_description = 'Permite gestionar roles del sistema'
WHERE name = 'ROLE_MANAGEMENT';
