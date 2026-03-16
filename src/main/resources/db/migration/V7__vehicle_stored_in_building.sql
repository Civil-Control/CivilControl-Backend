-- Migrate vehicle stored_in from free text to FK reference to buildings
-- Idempotent: safe to run even if column/constraint already exist or stored_in already dropped.
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS building_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_vehicles_building'
    ) THEN
        ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_building FOREIGN KEY (building_id) REFERENCES buildings(id);
    END IF;
END $$;

ALTER TABLE vehicles DROP COLUMN IF EXISTS stored_in;
