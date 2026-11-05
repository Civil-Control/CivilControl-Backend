-- V34__add_work_contracts_and_certifications.sql
-- Feature 4: Certificaciones de Obra y Seguimiento de Facturación por Contrato
-- Creates tables: work_contracts, certifications
-- Inserts permissions: WORK_CONTRACT_READ/WRITE/DELETE, CERTIFICATION_READ/WRITE/DELETE

-- ============================================================
-- 1. WORK CONTRACTS TABLE
-- ============================================================
CREATE TABLE work_contracts (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL REFERENCES tenants(id),
    contract_number     VARCHAR(50)     NOT NULL,
    client_id           BIGINT          NOT NULL REFERENCES clients(id),
    project_area_id     BIGINT          REFERENCES project_areas(id),
    description         VARCHAR(1000)   NOT NULL,
    contract_date       DATE            NOT NULL,
    end_date            DATE,
    contracted_amount   NUMERIC(19,2)   NOT NULL,
    currency            VARCHAR(5)      NOT NULL DEFAULT 'ARS',
    status              VARCHAR(15)     NOT NULL DEFAULT 'ACTIVO',
    comment             VARCHAR(500),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    UNIQUE (tenant_id, contract_number)
);

CREATE INDEX idx_work_contracts_tenant    ON work_contracts(tenant_id);
CREATE INDEX idx_work_contracts_client    ON work_contracts(client_id);
CREATE INDEX idx_work_contracts_project_area ON work_contracts(project_area_id);
CREATE INDEX idx_work_contracts_status    ON work_contracts(status);
CREATE INDEX idx_work_contracts_deleted   ON work_contracts(deleted);

-- ============================================================
-- 2. CERTIFICATIONS TABLE
-- ============================================================
CREATE TABLE certifications (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL REFERENCES tenants(id),
    certification_number    INTEGER         NOT NULL,
    work_contract_id        BIGINT          NOT NULL REFERENCES work_contracts(id),
    certification_date      DATE            NOT NULL,
    certified_amount        NUMERIC(19,2)   NOT NULL,
    sales_document_id       BIGINT          REFERENCES sales_documents(id),
    status                  VARCHAR(15)     NOT NULL DEFAULT 'PRESENTADO',
    comment                 VARCHAR(500),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_certifications_tenant         ON certifications(tenant_id);
CREATE INDEX idx_certifications_work_contract  ON certifications(work_contract_id);
CREATE INDEX idx_certifications_sales_document ON certifications(sales_document_id);
CREATE INDEX idx_certifications_status         ON certifications(status);
CREATE INDEX idx_certifications_deleted        ON certifications(deleted);

-- ============================================================
-- 3. INSERT PERMISSIONS
-- ============================================================
INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description)
VALUES
    ('WORK_CONTRACT_READ',
     'WorkContracts', 'contracts',
     'Read work contracts',
     'Ver Contratos',
     'Permite ver el listado de contratos de obra'),
    ('WORK_CONTRACT_WRITE',
     'WorkContracts', 'contracts',
     'Create and update work contracts',
     'Crear/Editar Contratos',
     'Permite crear y modificar contratos de obra'),
    ('WORK_CONTRACT_DELETE',
     'WorkContracts', 'contracts',
     'Delete work contracts',
     'Eliminar Contratos',
     'Permite eliminar contratos de obra'),
    ('CERTIFICATION_READ',
     'Certifications', 'contracts',
     'Read certifications',
     'Ver Certificaciones',
     'Permite ver las certificaciones de avance de obra'),
    ('CERTIFICATION_WRITE',
     'Certifications', 'contracts',
     'Create and update certifications',
     'Crear/Editar Certificaciones',
     'Permite crear y modificar certificaciones de avance'),
    ('CERTIFICATION_DELETE',
     'Certifications', 'contracts',
     'Delete certifications',
     'Eliminar Certificaciones',
     'Permite eliminar certificaciones de avance')
ON CONFLICT (name) DO NOTHING;
