-- ============================================================================
-- V37: Convert contact_info from OneToOne to OneToMany for clients/suppliers
--      Add reference_name column to contact_info
--      Add client_id and supplier_id FK columns to contact_info
--      Remove contact_info_id FK from clients and suppliers
-- ============================================================================

-- 1. Add reference_name column (NOT NULL with default for existing rows)
ALTER TABLE contact_info ADD COLUMN reference_name VARCHAR(100);
UPDATE contact_info SET reference_name = 'Principal' WHERE reference_name IS NULL;
ALTER TABLE contact_info ALTER COLUMN reference_name SET NOT NULL;

-- 2. Add client_id and supplier_id FK columns to contact_info
ALTER TABLE contact_info ADD COLUMN client_id BIGINT;
ALTER TABLE contact_info ADD COLUMN supplier_id BIGINT;

-- 3. Migrate existing relationships: set client_id from clients.contact_info_id
UPDATE contact_info ci
SET client_id = c.id
FROM clients c
WHERE c.contact_info_id = ci.id;

-- 4. Migrate existing relationships: set supplier_id from suppliers.contact_info_id
UPDATE contact_info ci
SET supplier_id = s.id
FROM suppliers s
WHERE s.contact_info_id = ci.id;

-- 5. Add foreign key constraints
ALTER TABLE contact_info
    ADD CONSTRAINT fk_contact_info_client
    FOREIGN KEY (client_id) REFERENCES clients(id);

ALTER TABLE contact_info
    ADD CONSTRAINT fk_contact_info_supplier
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

-- 6. Drop old FK column from clients
ALTER TABLE clients DROP COLUMN IF EXISTS contact_info_id;

-- 7. Drop old FK column from suppliers
ALTER TABLE suppliers DROP COLUMN IF EXISTS contact_info_id;
