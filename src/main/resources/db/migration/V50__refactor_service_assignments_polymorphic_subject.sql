-- ============================================================================
-- V50: Refactor service_assignments to support building OR vehicle subjects,
--      simplify service_payments to always reference an assignment.
-- ============================================================================

-- 1. Add subject_type and vehicle_id to service_assignments
ALTER TABLE service_assignments ADD COLUMN subject_type VARCHAR(20);
ALTER TABLE service_assignments ADD COLUMN vehicle_id BIGINT;

-- 2. Backfill all existing assignments as BUILDING
UPDATE service_assignments SET subject_type = 'BUILDING';
ALTER TABLE service_assignments ALTER COLUMN subject_type SET NOT NULL;

-- 3. Make building_id nullable (vehicle assignments won't have one)
ALTER TABLE service_assignments ALTER COLUMN building_id DROP NOT NULL;

-- 4. Add FK for vehicle
ALTER TABLE service_assignments
    ADD CONSTRAINT fk_service_assignment_vehicle
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id);

-- 5. Remove service_category from service_assignments (RODADO/INMUEBLE no longer needed)
ALTER TABLE service_assignments DROP COLUMN IF EXISTS service_category;

-- 6. Remove project_area_id from service_assignments
ALTER TABLE service_assignments DROP COLUMN IF EXISTS project_area_id;

-- 7. Delete orphan VEHICLE-type payments that have no assignment (test data from V49)
DELETE FROM service_payments WHERE subject_type = 'VEHICLE' AND service_assignment_id IS NULL;

-- 8. Remove vehicle_id and subject_type from service_payments
ALTER TABLE service_payments DROP COLUMN IF EXISTS vehicle_id;
ALTER TABLE service_payments DROP COLUMN IF EXISTS subject_type;

-- 9. Make service_assignment_id NOT NULL on service_payments
ALTER TABLE service_payments ALTER COLUMN service_assignment_id SET NOT NULL;
