-- V27: Fix REPAIR_ORDER_* permissions metadata and missing role assignments
--
-- Problems fixed:
--   1. work_module was set to 'vehicles' in V24/V25 but should be 'mechanic'
--      (same grouping as REPAIR_* and STOCK_* → appears in "Mecánica", not "Vehículos")
--   2. Any role named 'Root'/'ROOT' was not included in V26 (V26 only targeted
--      system_role=true OWNER/ADMIN/LECTOR)
--   3. Any custom role that already has REPAIR_WRITE (full workshop access) should
--      also receive the REPAIR_ORDER_* permissions

-- 1. Fix work_module grouping
UPDATE permissions
SET work_module = 'mechanic'
WHERE name IN ('REPAIR_ORDER_CREATE', 'REPAIR_ORDER_READ', 'REPAIR_ORDER_WRITE');

-- 2. Assign all 3 permissions to any non-deleted role named 'Root' (case-insensitive)
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.deleted = false
  AND UPPER(r.name) = 'ROOT'
  AND p.name IN ('REPAIR_ORDER_CREATE', 'REPAIR_ORDER_READ', 'REPAIR_ORDER_WRITE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Assign all 3 permissions to any non-deleted custom role that already has REPAIR_WRITE
--    (these are full-access workshop roles that missed V26)
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM roles r
INNER JOIN role_permissions rp_write ON rp_write.role_id = r.id
INNER JOIN permissions write_p ON write_p.id = rp_write.permission_id
    AND write_p.name = 'REPAIR_WRITE'
CROSS JOIN permissions p
WHERE r.deleted = false
  AND p.name IN ('REPAIR_ORDER_CREATE', 'REPAIR_ORDER_READ', 'REPAIR_ORDER_WRITE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
