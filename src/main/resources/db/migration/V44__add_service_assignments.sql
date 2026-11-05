-- ============================================
-- V44: Add service_assignments table and modify service_payments
-- ============================================

-- Create service_assignments table
CREATE TABLE IF NOT EXISTS service_assignments (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    service_supplier_id BIGINT          NOT NULL,
    building_id         BIGINT          NOT NULL,
    service_type        VARCHAR(50)     NOT NULL,
    account_number      VARCHAR(100),
    estimated_due_day   INTEGER,
    account_holder      VARCHAR(200),
    service_category    VARCHAR(50),
    payment_location_id BIGINT,
    project_area_id     BIGINT,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_service_assignments_service_supplier
        FOREIGN KEY (service_supplier_id) REFERENCES service_suppliers (id),
    CONSTRAINT fk_service_assignments_building
        FOREIGN KEY (building_id) REFERENCES buildings (id),
    CONSTRAINT fk_service_assignments_payment_location
        FOREIGN KEY (payment_location_id) REFERENCES buildings (id),
    CONSTRAINT fk_service_assignments_project_area
        FOREIGN KEY (project_area_id) REFERENCES project_areas (id),
    CONSTRAINT uq_service_assignments_tenant_supplier_building_type
        UNIQUE (tenant_id, service_supplier_id, building_id, service_type)
);

-- Modify service_payments: remove old direct references, add service_assignment_id

-- Step 1: Add service_assignment_id as NULLABLE first
ALTER TABLE service_payments ADD COLUMN service_assignment_id BIGINT;
ALTER TABLE service_payments ADD COLUMN project_area_id BIGINT;

-- Step 2: For each existing service_payment, create a service_assignment (if not already exists)
-- and link the payment to it
INSERT INTO service_assignments (tenant_id, service_supplier_id, building_id, service_type, deleted)
SELECT DISTINCT sp.tenant_id, sp.service_supplier_id, sp.building_id, sp.service_type, FALSE
FROM service_payments sp
WHERE sp.service_supplier_id IS NOT NULL
  AND sp.building_id IS NOT NULL
  AND sp.service_type IS NOT NULL
ON CONFLICT (tenant_id, service_supplier_id, building_id, service_type) DO NOTHING;

-- Step 3: Update existing service_payments to point to the created service_assignments
UPDATE service_payments sp
SET service_assignment_id = sa.id
FROM service_assignments sa
WHERE sa.tenant_id = sp.tenant_id
  AND sa.service_supplier_id = sp.service_supplier_id
  AND sa.building_id = sp.building_id
  AND sa.service_type = sp.service_type;

-- Step 4: Now enforce NOT NULL on service_assignment_id
ALTER TABLE service_payments ALTER COLUMN service_assignment_id SET NOT NULL;

-- Step 5: Drop old columns and constraints
ALTER TABLE service_payments DROP CONSTRAINT IF EXISTS fk_service_payments_service_supplier;
ALTER TABLE service_payments DROP CONSTRAINT IF EXISTS fk_service_payments_building;

ALTER TABLE service_payments DROP COLUMN IF EXISTS service_supplier_id;
ALTER TABLE service_payments DROP COLUMN IF EXISTS building_id;
ALTER TABLE service_payments DROP COLUMN IF EXISTS service_type;

-- Step 6: Add foreign key constraints
ALTER TABLE service_payments ADD CONSTRAINT fk_service_payments_service_assignment
    FOREIGN KEY (service_assignment_id) REFERENCES service_assignments (id);
ALTER TABLE service_payments ADD CONSTRAINT fk_service_payments_project_area
    FOREIGN KEY (project_area_id) REFERENCES project_areas (id);
