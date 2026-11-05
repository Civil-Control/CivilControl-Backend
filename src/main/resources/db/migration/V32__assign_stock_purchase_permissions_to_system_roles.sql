-- V32: Insert STOCK_PURCHASE_* permissions and assign them to system roles
-- The PermissionSeeder creates permissions from AppPermissions.java on app start,
-- but only newly provisioned tenants get them assigned to roles via TenantProvisioningService.
-- Existing tenants need this migration to grant the permissions to their system roles.
--
-- Distribution:
--   OWNER  → READ + WRITE + DELETE  (full control)
--   ADMIN  → READ + WRITE + DELETE  (workshop managers)
--   LECTOR → READ only              (reporting / read-only users)

-- Step 1: Ensure the permissions exist in the permissions table
-- (PermissionSeeder will also handle this at startup, but we need them for the INSERT below)
INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description)
VALUES
    ('STOCK_PURCHASE_READ',   'Stock Purchases', 'mechanic',
     'Read all stock purchase records',
     'Ver Compras de Stock',
     'Permite ver todas las compras de stock del tenant'),
    ('STOCK_PURCHASE_WRITE',  'Stock Purchases', 'mechanic',
     'Create and edit stock purchases',
     'Crear/Editar Compras de Stock',
     'Permite registrar y modificar compras de stock'),
    ('STOCK_PURCHASE_DELETE', 'Stock Purchases', 'mechanic',
     'Delete stock purchases',
     'Eliminar Compras de Stock',
     'Permite eliminar compras de stock')
ON CONFLICT (name) DO NOTHING;

-- Step 2: Assign permissions to system roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.system_role = true
  AND r.deleted = false
  AND (
      (UPPER(r.name) IN ('OWNER', 'ADMIN') AND p.name IN ('STOCK_PURCHASE_READ', 'STOCK_PURCHASE_WRITE', 'STOCK_PURCHASE_DELETE'))
      OR
      (UPPER(r.name) = 'LECTOR' AND p.name = 'STOCK_PURCHASE_READ')
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
