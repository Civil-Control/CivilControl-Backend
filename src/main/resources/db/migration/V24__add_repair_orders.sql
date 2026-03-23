-- V24__add_repair_orders.sql

CREATE TABLE repair_orders (
    id                  BIGSERIAL PRIMARY KEY,
    vehicle_id          BIGINT          NOT NULL REFERENCES vehicles(id),
    date                DATE            NOT NULL,
    description         TEXT            NOT NULL,
    reported_by         VARCHAR(100),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE',
    created_by_user_id  BIGINT          REFERENCES users(id),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    tenant_id           BIGINT          NOT NULL REFERENCES tenants(id)
);

CREATE INDEX idx_repair_orders_vehicle     ON repair_orders(vehicle_id);
CREATE INDEX idx_repair_orders_user        ON repair_orders(created_by_user_id);
CREATE INDEX idx_repair_orders_status      ON repair_orders(status);
CREATE INDEX idx_repair_orders_tenant      ON repair_orders(tenant_id);

ALTER TABLE repairs
    ADD COLUMN repair_order_id BIGINT REFERENCES repair_orders(id);

CREATE INDEX idx_repairs_repair_order ON repairs(repair_order_id);

INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description) VALUES
    ('REPAIR_ORDER_CREATE', 'Repair Orders', 'vehicles',
     'Create and manage own repair orders',
     'Crear Órdenes de Reparación',
     'Permite registrar nuevas órdenes y gestionar las propias'),
    ('REPAIR_ORDER_READ', 'Repair Orders', 'vehicles',
     'Read all repair orders',
     'Ver todas las Órdenes de Reparación',
     'Permite ver todas las órdenes del tenant'),
    ('REPAIR_ORDER_WRITE', 'Repair Orders', 'vehicles',
     'Manage repair order status and complete orders',
     'Gestionar Órdenes de Reparación',
     'Permite cambiar estado y completar órdenes')
ON CONFLICT (name) DO NOTHING;
