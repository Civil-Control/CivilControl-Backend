-- Soft-deleted custom fuel types must free up their key for reuse (e.g. the user
-- mistypes a name, deletes it, and wants to create the correctly-spelled one).
-- The plain (tenant_id, key) unique constraint from V98 ignored the deleted flag and
-- would still reject that re-creation. Replace it with a partial unique index that
-- only applies to active (non-deleted) rows.

ALTER TABLE custom_fuel_types DROP CONSTRAINT IF EXISTS uq_custom_fuel_types_tenant_key;

CREATE UNIQUE INDEX uq_custom_fuel_types_tenant_key
    ON custom_fuel_types (tenant_id, key)
    WHERE deleted = false;
