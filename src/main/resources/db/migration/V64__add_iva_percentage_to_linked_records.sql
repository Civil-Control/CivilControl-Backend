-- =====================================================================
-- V64 — IVA per linked record
-- =====================================================================
-- Adds an `iva_percentage` column to every entity that can be linked to a
-- TransactionalDocument as a "line item" (RepairItem, FuelLoad, SalaryPayment,
-- StockPurchase). Until now the document-totals recalculator hard-coded 21%
-- for these records, which produced wrong subtotals when a purchase invoice
-- mixed VAT rates (0%, 10.5%, 21%, 27%).
--
-- All existing rows are seeded with 21 (the previous implicit value) so totals
-- remain unchanged after the migration.
-- =====================================================================

ALTER TABLE repair_items
    ADD COLUMN iva_percentage NUMERIC(5,2) NOT NULL DEFAULT 21.00;

ALTER TABLE fuel_loads
    ADD COLUMN iva_percentage NUMERIC(5,2) NOT NULL DEFAULT 21.00;

ALTER TABLE salary_payments
    ADD COLUMN iva_percentage NUMERIC(5,2) NOT NULL DEFAULT 21.00;

ALTER TABLE stock_purchases
    ADD COLUMN iva_percentage NUMERIC(5,2) NOT NULL DEFAULT 21.00;
