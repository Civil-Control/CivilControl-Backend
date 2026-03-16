-- Fix enum columns in vehicles table that were stored as ordinals (int2)
-- because they lacked @Enumerated(EnumType.STRING) before commit 816048f.
--
-- JurisdictionType ordinals: 0=MUNICIPAL, 1=PROVINCIAL
-- TruckEquipment ordinals:   0=NADA, 1=HIDROELEVADOR, 2=HIDROGRUA
--
-- Uses EXECUTE (dynamic DDL) + array indexing to avoid compile-time operator
-- resolution errors that occur when CASE comparisons are planned before the
-- column type is known at DO-block parse time.
-- table_schema = 'public' prevents false matches from other schemas.

DO $$
DECLARE
    col_type text;
BEGIN
    -- Fix jurisdiction_type: convert from int2 ordinal to varchar if needed
    SELECT data_type INTO col_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name   = 'vehicles'
      AND column_name  = 'jurisdiction_type';

    IF col_type IN ('smallint', 'integer', 'bigint') THEN
        EXECUTE '
            ALTER TABLE vehicles
                ALTER COLUMN jurisdiction_type TYPE VARCHAR(255)
                USING (ARRAY[''MUNICIPAL'', ''PROVINCIAL''])[jurisdiction_type::int + 1]
        ';
    END IF;

    -- Fix truck_equipment: convert from int2 ordinal to varchar if needed
    SELECT data_type INTO col_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name   = 'vehicles'
      AND column_name  = 'truck_equipment';

    IF col_type IN ('smallint', 'integer', 'bigint') THEN
        EXECUTE '
            ALTER TABLE vehicles
                ALTER COLUMN truck_equipment TYPE VARCHAR(255)
                USING (ARRAY[''NADA'', ''HIDROELEVADOR'', ''HIDROGRUA''])[truck_equipment::int + 1]
        ';
    END IF;
END $$;
