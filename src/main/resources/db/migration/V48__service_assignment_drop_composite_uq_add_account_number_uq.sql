-- Drop the old composite unique constraint (supplier + building + service type)
ALTER TABLE service_assignments DROP CONSTRAINT IF EXISTS uq_service_assignments_tenant_supplier_building_type;

-- Add partial unique index for account_number per tenant (only non-null, non-deleted)
CREATE UNIQUE INDEX uq_service_assignments_tenant_account_number
    ON service_assignments (tenant_id, account_number)
    WHERE deleted = false AND account_number IS NOT NULL AND account_number <> '';
