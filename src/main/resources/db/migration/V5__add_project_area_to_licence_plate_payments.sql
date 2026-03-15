-- Add project_area_id to licence_plate_payments.
-- Allows associating a licence plate payment with a project area (like fuel loads).
ALTER TABLE licence_plate_payments
    ADD COLUMN IF NOT EXISTS project_area_id BIGINT,
    ADD CONSTRAINT IF NOT EXISTS fk_licence_plate_payments_project_area
        FOREIGN KEY (project_area_id) REFERENCES project_areas (id);
