-- Fix enum columns in vehicles table that were stored as ordinals (int2)
-- because they lacked @Enumerated(EnumType.STRING) before commit 816048f.
--
-- JurisdictionType ordinals: 0=MUNICIPAL, 1=PROVINCIAL
-- TruckEquipment ordinals:   0=NADA, 1=HIDROELEVADOR, 2=HIDROGRUA
--
-- Uses USING CASE for safe int2 -> varchar conversion in PostgreSQL.

DO $$
BEGIN
    -- Fix jurisdiction_type only if it is still stored as a numeric type
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'vehicles'
          AND column_name = 'jurisdiction_type'
          AND data_type IN ('smallint', 'integer', 'bigint', 'int2', 'int4', 'int8')
    ) THEN
        ALTER TABLE vehicles
            ALTER COLUMN jurisdiction_type TYPE VARCHAR(255)
            USING CASE jurisdiction_type::int
                WHEN 0 THEN 'MUNICIPAL'
                WHEN 1 THEN 'PROVINCIAL'
                ELSE NULL
            END;
    END IF;

    -- Fix truck_equipment only if it is still stored as a numeric type
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'vehicles'
          AND column_name = 'truck_equipment'
          AND data_type IN ('smallint', 'integer', 'bigint', 'int2', 'int4', 'int8')
    ) THEN
        ALTER TABLE vehicles
            ALTER COLUMN truck_equipment TYPE VARCHAR(255)
            USING CASE truck_equipment::int
                WHEN 0 THEN 'NADA'
                WHEN 1 THEN 'HIDROELEVADOR'
                WHEN 2 THEN 'HIDROGRUA'
                ELSE NULL
            END;
    END IF;
END $$;
