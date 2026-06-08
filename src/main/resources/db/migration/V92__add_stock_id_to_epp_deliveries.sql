-- Add optional stock link to epp_deliveries.
-- Nullable so quick, unlinked deliveries remain valid.
ALTER TABLE epp_deliveries ADD COLUMN stock_id BIGINT NULL REFERENCES stocks(id);

CREATE INDEX idx_epp_deliveries_stock ON epp_deliveries(stock_id);
