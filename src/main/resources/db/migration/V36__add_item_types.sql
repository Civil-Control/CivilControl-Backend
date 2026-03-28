-- ============================================
-- V36: Add item_types table for Item usage classification
-- ============================================
-- Creates a join table to support multiple ItemType values per Item
-- (COMPRA, VENTA, or both). Assigns COMPRA to all existing items
-- since historically all items in the system were purchase items.

CREATE TABLE item_types (
    item_id     BIGINT       NOT NULL,
    item_type   VARCHAR(10)  NOT NULL,
    CONSTRAINT pk_item_types        PRIMARY KEY (item_id, item_type),
    CONSTRAINT fk_item_types_item   FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    CONSTRAINT chk_item_type_value  CHECK (item_type IN ('COMPRA', 'VENTA'))
);

-- Assign COMPRA to every existing item
INSERT INTO item_types (item_id, item_type)
SELECT id, 'COMPRA' FROM items;
