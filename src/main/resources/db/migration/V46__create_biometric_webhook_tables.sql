-- =============================================================================
-- V46__create_biometric_webhook_tables.sql
-- Dead-letter queue for biometric webhook payloads that could not be processed
-- =============================================================================

CREATE TABLE IF NOT EXISTS orphan_biometric_logs (
    id              BIGSERIAL       PRIMARY KEY,
    clock_brand     VARCHAR(50)     NOT NULL,
    raw_payload     TEXT            NOT NULL,
    error_message   TEXT,
    received_at     TIMESTAMP       NOT NULL DEFAULT NOW()
);
