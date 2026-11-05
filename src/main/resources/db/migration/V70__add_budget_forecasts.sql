-- =============================================================================
-- Feature 17: Previsiones de Gastos (Budget Forecasts)
-- =============================================================================
-- Crea las tablas para gestionar previsiones de gastos por período y plantillas
-- frecuentes. Cada previsión es una "planilla" de items esperados que pueden
-- materializarse en registros reales (SalaryPayment, ServicePayment, Repair,
-- StockPurchase) mediante el patrón Strategy de "appliers".
-- =============================================================================

-- ───────────────────────────────────────────────────────────────────────────
-- Cabecera de previsión
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE budget_forecasts (
    id                       BIGSERIAL    PRIMARY KEY,
    tenant_id                BIGINT       NOT NULL,
    name                     VARCHAR(200) NOT NULL,
    description              VARCHAR(1000),
    period_from              DATE         NOT NULL,
    period_to                DATE         NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    total_amount             NUMERIC(19,2) NOT NULL DEFAULT 0,
    applied_amount           NUMERIC(19,2) NOT NULL DEFAULT 0,
    created_from_template_id BIGINT,
    confirmed_at             TIMESTAMP,
    confirmed_by_user_id     BIGINT,
    closed_at                TIMESTAMP,
    closed_by_user_id        BIGINT,
    deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_budget_forecasts_period CHECK (period_to >= period_from),
    CONSTRAINT chk_budget_forecasts_status CHECK (status IN ('BORRADOR','CONFIRMADA','CERRADA'))
);

CREATE INDEX idx_budget_forecasts_tenant_period ON budget_forecasts(tenant_id, period_from, period_to);
CREATE INDEX idx_budget_forecasts_status        ON budget_forecasts(tenant_id, status);
CREATE INDEX idx_budget_forecasts_deleted       ON budget_forecasts(tenant_id, deleted);

-- ───────────────────────────────────────────────────────────────────────────
-- Items de previsión
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE budget_forecast_items (
    id                    BIGSERIAL    PRIMARY KEY,
    tenant_id             BIGINT       NOT NULL,
    budget_forecast_id    BIGINT       NOT NULL,
    row_order             INT          NOT NULL,
    item_type             VARCHAR(30)  NOT NULL,
    description           VARCHAR(500) NOT NULL,
    expected_date         DATE         NOT NULL,
    expected_amount       NUMERIC(19,2) NOT NULL,
    employee_id           BIGINT,
    supplier_id           BIGINT,
    service_assignment_id BIGINT,
    vehicle_id            BIGINT,
    stock_id              BIGINT,
    stock_quantity        NUMERIC(19,4),
    application_status    VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    applied_entity_type   VARCHAR(30),
    applied_entity_id     BIGINT,
    applied_at            TIMESTAMP,
    applied_by_user_id    BIGINT,
    skip_reason           VARCHAR(500),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bf_item_forecast    FOREIGN KEY (budget_forecast_id)    REFERENCES budget_forecasts(id) ON DELETE CASCADE,
    CONSTRAINT fk_bf_item_employee    FOREIGN KEY (employee_id)           REFERENCES employees(id),
    CONSTRAINT fk_bf_item_supplier    FOREIGN KEY (supplier_id)           REFERENCES suppliers(id),
    CONSTRAINT fk_bf_item_assignment  FOREIGN KEY (service_assignment_id) REFERENCES service_assignments(id),
    CONSTRAINT fk_bf_item_vehicle     FOREIGN KEY (vehicle_id)            REFERENCES vehicles(id),
    CONSTRAINT fk_bf_item_stock       FOREIGN KEY (stock_id)              REFERENCES stocks(id),
    CONSTRAINT chk_bf_item_type       CHECK (item_type IN ('SALARIO','SERVICIO','REPARACION','COMPRA_STOCK','PATENTE','OTRO')),
    CONSTRAINT chk_bf_item_app_status CHECK (application_status IN ('PENDIENTE','APLICADO','OMITIDO'))
);

CREATE INDEX idx_bf_items_forecast ON budget_forecast_items(budget_forecast_id, row_order);
CREATE INDEX idx_bf_items_status   ON budget_forecast_items(application_status);
CREATE INDEX idx_bf_items_tenant   ON budget_forecast_items(tenant_id);

-- ───────────────────────────────────────────────────────────────────────────
-- Plantillas frecuentes
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE budget_forecast_templates (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    name                VARCHAR(200) NOT NULL,
    description         VARCHAR(1000),
    default_period_days INT          NOT NULL DEFAULT 7,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_bf_template_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_bf_templates_tenant ON budget_forecast_templates(tenant_id, deleted);

-- ───────────────────────────────────────────────────────────────────────────
-- Items de plantilla
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE budget_forecast_template_items (
    id                    BIGSERIAL    PRIMARY KEY,
    tenant_id             BIGINT       NOT NULL,
    template_id           BIGINT       NOT NULL,
    row_order             INT          NOT NULL,
    item_type             VARCHAR(30)  NOT NULL,
    description           VARCHAR(500) NOT NULL,
    day_offset            INT          NOT NULL DEFAULT 0,
    expected_amount       NUMERIC(19,2) NOT NULL,
    employee_id           BIGINT,
    supplier_id           BIGINT,
    service_assignment_id BIGINT,
    vehicle_id            BIGINT,
    stock_id              BIGINT,
    stock_quantity        NUMERIC(19,4),

    CONSTRAINT fk_bf_tmpl_item_template   FOREIGN KEY (template_id)           REFERENCES budget_forecast_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_bf_tmpl_item_employee   FOREIGN KEY (employee_id)           REFERENCES employees(id),
    CONSTRAINT fk_bf_tmpl_item_supplier   FOREIGN KEY (supplier_id)           REFERENCES suppliers(id),
    CONSTRAINT fk_bf_tmpl_item_assignment FOREIGN KEY (service_assignment_id) REFERENCES service_assignments(id),
    CONSTRAINT fk_bf_tmpl_item_vehicle    FOREIGN KEY (vehicle_id)            REFERENCES vehicles(id),
    CONSTRAINT fk_bf_tmpl_item_stock      FOREIGN KEY (stock_id)              REFERENCES stocks(id),
    CONSTRAINT chk_bf_tmpl_item_type      CHECK (item_type IN ('SALARIO','SERVICIO','REPARACION','COMPRA_STOCK','PATENTE','OTRO'))
);

CREATE INDEX idx_bf_tmpl_items_template ON budget_forecast_template_items(template_id, row_order);
CREATE INDEX idx_bf_tmpl_items_tenant   ON budget_forecast_template_items(tenant_id);
