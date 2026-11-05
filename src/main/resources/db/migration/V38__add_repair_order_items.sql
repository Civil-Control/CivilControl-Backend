-- Add items list to repair orders (ElementCollection table)
CREATE TABLE repair_order_items (
    repair_order_id BIGINT NOT NULL,
    item            VARCHAR(200) NOT NULL,
    CONSTRAINT fk_repair_order_items_order
        FOREIGN KEY (repair_order_id) REFERENCES repair_orders(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_repair_order_items_order_id ON repair_order_items(repair_order_id);

-- Make description nullable (no longer required)
ALTER TABLE repair_orders ALTER COLUMN description DROP NOT NULL;
