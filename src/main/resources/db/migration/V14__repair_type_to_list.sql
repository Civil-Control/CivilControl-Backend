-- V14: Change repair_type from a single enum column to a one-to-many collection table.
-- Existing data in the repair_type column is migrated to the new table before the column is dropped.

-- Step 1: Create the new repair_types collection table
CREATE TABLE IF NOT EXISTS repair_types (
    repair_id BIGINT NOT NULL,
    type      VARCHAR(100) NOT NULL,
    CONSTRAINT fk_repair_types_repair FOREIGN KEY (repair_id) REFERENCES repairs(id) ON DELETE CASCADE
);

-- Step 2: Migrate existing single repair_type values into the new table
INSERT INTO repair_types (repair_id, type)
SELECT id, repair_type FROM repairs WHERE repair_type IS NOT NULL;

-- Step 3: Drop the old single-value column
ALTER TABLE repairs DROP COLUMN repair_type;
