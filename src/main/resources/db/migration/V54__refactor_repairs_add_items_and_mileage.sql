-- =====================================================================
-- V54: Refactor repairs — add items table, mileage, drop old columns
-- =====================================================================

-- 1. Add mileage column to repairs
ALTER TABLE repairs ADD COLUMN mileage INTEGER;

-- 2. Create repair_items table
CREATE TABLE repair_items (
    id BIGSERIAL PRIMARY KEY,
    repair_id BIGINT NOT NULL REFERENCES repairs(id) ON DELETE CASCADE,
    item_type VARCHAR(20) NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount DECIMAL(12,2),
    transactional_document_id BIGINT REFERENCES transactional_documents(id),
    sort_order INTEGER NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id)
);

CREATE INDEX idx_repair_items_repair_id ON repair_items(repair_id);
CREATE INDEX idx_repair_items_tenant_id ON repair_items(tenant_id);

-- 3. Migrate existing data: employee + cost → MANO_DE_OBRA item
INSERT INTO repair_items (repair_id, item_type, description, amount, transactional_document_id, sort_order, tenant_id)
SELECT r.id, 'MANO_DE_OBRA', COALESCE(r.employee, 'Mecánico'), r.cost, r.transactional_document_id, 0, r.tenant_id
FROM repairs r
WHERE r.employee IS NOT NULL OR r.cost IS NOT NULL;

-- 4. Migrate repair_types entries as MATERIAL items (no amount)
INSERT INTO repair_items (repair_id, item_type, description, amount, sort_order, tenant_id)
SELECT rt.repair_id, 'MATERIAL', rt.type, NULL, ROW_NUMBER() OVER (PARTITION BY rt.repair_id ORDER BY rt.type), r.tenant_id
FROM repair_types rt JOIN repairs r ON r.id = rt.repair_id;

-- 5. Drop obsolete columns from repairs
ALTER TABLE repairs DROP COLUMN IF EXISTS cost;
ALTER TABLE repairs DROP COLUMN IF EXISTS employee;
ALTER TABLE repairs DROP COLUMN IF EXISTS transactional_document_id;
ALTER TABLE repairs DROP COLUMN IF EXISTS document_sort_order;

-- 6. Drop the repair_types ElementCollection table
DROP TABLE IF EXISTS repair_types;
