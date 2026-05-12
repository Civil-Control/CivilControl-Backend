-- =============================================================================
-- V58__create_labor_incidents.sql
-- Feature 20 — Labor Incidents
-- =============================================================================

CREATE TABLE labor_incidents (
    id               BIGSERIAL      NOT NULL,
    tenant_id        BIGINT         NOT NULL,
    incident_type    VARCHAR(50)    NOT NULL,
    incident_date    DATE           NOT NULL,
    description      VARCHAR(1000)  NOT NULL,
    financial_impact DECIMAL(15, 2),
    affected_asset   VARCHAR(500),
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDIENTE',
    resolved_date    DATE,
    notes            VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_labor_incidents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_labor_incidents_tenant_date ON labor_incidents(tenant_id, incident_date);
CREATE INDEX idx_labor_incidents_status      ON labor_incidents(tenant_id, status);

CREATE TABLE labor_incident_employees (
    labor_incident_id BIGINT NOT NULL,
    employee_id       BIGINT NOT NULL,
    PRIMARY KEY (labor_incident_id, employee_id),
    CONSTRAINT fk_lie_incident FOREIGN KEY (labor_incident_id) REFERENCES labor_incidents(id) ON DELETE CASCADE,
    CONSTRAINT fk_lie_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

ALTER TABLE disciplinary_actions
    ADD COLUMN labor_incident_id BIGINT,
    ADD CONSTRAINT fk_da_labor_incident
        FOREIGN KEY (labor_incident_id) REFERENCES labor_incidents(id) ON DELETE SET NULL;
