-- Soft-delete legacy "Root" role (artifact from initial seeder before OWNER/ADMIN/LECTOR refactoring).
-- Reassign any users who still have the Root role to the OWNER role within their tenant.

DO $$
DECLARE
    root_role RECORD;
    owner_role_id BIGINT;
BEGIN
    FOR root_role IN
        SELECT id, tenant_id FROM roles
        WHERE UPPER(name) = 'ROOT' AND deleted = false
    LOOP
        -- Find the OWNER role for the same tenant
        SELECT id INTO owner_role_id
        FROM roles
        WHERE UPPER(name) = 'OWNER' AND tenant_id = root_role.tenant_id AND deleted = false
        LIMIT 1;

        -- Reassign users from Root to OWNER (if OWNER exists)
        IF owner_role_id IS NOT NULL THEN
            INSERT INTO user_roles (user_id, role_id)
            SELECT ur.user_id, owner_role_id
            FROM user_roles ur
            WHERE ur.role_id = root_role.id
              AND NOT EXISTS (
                  SELECT 1 FROM user_roles ur2
                  WHERE ur2.user_id = ur.user_id AND ur2.role_id = owner_role_id
              );

            -- Remove Root role assignments
            DELETE FROM user_roles WHERE role_id = root_role.id;
        END IF;

        -- Soft-delete the Root role
        UPDATE roles SET deleted = true, active = false, position = 0
        WHERE id = root_role.id;
    END LOOP;
END $$;
