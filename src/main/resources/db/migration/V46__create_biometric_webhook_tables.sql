-- =============================================================================
-- V46__create_biometric_webhook_tables.sql
-- Biometric webhook integration (clock-in/clock-out from hardware devices)
-- =============================================================================

-- Successful biometric logs (tenant-scoped)
CREATE TABLE IF NOT EXISTS biometric_logs (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    employee_dni    VARCHAR(20)     NOT NULL,
    timestamp       TIMESTAMP       NOT NULL,
    clock_brand     VARCHAR(50)     NOT NULL,
    raw_payload     TEXT            NOT NULL,
    received_at     TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_biometric_log UNIQUE (tenant_id, employee_dni, timestamp)
);

-- Dead-letter queue for malformed or unparseable payloads (no tenant — raw storage)
CREATE TABLE IF NOT EXISTS orphan_biometric_logs (
    id              BIGSERIAL       PRIMARY KEY,
    clock_brand     VARCHAR(50)     NOT NULL,
    raw_payload     TEXT            NOT NULL,
    error_message   TEXT,
    received_at     TIMESTAMP       NOT NULL DEFAULT NOW()
);
