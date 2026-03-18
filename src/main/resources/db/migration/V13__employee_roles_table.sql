-- V13: Convert employee_role (single enum column) to employee_roles join table
-- This allows assigning multiple roles/positions to a single employee.

-- Step 1: Create the new join table, migrating existing single-role data
CREATE TABLE employee_roles (
    employee_id BIGINT      NOT NULL,
    role        VARCHAR(50) NOT NULL,
    CONSTRAINT fk_employee_roles_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- Step 2: Migrate existing data - copy the single role from each employee
INSERT INTO employee_roles (employee_id, role)
SELECT id, employee_role FROM employees;

-- Step 3: Drop the now-obsolete column
ALTER TABLE employees DROP COLUMN employee_role;
