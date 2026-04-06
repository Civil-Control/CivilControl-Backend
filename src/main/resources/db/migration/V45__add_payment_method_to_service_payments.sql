-- Add payment_method column to service_payments table.
-- Nullable so existing records remain valid (no default needed).
ALTER TABLE service_payments ADD COLUMN payment_method VARCHAR(50) NULL;
