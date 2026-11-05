-- Add hierarchical position to roles.
-- Position 1 = highest authority (OWNER). Higher numbers = lower authority.
-- Uniqueness per tenant is enforced in application logic (RoleService) to avoid
-- transient constraint violations during bulk reorder operations.
ALTER TABLE roles ADD COLUMN position INTEGER NOT NULL DEFAULT 0;

-- Initialise positions for existing roles within each tenant.
-- System roles get fixed positions: OWNER=1, ADMIN=2, LECTOR=3.
-- Custom roles are numbered sequentially starting from 4.
DO $$
DECLARE
    t_id BIGINT;
    r RECORD;
    pos INTEGER;
BEGIN
    FOR t_id IN SELECT DISTINCT tenant_id FROM roles LOOP
        -- System roles first, in fixed order
        UPDATE roles SET position = 1
        WHERE tenant_id = t_id AND UPPER(name) = 'OWNER' AND deleted = false;

        UPDATE roles SET position = 2
        WHERE tenant_id = t_id AND UPPER(name) = 'ADMIN' AND deleted = false;

        UPDATE roles SET position = 3
        WHERE tenant_id = t_id AND UPPER(name) = 'LECTOR' AND deleted = false;

        -- Custom roles after system roles
        pos := 4;
        FOR r IN
            SELECT id FROM roles
            WHERE tenant_id = t_id
              AND deleted = false
              AND UPPER(name) NOT IN ('OWNER', 'ADMIN', 'LECTOR')
            ORDER BY id
        LOOP
            UPDATE roles SET position = pos WHERE id = r.id;
            pos := pos + 1;
        END LOOP;

        -- Deleted roles get position 0 (irrelevant)
        UPDATE roles SET position = 0
        WHERE tenant_id = t_id AND deleted = true;
    END LOOP;
END $$;
