-- Add project_area_id to licence_plate_payments.
-- Allows associating a licence plate payment with a project area (like fuel loads).
-- Idempotent: safe to run even if column/constraint already exist.
ALTER TABLE licence_plate_payments
    ADD COLUMN IF NOT EXISTS project_area_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_licence_plate_payments_project_area'
    ) THEN
        ALTER TABLE licence_plate_payments
            ADD CONSTRAINT fk_licence_plate_payments_project_area
                FOREIGN KEY (project_area_id) REFERENCES project_areas (id);
    END IF;
END $$;
