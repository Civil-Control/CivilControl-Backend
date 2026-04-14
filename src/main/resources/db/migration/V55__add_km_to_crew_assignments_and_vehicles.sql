-- V55: Add km tracking to crew_assignments and vehicles, relax unique constraint

-- 1. Add km column to crew_assignments
ALTER TABLE crew_assignments ADD COLUMN km INTEGER;

-- 2. Add km column to vehicles
ALTER TABLE vehicles ADD COLUMN km INTEGER;

-- 3. Drop the unique constraint so multiple assignments per employee per day are allowed
ALTER TABLE crew_assignments DROP CONSTRAINT IF EXISTS uk_crew_employee_date;
