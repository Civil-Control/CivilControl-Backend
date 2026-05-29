ALTER TABLE purchase_order_items RENAME COLUMN item TO name;
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS quantity DECIMAL(10,2);
