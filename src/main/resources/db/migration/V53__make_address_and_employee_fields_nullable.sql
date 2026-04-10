-- =====================================================
-- V53: Make address fields nullable across all entities
-- and relax employee field constraints
-- =====================================================

-- ── Address columns: employees ──
ALTER TABLE employees ALTER COLUMN street DROP NOT NULL;
ALTER TABLE employees ALTER COLUMN number DROP NOT NULL;
ALTER TABLE employees ALTER COLUMN city DROP NOT NULL;
ALTER TABLE employees ALTER COLUMN state DROP NOT NULL;
ALTER TABLE employees ALTER COLUMN country DROP NOT NULL;
ALTER TABLE employees ALTER COLUMN zip_code DROP NOT NULL;

-- ── Address columns: tenants ──
ALTER TABLE tenants ALTER COLUMN street DROP NOT NULL;
ALTER TABLE tenants ALTER COLUMN number DROP NOT NULL;
ALTER TABLE tenants ALTER COLUMN city DROP NOT NULL;
ALTER TABLE tenants ALTER COLUMN state DROP NOT NULL;
ALTER TABLE tenants ALTER COLUMN country DROP NOT NULL;
ALTER TABLE tenants ALTER COLUMN zip_code DROP NOT NULL;

-- ── Address columns: buildings ──
ALTER TABLE buildings ALTER COLUMN street DROP NOT NULL;
ALTER TABLE buildings ALTER COLUMN number DROP NOT NULL;
ALTER TABLE buildings ALTER COLUMN city DROP NOT NULL;
ALTER TABLE buildings ALTER COLUMN state DROP NOT NULL;
ALTER TABLE buildings ALTER COLUMN country DROP NOT NULL;
ALTER TABLE buildings ALTER COLUMN zip_code DROP NOT NULL;

-- ── Address columns: clients ──
ALTER TABLE clients ALTER COLUMN street DROP NOT NULL;
ALTER TABLE clients ALTER COLUMN number DROP NOT NULL;
ALTER TABLE clients ALTER COLUMN city DROP NOT NULL;
ALTER TABLE clients ALTER COLUMN state DROP NOT NULL;
ALTER TABLE clients ALTER COLUMN country DROP NOT NULL;
ALTER TABLE clients ALTER COLUMN zip_code DROP NOT NULL;

-- ── Address columns: suppliers ──
ALTER TABLE suppliers ALTER COLUMN street DROP NOT NULL;
ALTER TABLE suppliers ALTER COLUMN number DROP NOT NULL;
ALTER TABLE suppliers ALTER COLUMN city DROP NOT NULL;
ALTER TABLE suppliers ALTER COLUMN state DROP NOT NULL;
ALTER TABLE suppliers ALTER COLUMN country DROP NOT NULL;
ALTER TABLE suppliers ALTER COLUMN zip_code DROP NOT NULL;

-- ── Employee-specific: relax constraints ──
ALTER TABLE employees ALTER COLUMN project_area_id DROP NOT NULL;
ALTER TABLE employees ALTER COLUMN employee_status DROP NOT NULL;
