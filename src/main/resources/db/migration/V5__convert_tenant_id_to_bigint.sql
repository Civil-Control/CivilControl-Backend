-- ============================================
-- Flyway Migration V5
-- Convert tenant_id from VARCHAR to BIGINT
-- ============================================
-- Author: Maximo Andriola
-- Date: 2026-02-18
-- Description: Converts all tenant_id columns from VARCHAR(50) to BIGINT
-- Reason: Changed tenant_id to use numeric autoincremental IDs instead of strings
-- ============================================

-- ============================================
-- Step 1: Update tenants table
-- ============================================

-- Create new BIGINT id column
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS id_new BIGSERIAL;

-- Copy data: 'esea-sa' -> 1
UPDATE tenants SET id_new = 1 WHERE id = 'esea-sa';

-- Drop old foreign keys referencing tenants.id
ALTER TABLE employees DROP CONSTRAINT IF EXISTS fk_employees_tenant;
ALTER TABLE disciplinary_actions DROP CONSTRAINT IF EXISTS fk_disciplinary_actions_tenant;
ALTER TABLE employee_vacations DROP CONSTRAINT IF EXISTS fk_employee_vacations_tenant;
ALTER TABLE epp_deliveries DROP CONSTRAINT IF EXISTS fk_epp_deliveries_tenant;
ALTER TABLE salary_payments DROP CONSTRAINT IF EXISTS fk_salary_payments_tenant;
ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS fk_vehicles_tenant;
ALTER TABLE repairs DROP CONSTRAINT IF EXISTS fk_repairs_tenant;
ALTER TABLE licence_plate_payments DROP CONSTRAINT IF EXISTS fk_licence_plate_payments_tenant;
ALTER TABLE insurance_policies DROP CONSTRAINT IF EXISTS fk_insurance_policies_tenant;
ALTER TABLE auto_policies DROP CONSTRAINT IF EXISTS fk_auto_policies_tenant;
ALTER TABLE policy_vehicles DROP CONSTRAINT IF EXISTS fk_policy_vehicles_tenant;
ALTER TABLE buildings DROP CONSTRAINT IF EXISTS fk_buildings_tenant;
ALTER TABLE project_areas DROP CONSTRAINT IF EXISTS fk_project_areas_tenant;
ALTER TABLE suppliers DROP CONSTRAINT IF EXISTS fk_suppliers_tenant;
ALTER TABLE items DROP CONSTRAINT IF EXISTS fk_items_tenant;
ALTER TABLE item_details DROP CONSTRAINT IF EXISTS fk_item_details_tenant;
ALTER TABLE stocks DROP CONSTRAINT IF EXISTS fk_stocks_tenant;
ALTER TABLE contact_info DROP CONSTRAINT IF EXISTS fk_contact_info_tenant;
ALTER TABLE transactional_documents DROP CONSTRAINT IF EXISTS fk_transactional_documents_tenant;
ALTER TABLE payment_details DROP CONSTRAINT IF EXISTS fk_payment_details_tenant;
ALTER TABLE cash_payments DROP CONSTRAINT IF EXISTS fk_cash_payments_tenant;
ALTER TABLE transfer_payments DROP CONSTRAINT IF EXISTS fk_transfer_payments_tenant;
ALTER TABLE check_payments DROP CONSTRAINT IF EXISTS fk_check_payments_tenant;
ALTER TABLE gas_stations DROP CONSTRAINT IF EXISTS fk_gas_stations_tenant;
ALTER TABLE fuel_loads DROP CONSTRAINT IF EXISTS fk_fuel_loads_tenant;
ALTER TABLE service_suppliers DROP CONSTRAINT IF EXISTS fk_service_suppliers_tenant;
ALTER TABLE service_payments DROP CONSTRAINT IF EXISTS fk_service_payments_tenant;
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_tenant;
ALTER TABLE credentials DROP CONSTRAINT IF EXISTS fk_credentials_tenant;

-- Drop old primary key and rename columns in tenants table
ALTER TABLE tenants DROP CONSTRAINT IF EXISTS tenants_pkey;
ALTER TABLE tenants DROP COLUMN id;
ALTER TABLE tenants RENAME COLUMN id_new TO id;
ALTER TABLE tenants ADD PRIMARY KEY (id);

-- Rename sequence from id_new_seq to id_seq
ALTER SEQUENCE IF EXISTS tenants_id_new_seq RENAME TO tenants_id_seq;

-- Reset sequence
SELECT setval('tenants_id_seq', (SELECT MAX(id) FROM tenants));

-- ============================================
-- Step 2: Convert tenant_id columns from VARCHAR to BIGINT
-- ============================================

-- Employee module
ALTER TABLE employees ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE employees SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE employees DROP COLUMN tenant_id;
ALTER TABLE employees RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE employees ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE employees ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE disciplinary_actions ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE disciplinary_actions SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE disciplinary_actions DROP COLUMN tenant_id;
ALTER TABLE disciplinary_actions RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE disciplinary_actions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE disciplinary_actions ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE employee_vacations ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE employee_vacations SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE employee_vacations DROP COLUMN tenant_id;
ALTER TABLE employee_vacations RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE employee_vacations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE employee_vacations ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE epp_deliveries ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE epp_deliveries SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE epp_deliveries DROP COLUMN tenant_id;
ALTER TABLE epp_deliveries RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE epp_deliveries ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE epp_deliveries ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE salary_payments ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE salary_payments SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE salary_payments DROP COLUMN tenant_id;
ALTER TABLE salary_payments RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE salary_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE salary_payments ALTER COLUMN tenant_id SET DEFAULT 1;

-- Vehicle module
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE vehicles SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE vehicles DROP COLUMN tenant_id;
ALTER TABLE vehicles RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE vehicles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE vehicles ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE repairs ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE repairs SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE repairs DROP COLUMN tenant_id;
ALTER TABLE repairs RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE repairs ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE repairs ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE licence_plate_payments ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE licence_plate_payments SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE licence_plate_payments DROP COLUMN tenant_id;
ALTER TABLE licence_plate_payments RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE licence_plate_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE licence_plate_payments ALTER COLUMN tenant_id SET DEFAULT 1;

-- Insurance module
ALTER TABLE insurance_policies ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE insurance_policies SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE insurance_policies DROP COLUMN tenant_id;
ALTER TABLE insurance_policies RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE insurance_policies ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE insurance_policies ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE auto_policies ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE auto_policies SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE auto_policies DROP COLUMN tenant_id;
ALTER TABLE auto_policies RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE auto_policies ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE auto_policies ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE policy_vehicles ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE policy_vehicles SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE policy_vehicles DROP COLUMN tenant_id;
ALTER TABLE policy_vehicles RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE policy_vehicles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE policy_vehicles ALTER COLUMN tenant_id SET DEFAULT 1;

-- Company module
ALTER TABLE buildings ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE buildings SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE buildings DROP COLUMN tenant_id;
ALTER TABLE buildings RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE buildings ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE buildings ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE project_areas ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE project_areas SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE project_areas DROP COLUMN tenant_id;
ALTER TABLE project_areas RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE project_areas ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE project_areas ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE suppliers SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE suppliers DROP COLUMN tenant_id;
ALTER TABLE suppliers RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE suppliers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE suppliers ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE items ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE items SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE items DROP COLUMN tenant_id;
ALTER TABLE items RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE items ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE items ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE item_details ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE item_details SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE item_details DROP COLUMN tenant_id;
ALTER TABLE item_details RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE item_details ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE item_details ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE stocks ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE stocks SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE stocks DROP COLUMN tenant_id;
ALTER TABLE stocks RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE stocks ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE stocks ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE contact_info ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE contact_info SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE contact_info DROP COLUMN tenant_id;
ALTER TABLE contact_info RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE contact_info ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE contact_info ALTER COLUMN tenant_id SET DEFAULT 1;

-- Documents module
ALTER TABLE transactional_documents ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE transactional_documents SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE transactional_documents DROP COLUMN tenant_id;
ALTER TABLE transactional_documents RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE transactional_documents ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transactional_documents ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE payment_details ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE payment_details SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE payment_details DROP COLUMN tenant_id;
ALTER TABLE payment_details RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE payment_details ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE payment_details ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE cash_payments ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE cash_payments SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE cash_payments DROP COLUMN tenant_id;
ALTER TABLE cash_payments RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE cash_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE cash_payments ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE transfer_payments ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE transfer_payments SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE transfer_payments DROP COLUMN tenant_id;
ALTER TABLE transfer_payments RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE transfer_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transfer_payments ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE check_payments ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE check_payments SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE check_payments DROP COLUMN tenant_id;
ALTER TABLE check_payments RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE check_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE check_payments ALTER COLUMN tenant_id SET DEFAULT 1;

-- Gas Station module
ALTER TABLE gas_stations ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE gas_stations SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE gas_stations DROP COLUMN tenant_id;
ALTER TABLE gas_stations RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE gas_stations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE gas_stations ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE fuel_loads ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE fuel_loads SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE fuel_loads DROP COLUMN tenant_id;
ALTER TABLE fuel_loads RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE fuel_loads ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE fuel_loads ALTER COLUMN tenant_id SET DEFAULT 1;

-- Service Supplier module
ALTER TABLE service_suppliers ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE service_suppliers SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE service_suppliers DROP COLUMN tenant_id;
ALTER TABLE service_suppliers RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE service_suppliers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE service_suppliers ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE service_payments ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE service_payments SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE service_payments DROP COLUMN tenant_id;
ALTER TABLE service_payments RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE service_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE service_payments ALTER COLUMN tenant_id SET DEFAULT 1;

-- Security module
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE users SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE users DROP COLUMN tenant_id;
ALTER TABLE users RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN tenant_id SET DEFAULT 1;

ALTER TABLE credentials ADD COLUMN IF NOT EXISTS tenant_id_new BIGINT;
UPDATE credentials SET tenant_id_new = 1 WHERE tenant_id = 'esea-sa';
ALTER TABLE credentials DROP COLUMN tenant_id;
ALTER TABLE credentials RENAME COLUMN tenant_id_new TO tenant_id;
ALTER TABLE credentials ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE credentials ALTER COLUMN tenant_id SET DEFAULT 1;

-- ============================================
-- Step 3: Re-create foreign key constraints
-- ============================================

ALTER TABLE employees ADD CONSTRAINT fk_employees_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE disciplinary_actions ADD CONSTRAINT fk_disciplinary_actions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE employee_vacations ADD CONSTRAINT fk_employee_vacations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE epp_deliveries ADD CONSTRAINT fk_epp_deliveries_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE salary_payments ADD CONSTRAINT fk_salary_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE repairs ADD CONSTRAINT fk_repairs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE licence_plate_payments ADD CONSTRAINT fk_licence_plate_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE insurance_policies ADD CONSTRAINT fk_insurance_policies_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE auto_policies ADD CONSTRAINT fk_auto_policies_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE policy_vehicles ADD CONSTRAINT fk_policy_vehicles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE buildings ADD CONSTRAINT fk_buildings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE project_areas ADD CONSTRAINT fk_project_areas_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE suppliers ADD CONSTRAINT fk_suppliers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE items ADD CONSTRAINT fk_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE item_details ADD CONSTRAINT fk_item_details_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE stocks ADD CONSTRAINT fk_stocks_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE contact_info ADD CONSTRAINT fk_contact_info_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE transactional_documents ADD CONSTRAINT fk_transactional_documents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE payment_details ADD CONSTRAINT fk_payment_details_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE cash_payments ADD CONSTRAINT fk_cash_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE transfer_payments ADD CONSTRAINT fk_transfer_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE check_payments ADD CONSTRAINT fk_check_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE gas_stations ADD CONSTRAINT fk_gas_stations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE fuel_loads ADD CONSTRAINT fk_fuel_loads_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE service_suppliers ADD CONSTRAINT fk_service_suppliers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE service_payments ADD CONSTRAINT fk_service_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE credentials ADD CONSTRAINT fk_credentials_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- ============================================
-- Migration Complete
-- ============================================
-- All tenant_id columns converted from VARCHAR(50) to BIGINT
-- All data migrated: 'esea-sa' -> 1
-- Foreign key constraints re-created
-- ============================================


