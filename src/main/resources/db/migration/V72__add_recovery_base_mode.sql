-- V72: Feature 18 — Value Recovery — selectable calculation base (NET / TOTAL).
-- Adds the recovery base to supplier configs (default NET, preserving the legacy formula
-- (net * %) + iva) and snapshots the base used on every event so historic recoveries are
-- never re-interpreted when the config changes.

ALTER TABLE recovery_supplier_configs
    ADD COLUMN IF NOT EXISTS recovery_base VARCHAR(10) NOT NULL DEFAULT 'NET';

ALTER TABLE recovery_supplier_configs
    DROP CONSTRAINT IF EXISTS chk_recovery_base;
ALTER TABLE recovery_supplier_configs
    ADD CONSTRAINT chk_recovery_base CHECK (recovery_base IN ('NET', 'TOTAL'));

ALTER TABLE recovery_events
    ADD COLUMN IF NOT EXISTS snapshot_base VARCHAR(10) NOT NULL DEFAULT 'NET';

ALTER TABLE recovery_events
    DROP CONSTRAINT IF EXISTS chk_recovery_event_snapshot_base;
ALTER TABLE recovery_events
    ADD CONSTRAINT chk_recovery_event_snapshot_base CHECK (snapshot_base IN ('NET', 'TOTAL'));
