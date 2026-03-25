-- Add sort order column to item_details and linked record tables
-- to persist the display order of rows within a transactional document.

ALTER TABLE item_details      ADD COLUMN document_sort_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE repairs            ADD COLUMN document_sort_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fuel_loads         ADD COLUMN document_sort_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE salary_payments    ADD COLUMN document_sort_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE stock_purchases    ADD COLUMN document_sort_order INTEGER NOT NULL DEFAULT 0;
