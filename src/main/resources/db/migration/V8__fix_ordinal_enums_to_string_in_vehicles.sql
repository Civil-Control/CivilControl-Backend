-- Fix enum columns that were stored as ordinals (int2) before commit 816048f.
--
-- Root cause of previous failures:
--   Hibernate 6 generates CHECK constraints for ordinal enums:
--     CHECK (jurisdiction_type BETWEEN 0 AND 1)
--   BETWEEN expands to: jurisdiction_type >= 0 AND jurisdiction_type <= 1
--   When ALTER TABLE changes int2 → varchar, PostgreSQL re-validates constraints
--   and fails with: "operator does not exist: character varying >= integer"
--   This happens REGARDLESS of the USING clause, which is why all prior attempts failed.
--
-- Fix:
--   1. Drop the integer-based CHECK constraints first (in a DO block via pg_constraint)
--   2. Drop any integer DEFAULT values on these columns
--   3. ALTER COLUMN TYPE using a plain ::text cast (no CASE in USING)
--   4. UPDATE to map '0'/'1'/... to the actual enum name strings
-- Fully idempotent: steps 1-2 no-op if nothing to drop; step 4 no-ops if already converted.

-- Step 1: Drop any CHECK constraints that reference jurisdiction_type or truck_equipment.
-- These are the Hibernate-generated BETWEEN/IN constraints with integer literals.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT DISTINCT c.conname
        FROM pg_constraint c
        JOIN pg_class t     ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
        WHERE n.nspname = 'public'
          AND t.relname  = 'vehicles'
          AND c.contype  = 'c'
          AND a.attname  IN ('jurisdiction_type', 'truck_equipment')
    LOOP
        EXECUTE format('ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

-- Step 2: Drop any integer DEFAULT values (prevents type cast failure on the default expr).
ALTER TABLE vehicles ALTER COLUMN jurisdiction_type DROP DEFAULT;
ALTER TABLE vehicles ALTER COLUMN truck_equipment   DROP DEFAULT;

-- Step 3: Change column types. Simple ::text cast avoids any operator resolution issues.
-- For int2 columns: 0::text = '0', 1::text = '1', etc.
-- For already-varchar columns: varchar::text is always valid, this is a no-op conversion.
ALTER TABLE vehicles
    ALTER COLUMN jurisdiction_type TYPE VARCHAR(255)
    USING jurisdiction_type::text;

ALTER TABLE vehicles
    ALTER COLUMN truck_equipment TYPE VARCHAR(255)
    USING truck_equipment::text;

-- Step 4: Convert ordinal strings to enum names.
-- Rows already containing names ('MUNICIPAL', etc.) do not match IN (...) and are skipped.
UPDATE vehicles
SET jurisdiction_type = CASE jurisdiction_type
    WHEN '0' THEN 'MUNICIPAL'
    WHEN '1' THEN 'PROVINCIAL'
    ELSE jurisdiction_type
END
WHERE jurisdiction_type IN ('0', '1');

UPDATE vehicles
SET truck_equipment = CASE truck_equipment
    WHEN '0' THEN 'NADA'
    WHEN '1' THEN 'HIDROELEVADOR'
    WHEN '2' THEN 'HIDROGRUA'
    ELSE truck_equipment
END
WHERE truck_equipment IN ('0', '1', '2');
