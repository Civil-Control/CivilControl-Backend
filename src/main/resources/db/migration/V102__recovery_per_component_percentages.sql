-- V102: Feature 18 — Value Recovery — per-component recovery percentages.
-- Replaces the single recovery_percentage + recovery_base (NET/TOTAL) enum with four
-- independent percentages (net/iva/iibb/other_taxes), each applied to its own amount on the
-- document. IIBB and "other taxes" are read from TransactionalDocument.iibbPerception /
-- otherTaxes (manually entered by the user, never calculated).
--
-- recovery_supplier_configs is mutable config state: old columns are backfilled into the new
-- ones (lossless) and then dropped.
-- recovery_events is an immutable audit log: old columns are NEVER touched or dropped, only
-- backfilled for readability under the new model and relaxed to nullable so new events can
-- leave them null.

ALTER TABLE recovery_supplier_configs
    ADD COLUMN net_percentage         NUMERIC(5,2) NOT NULL DEFAULT 0,
    ADD COLUMN iva_percentage         NUMERIC(5,2) NOT NULL DEFAULT 0,
    ADD COLUMN iibb_percentage        NUMERIC(5,2) NOT NULL DEFAULT 0,
    ADD COLUMN other_taxes_percentage NUMERIC(5,2) NOT NULL DEFAULT 0;

UPDATE recovery_supplier_configs SET
    net_percentage = recovery_percentage,
    iva_percentage = CASE WHEN recovery_base = 'TOTAL' THEN recovery_percentage ELSE 100 END;

ALTER TABLE recovery_supplier_configs
    DROP CONSTRAINT IF EXISTS chk_recovery_percentage_range,
    DROP CONSTRAINT IF EXISTS chk_recovery_base,
    DROP COLUMN recovery_percentage,
    DROP COLUMN recovery_base,
    ADD CONSTRAINT chk_recovery_net_pct_range   CHECK (net_percentage BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_recovery_iva_pct_range   CHECK (iva_percentage BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_recovery_iibb_pct_range  CHECK (iibb_percentage BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_recovery_other_pct_range CHECK (other_taxes_percentage BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_recovery_pct_not_all_zero CHECK (
        net_percentage > 0 OR iva_percentage > 0 OR iibb_percentage > 0 OR other_taxes_percentage > 0
    );

ALTER TABLE recovery_events
    ADD COLUMN snapshot_net_percentage         NUMERIC(5,2)  NOT NULL DEFAULT 0,
    ADD COLUMN snapshot_iva_percentage         NUMERIC(5,2)  NOT NULL DEFAULT 0,
    ADD COLUMN snapshot_iibb_percentage        NUMERIC(5,2)  NOT NULL DEFAULT 0,
    ADD COLUMN snapshot_other_taxes_percentage NUMERIC(5,2)  NOT NULL DEFAULT 0,
    ADD COLUMN document_iibb                   NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN document_other_taxes            NUMERIC(19,2) NOT NULL DEFAULT 0;

-- Backfill: historic events never considered IIBB/other taxes, so 0 is the true historical
-- value for those two columns, not an arbitrary placeholder.
UPDATE recovery_events SET
    snapshot_net_percentage = snapshot_percentage,
    snapshot_iva_percentage = CASE WHEN snapshot_base = 'TOTAL' THEN snapshot_percentage ELSE 100 END;

ALTER TABLE recovery_events
    ALTER COLUMN snapshot_percentage DROP NOT NULL,
    ALTER COLUMN snapshot_base DROP NOT NULL;
