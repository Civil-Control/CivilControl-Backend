-- =====================================================================
-- V68: Add quantity column to repair_items
-- Each repair item now tracks how many units were used. Existing rows are
-- backfilled to 1 (the implicit prior behaviour). Subtotals are computed as
-- amount * quantity at the application layer.
-- =====================================================================

ALTER TABLE repair_items
    ADD COLUMN quantity DECIMAL(12,2) NOT NULL DEFAULT 1;
