CREATE TABLE purchase_orders (
    id                          BIGSERIAL PRIMARY KEY,
    date                        DATE NOT NULL,
    category                    VARCHAR(30) NOT NULL,
    description                 TEXT NOT NULL,
    requested_by                VARCHAR(100),
    estimated_amount            DECIMAL(12, 2),
    priority                    VARCHAR(10) NOT NULL DEFAULT 'MEDIA',
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    transactional_document_id   BIGINT REFERENCES transactional_documents(id),
    created_by_user_id          BIGINT REFERENCES users(id),
    deleted                     BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id                   BIGINT NOT NULL REFERENCES tenants(id)
);

CREATE TABLE purchase_order_items (
    purchase_order_id   BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    item                VARCHAR(200) NOT NULL
);

INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description) VALUES
    ('PURCHASE_ORDER_CREATE', 'Purchase Orders', 'administration',
     'Create and manage own purchase orders',
     'Crear Órdenes de Compra',
     'Permite registrar nuevas órdenes y gestionar las propias'),
    ('PURCHASE_ORDER_READ', 'Purchase Orders', 'administration',
     'Read all purchase orders of the tenant',
     'Ver todas las Órdenes de Compra',
     'Permite ver todas las órdenes con filtros completos'),
    ('PURCHASE_ORDER_WRITE', 'Purchase Orders', 'administration',
     'Manage purchase order status and link fiscal documents',
     'Gestionar Órdenes de Compra',
     'Permite cambiar estado y vincular comprobantes fiscales');
