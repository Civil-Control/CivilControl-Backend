-- =============================================================================
-- V94__sales_documents_totals_not_null.sql
-- Backfills any legacy NULL totals on sales_documents (net_total, iva_total,
-- iva_exempt_total, other_taxes, total, discount_percentage were never
-- required and other_taxes/iva_exempt_total were never even sent by the
-- frontend before this fix) and enforces NOT NULL going forward, matching
-- transactional_documents.
-- =============================================================================

UPDATE sales_documents SET net_total = 0 WHERE net_total IS NULL;
UPDATE sales_documents SET iva_total = 0 WHERE iva_total IS NULL;
UPDATE sales_documents SET iva_exempt_total = 0 WHERE iva_exempt_total IS NULL;
UPDATE sales_documents SET other_taxes = 0 WHERE other_taxes IS NULL;
UPDATE sales_documents SET total = 0 WHERE total IS NULL;
UPDATE sales_documents SET discount_percentage = 0 WHERE discount_percentage IS NULL;

ALTER TABLE sales_documents ALTER COLUMN net_total SET NOT NULL;
ALTER TABLE sales_documents ALTER COLUMN iva_total SET NOT NULL;
ALTER TABLE sales_documents ALTER COLUMN iva_exempt_total SET NOT NULL;
ALTER TABLE sales_documents ALTER COLUMN other_taxes SET NOT NULL;
ALTER TABLE sales_documents ALTER COLUMN total SET NOT NULL;
ALTER TABLE sales_documents ALTER COLUMN discount_percentage SET NOT NULL;
