-- V33__add_sales_invoicing.sql
-- Feature 3: Sales Invoicing & Client Management
-- Creates tables: clients, sales_documents, sales_item_details
-- Inserts permissions: CLIENT_READ/WRITE/DELETE, SALES_DOCUMENT_READ/WRITE/DELETE

-- ============================================================
-- 1. CLIENT TABLE
-- ============================================================
CREATE TABLE clients (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    cuit                VARCHAR(100),
    business_name       VARCHAR(200) NOT NULL,
    trade_name          VARCHAR(200),
    iva_condition       VARCHAR(50) NOT NULL,
    -- Address (embedded)
    street              VARCHAR(200),
    number              INTEGER,
    city                VARCHAR(100),
    state               VARCHAR(100),
    country             VARCHAR(100),
    zip_code            VARCHAR(20),
    -- ContactInfo FK
    contact_info_id     BIGINT REFERENCES contact_info(id),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_clients_cuit_tenant UNIQUE (tenant_id, cuit)
);

CREATE INDEX idx_clients_tenant ON clients(tenant_id);
CREATE INDEX idx_clients_deleted ON clients(deleted);

-- ============================================================
-- 2. SALES DOCUMENTS TABLE
-- ============================================================
CREATE TABLE sales_documents (
    id                          BIGSERIAL PRIMARY KEY,
    tenant_id                   BIGINT NOT NULL,
    document_type               VARCHAR(50) NOT NULL,
    branch_code                 VARCHAR(5),
    document_number             VARCHAR(8),
    date                        DATE NOT NULL,
    client_id                   BIGINT REFERENCES clients(id),
    purchase_order_reference    VARCHAR(100),
    net_total                   NUMERIC(19, 2),
    iva_total                   NUMERIC(19, 2),
    iva_exempt_total            NUMERIC(19, 2),
    other_taxes                 NUMERIC(19, 2),
    total                       NUMERIC(19, 2),
    discount_percentage         NUMERIC(5, 2),
    project_area_id             BIGINT REFERENCES project_areas(id),
    paid                        BOOLEAN NOT NULL DEFAULT FALSE,
    comment                     VARCHAR(500),
    deleted                     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_sales_documents_tenant ON sales_documents(tenant_id);
CREATE INDEX idx_sales_documents_client ON sales_documents(client_id);
CREATE INDEX idx_sales_documents_date ON sales_documents(date);

-- ============================================================
-- 3. SALES ITEM DETAILS TABLE
-- ============================================================
CREATE TABLE sales_item_details (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    item_id             BIGINT NOT NULL REFERENCES items(id),
    sales_document_id   BIGINT NOT NULL REFERENCES sales_documents(id),
    unit_amount         NUMERIC(19, 2) NOT NULL,
    quantity            INTEGER NOT NULL,
    iva_percentage      NUMERIC(5, 2) NOT NULL,
    total_amount        NUMERIC(19, 2) NOT NULL
);

CREATE INDEX idx_sales_item_details_tenant ON sales_item_details(tenant_id);
CREATE INDEX idx_sales_item_details_doc ON sales_item_details(sales_document_id);

-- ============================================================
-- 4. INSERT PERMISSIONS
-- ============================================================
INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description)
VALUES
    ('CLIENT_READ',             'Clients', 'clients',
     'Read all client records',
     'Ver Clientes',
     'Permite ver todos los clientes del tenant'),
    ('CLIENT_WRITE',            'Clients', 'clients',
     'Create and edit clients',
     'Crear/Editar Clientes',
     'Permite crear y modificar clientes'),
    ('CLIENT_DELETE',           'Clients', 'clients',
     'Delete clients',
     'Eliminar Clientes',
     'Permite eliminar clientes'),
    ('SALES_DOCUMENT_READ',     'Sales Documents', 'clients',
     'Read all sales document records',
     'Ver Facturas de Venta',
     'Permite ver todas las facturas de venta del tenant'),
    ('SALES_DOCUMENT_WRITE',    'Sales Documents', 'clients',
     'Create and edit sales documents',
     'Crear/Editar Facturas de Venta',
     'Permite registrar y modificar facturas de venta'),
    ('SALES_DOCUMENT_DELETE',   'Sales Documents', 'clients',
     'Delete sales documents',
     'Eliminar Facturas de Venta',
     'Permite eliminar facturas de venta')
ON CONFLICT (name) DO NOTHING;

-- ============================================================
-- 5. ASSIGN PERMISSIONS TO SYSTEM ROLES
-- ============================================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.system_role = true
  AND r.deleted = false
  AND (
      (UPPER(r.name) IN ('OWNER', 'ADMIN') AND p.name IN (
          'CLIENT_READ', 'CLIENT_WRITE', 'CLIENT_DELETE',
          'SALES_DOCUMENT_READ', 'SALES_DOCUMENT_WRITE', 'SALES_DOCUMENT_DELETE'
      ))
      OR
      (UPPER(r.name) = 'LECTOR' AND p.name IN ('CLIENT_READ', 'SALES_DOCUMENT_READ'))
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
