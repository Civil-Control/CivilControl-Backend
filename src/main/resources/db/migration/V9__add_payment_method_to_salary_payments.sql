-- Add payment_method column to salary_payments table.
-- Nullable so existing records remain valid (no default needed).
ALTER TABLE salary_payments ADD COLUMN payment_method VARCHAR(50) NULL;
