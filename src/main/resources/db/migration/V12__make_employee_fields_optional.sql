-- Make optional employee fields nullable (dni, cuil, hire_date)
ALTER TABLE employees ALTER COLUMN dni DROP NOT NULL;
ALTER TABLE employees ALTER COLUMN cuil DROP NOT NULL;
ALTER TABLE employees ALTER COLUMN hire_date DROP NOT NULL;

-- Ensure project_area_id is required (NOT NULL)
ALTER TABLE employees ALTER COLUMN project_area_id SET NOT NULL;
