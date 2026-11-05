-- V69: Check payment lifecycle status (Feature 16, Part A).
-- Adds the persisted operational status of a check plus auditing fields for status changes.
-- The VENCIDO value is derived at read-time and is therefore never written here.

ALTER TABLE check_payments ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE check_payments ADD COLUMN IF NOT EXISTS settled_date DATE;
ALTER TABLE check_payments ADD COLUMN IF NOT EXISTS status_comment VARCHAR(500);
ALTER TABLE check_payments ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP;
ALTER TABLE check_payments ADD COLUMN IF NOT EXISTS status_changed_by BIGINT;

-- Existing checks are backfilled to PENDIENTE through the column DEFAULT.

-- Speed up the issued-payments report when filtering by check status.
CREATE INDEX IF NOT EXISTS idx_check_payments_tenant_status
    ON check_payments(tenant_id, status);
