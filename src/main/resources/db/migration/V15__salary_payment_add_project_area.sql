-- Add independent project_area_id to salary_payments
-- This allows each payment to be assigned to a project area independently of the employee's area
ALTER TABLE salary_payments
    ADD COLUMN project_area_id BIGINT REFERENCES project_areas(id);
