-- ============================================
-- Flyway Migration V4
-- Implement Multi-Tenancy
-- ============================================
-- Author: Maximo Andriola
-- Date: 2026-02-18
-- Description: Adds tenant_id column (BIGINT) to all tables and creates composite unique indexes
-- Strategy: Database Shared, Schema Shared, Discriminator Column (tenant_id)
-- ============================================

-- ============================================
-- Step 1: Create tenants table
-- ============================================
CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tenants IS 'Stores tenant information for multi-tenancy';
COMMENT ON COLUMN tenants.id IS 'Auto-incremental tenant ID';

-- Insert default tenant with ID 1
INSERT INTO tenants (id, name, active)
VALUES (1, 'ESEA SA', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Reset sequence to start from 2
SELECT setval('tenants_id_seq', (SELECT MAX(id) FROM tenants));

-- ============================================
-- Step 2: Add tenant_id column to all tenant-scoped tables
-- ============================================

-- Employee module
ALTER TABLE employees ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE disciplinary_actions ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE employee_vacations ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE epp_deliveries ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE salary_payments ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- Vehicle module
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE repairs ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE licence_plate_payments ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- Insurance module
ALTER TABLE insurance_policies ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE auto_policies ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE policy_vehicles ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- Company module
ALTER TABLE buildings ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE project_areas ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE items ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE item_details ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE contact_info ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- Documents module
ALTER TABLE transactional_documents ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE payment_details ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE cash_payments ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE transfer_payments ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE check_payments ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- Gas Station module
ALTER TABLE gas_stations ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fuel_loads ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- Service Supplier module
ALTER TABLE service_suppliers ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE service_payments ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- Security module
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE credentials ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- ============================================
-- Step 3: Drop old unique constraints and create composite unique constraints
-- ============================================

-- Employees: tenant_id + dni, tenant_id + cuil
ALTER TABLE employees DROP CONSTRAINT IF EXISTS employees_dni_key;
ALTER TABLE employees DROP CONSTRAINT IF EXISTS employees_cuil_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_employees_tenant_dni ON employees(tenant_id, dni);
CREATE UNIQUE INDEX IF NOT EXISTS uk_employees_tenant_cuil ON employees(tenant_id, cuil);

-- Vehicles: tenant_id + license_plate
ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS vehicles_license_plate_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_vehicles_tenant_license_plate ON vehicles(tenant_id, license_plate);

-- Suppliers: tenant_id + cuit, tenant_id + legal_name
ALTER TABLE suppliers DROP CONSTRAINT IF EXISTS suppliers_cuit_key;
ALTER TABLE suppliers DROP CONSTRAINT IF EXISTS suppliers_legal_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_suppliers_tenant_cuit ON suppliers(tenant_id, cuit);
CREATE UNIQUE INDEX IF NOT EXISTS uk_suppliers_tenant_legal_name ON suppliers(tenant_id, legal_name);

-- Buildings: tenant_id + code
ALTER TABLE buildings DROP CONSTRAINT IF EXISTS buildings_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_buildings_tenant_code ON buildings(tenant_id, code);

-- ProjectAreas: tenant_id + name
ALTER TABLE project_areas DROP CONSTRAINT IF EXISTS project_areas_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_project_areas_tenant_name ON project_areas(tenant_id, name);

-- Users: tenant_id + email
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_tenant_email ON users(tenant_id, email);

-- Credentials: tenant_id + username
ALTER TABLE credentials DROP CONSTRAINT IF EXISTS credentials_username_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_credentials_tenant_username ON credentials(tenant_id, username);

-- InsurancePolicy: tenant_id + policy_number
ALTER TABLE insurance_policies DROP CONSTRAINT IF EXISTS insurance_policies_policy_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_insurance_policies_tenant_policy_number ON insurance_policies(tenant_id, policy_number);

-- TransactionalDocuments: tenant_id + branch_code + document_number + supplier_id
-- First drop old constraint
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'transactional_documents_branch_code_document_number_supplier__key') THEN
        ALTER TABLE transactional_documents DROP CONSTRAINT transactional_documents_branch_code_document_number_supplier__key;
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uk_transactional_documents_tenant_composite ON transactional_documents(tenant_id, branch_code, document_number, supplier_id);

-- ServicePayment: tenant_id + reference_number
ALTER TABLE service_payments DROP CONSTRAINT IF EXISTS uk_service_payment_reference_number;
CREATE UNIQUE INDEX IF NOT EXISTS uk_service_payments_tenant_reference_number ON service_payments(tenant_id, reference_number);

-- ============================================
-- Step 4: Add foreign key constraints to tenants table
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
-- Step 5: Create indexes for tenant_id for query performance
-- ============================================

CREATE INDEX IF NOT EXISTS idx_employees_tenant_id ON employees(tenant_id);
CREATE INDEX IF NOT EXISTS idx_disciplinary_actions_tenant_id ON disciplinary_actions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_employee_vacations_tenant_id ON employee_vacations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_epp_deliveries_tenant_id ON epp_deliveries(tenant_id);
CREATE INDEX IF NOT EXISTS idx_salary_payments_tenant_id ON salary_payments(tenant_id);

CREATE INDEX IF NOT EXISTS idx_vehicles_tenant_id ON vehicles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_repairs_tenant_id ON repairs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_licence_plate_payments_tenant_id ON licence_plate_payments(tenant_id);

CREATE INDEX IF NOT EXISTS idx_insurance_policies_tenant_id ON insurance_policies(tenant_id);
CREATE INDEX IF NOT EXISTS idx_auto_policies_tenant_id ON auto_policies(tenant_id);
CREATE INDEX IF NOT EXISTS idx_policy_vehicles_tenant_id ON policy_vehicles(tenant_id);

CREATE INDEX IF NOT EXISTS idx_buildings_tenant_id ON buildings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_project_areas_tenant_id ON project_areas(tenant_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_tenant_id ON suppliers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_items_tenant_id ON items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_item_details_tenant_id ON item_details(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stocks_tenant_id ON stocks(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contact_info_tenant_id ON contact_info(tenant_id);

CREATE INDEX IF NOT EXISTS idx_transactional_documents_tenant_id ON transactional_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_details_tenant_id ON payment_details(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cash_payments_tenant_id ON cash_payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_transfer_payments_tenant_id ON transfer_payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_check_payments_tenant_id ON check_payments(tenant_id);

CREATE INDEX IF NOT EXISTS idx_gas_stations_tenant_id ON gas_stations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_fuel_loads_tenant_id ON fuel_loads(tenant_id);

CREATE INDEX IF NOT EXISTS idx_service_suppliers_tenant_id ON service_suppliers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_service_payments_tenant_id ON service_payments(tenant_id);

CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_credentials_tenant_id ON credentials(tenant_id);

-- ============================================
-- Migration Complete
-- ============================================
-- All tables now have tenant_id column (BIGINT) with default value 1
-- Unique constraints are now composite with tenant_id
-- Foreign key constraints to tenants table are in place
-- Indexes created for optimal query performance
-- Default tenant: ID = 1, Name = 'ESEA SA'
-- ============================================



