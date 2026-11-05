-- =============================================================================
-- V2__add_vehicle_type_fk_to_vehicles.sql
-- Migrates the vehicle_type column (VARCHAR enum) to vehicle_type_id (FK).
-- The vehicle_types table was already created in V1.
-- Idempotent: safe to re-execute regardless of partial prior state.
-- =============================================================================

-- 1. Add vehicle_type_id to vehicles (only if the column does not exist yet)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'vehicles' AND column_name = 'vehicle_type_id'
    ) THEN
        ALTER TABLE vehicles ADD COLUMN vehicle_type_id BIGINT;
    END IF;
END $$;

-- 2. Add the FK constraint (only if it does not exist yet)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_vehicles_vehicle_type'
    ) THEN
        ALTER TABLE vehicles
            ADD CONSTRAINT fk_vehicles_vehicle_type
                FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types (id);
    END IF;
END $$;

-- 3. Drop the old enum column (only if it still exists)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'vehicles' AND column_name = 'vehicle_type'
    ) THEN
        ALTER TABLE vehicles DROP COLUMN vehicle_type;
    END IF;
END $$;


