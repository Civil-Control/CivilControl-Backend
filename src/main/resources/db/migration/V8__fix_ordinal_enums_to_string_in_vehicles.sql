-- Fix enum columns in vehicles table that were stored as ordinals (int2)
-- because they lacked @Enumerated(EnumType.STRING) before commit 816048f.
--
-- JurisdictionType ordinals: 0=MUNICIPAL, 1=PROVINCIAL
-- TruckEquipment ordinals:   0=NADA, 1=HIDROELEVADOR, 2=HIDROGRUA
--
-- Uses ::text casting so CASE comparisons are always text=text, which works
-- regardless of whether the column is currently int2 or already varchar.
-- Idempotent: if values are already names (not '0','1'...) ELSE preserves them.

ALTER TABLE vehicles
    ALTER COLUMN jurisdiction_type TYPE VARCHAR(255)
    USING CASE jurisdiction_type::text
        WHEN '0' THEN 'MUNICIPAL'
        WHEN '1' THEN 'PROVINCIAL'
        ELSE jurisdiction_type::text
    END;

ALTER TABLE vehicles
    ALTER COLUMN truck_equipment TYPE VARCHAR(255)
    USING CASE truck_equipment::text
        WHEN '0' THEN 'NADA'
        WHEN '1' THEN 'HIDROELEVADOR'
        WHEN '2' THEN 'HIDROGRUA'
        ELSE truck_equipment::text
    END;
