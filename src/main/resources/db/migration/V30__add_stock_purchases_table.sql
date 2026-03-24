-- V30__add_stock_purchases_table.sql
-- Creates the stock_purchases table to track stock item purchases.
-- When a purchase is registered, the corresponding stock quantity is increased automatically (via the service layer).

CREATE TABLE stock_purchases (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    date DATE NOT NULL,
    stock_id BIGINT NOT NULL REFERENCES stocks(id),
    quantity NUMERIC(10, 2) NOT NULL,
    unit_price NUMERIC(10, 2),
    total_amount NUMERIC(12, 2),
    notes VARCHAR(500),
    transactional_document_id BIGINT REFERENCES transactional_documents(id)
);

CREATE INDEX idx_stock_purchases_tenant ON stock_purchases(tenant_id);
CREATE INDEX idx_stock_purchases_stock ON stock_purchases(stock_id);
CREATE INDEX idx_stock_purchases_date ON stock_purchases(date);
