-- V71: Feature 18 — Value Recovery (Recupero de Valores).
-- Adds the hidden recovery sector flag on project_areas, the supplier config table
-- and the recovery_events audit table. Backfills the flag on tenants that already
-- have a project area named exactly "Recupero" (case-insensitive, trimmed).

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. project_areas: add is_recovery_sector flag
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE project_areas
    ADD COLUMN IF NOT EXISTS is_recovery_sector BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: mark the first non-deleted project area per tenant whose name (case-insensitive,
-- trimmed) is exactly "recupero". Subsequent ones stay as ordinary sectors.
UPDATE project_areas pa
SET is_recovery_sector = TRUE
WHERE pa.id IN (
    SELECT MIN(pa2.id)
    FROM project_areas pa2
    WHERE LOWER(TRIM(pa2.name)) = 'recupero'
      AND pa2.deleted = FALSE
    GROUP BY pa2.tenant_id
);

-- Only one recovery sector per tenant.
CREATE UNIQUE INDEX IF NOT EXISTS uk_project_areas_recovery_per_tenant
    ON project_areas (tenant_id)
    WHERE is_recovery_sector = TRUE;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. recovery_supplier_configs: per-supplier % and target cash box.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS recovery_supplier_configs (
    id                  BIGSERIAL     PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    project_area_id     BIGINT        NOT NULL REFERENCES project_areas(id),
    supplier_id         BIGINT        NOT NULL REFERENCES suppliers(id),
    cash_box_id         BIGINT        NOT NULL REFERENCES cash_boxes(id),
    recovery_percentage NUMERIC(5,2)  NOT NULL,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT uk_recovery_supplier_configs UNIQUE (tenant_id, project_area_id, supplier_id),
    CONSTRAINT chk_recovery_percentage_range CHECK (recovery_percentage >= 0 AND recovery_percentage <= 100)
);
CREATE INDEX IF NOT EXISTS idx_recovery_configs_tenant ON recovery_supplier_configs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_recovery_configs_supplier ON recovery_supplier_configs(supplier_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. recovery_events: immutable audit of recoveries / reverses / adjustments.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS recovery_events (
    id                          BIGSERIAL     PRIMARY KEY,
    tenant_id                   BIGINT        NOT NULL,
    event_type                  VARCHAR(30)   NOT NULL,
    transactional_document_id   BIGINT        NOT NULL REFERENCES transactional_documents(id),
    credit_note_document_id     BIGINT                 REFERENCES transactional_documents(id),
    reverses_event_id           BIGINT                 REFERENCES recovery_events(id),
    supplier_config_id          BIGINT        NOT NULL REFERENCES recovery_supplier_configs(id),
    snapshot_percentage         NUMERIC(5,2)  NOT NULL,
    snapshot_cash_box_id        BIGINT        NOT NULL REFERENCES cash_boxes(id),
    document_net                NUMERIC(19,2) NOT NULL,
    document_iva                NUMERIC(19,2) NOT NULL,
    recovered_amount            NUMERIC(19,2) NOT NULL,
    cash_box_movement_id        BIGINT        NOT NULL REFERENCES cash_box_movements(id),
    occurred_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    triggered_by_user_id        BIGINT        NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_recovery_events_doc ON recovery_events(transactional_document_id);
CREATE INDEX IF NOT EXISTS idx_recovery_events_tenant_date ON recovery_events(tenant_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_recovery_events_supplier_config ON recovery_events(supplier_config_id);
