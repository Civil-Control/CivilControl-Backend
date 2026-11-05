-- V26: Assign REPAIR_ORDER_* permissions to system roles (OWNER, ADMIN, LECTOR)
-- Without this, even after V25 creates the permissions, no one can see the
-- "Órdenes de Reparación" tab because the permissions aren't assigned to any role.
--
-- Distribution:
--   OWNER  → CREATE + READ + WRITE  (full control)
--   ADMIN  → CREATE + READ + WRITE  (workshop managers)
--   LECTOR → READ only              (reporting / read-only users)

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.system_role = true
  AND r.deleted = false
  AND (
      -- OWNER and ADMIN get all three permissions
      (UPPER(r.name) IN ('OWNER', 'ADMIN') AND p.name IN ('REPAIR_ORDER_CREATE', 'REPAIR_ORDER_READ', 'REPAIR_ORDER_WRITE'))
      OR
      -- LECTOR gets read-only
      (UPPER(r.name) = 'LECTOR' AND p.name = 'REPAIR_ORDER_READ')
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
