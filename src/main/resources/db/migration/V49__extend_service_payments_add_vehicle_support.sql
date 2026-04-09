-- V49: Extend service_payments to support both building-based and vehicle-based payments.
-- Adds subject_type, vehicle_id, year, and period columns.
-- Makes service_assignment_id nullable (only required for BUILDING subject type).
-- Migrates existing data to default subject_type = 'BUILDING'.

-- Step 1: Add new columns
ALTER TABLE service_payments ADD COLUMN subject_type VARCHAR(30);
ALTER TABLE service_payments ADD COLUMN vehicle_id BIGINT;
ALTER TABLE service_payments ADD COLUMN year INTEGER;
ALTER TABLE service_payments ADD COLUMN period INTEGER;

-- Step 2: Set default subject_type for existing rows
UPDATE service_payments SET subject_type = 'BUILDING' WHERE subject_type IS NULL;

-- Step 3: Make subject_type NOT NULL after backfill
ALTER TABLE service_payments ALTER COLUMN subject_type SET NOT NULL;

-- Step 4: Make service_assignment_id nullable (was NOT NULL for building-only payments)
ALTER TABLE service_payments ALTER COLUMN service_assignment_id DROP NOT NULL;

-- Step 5: Add FK for vehicle_id
ALTER TABLE service_payments ADD CONSTRAINT fk_service_payments_vehicle
    FOREIGN KEY (vehicle_id) REFERENCES vehicles (id);

-- Step 6: Drop the licence_plate_payments table (data migrated to service_payments)
DROP TABLE IF EXISTS licence_plate_payments;
