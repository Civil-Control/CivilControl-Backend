-- =============================================================================
-- V2__add_vehicle_type_fk_to_vehicles.sql
-- Migra la columna vehicle_type (VARCHAR enum) hacia vehicle_type_id (FK).
-- La tabla vehicle_types ya fue creada en V1.
-- Idempotente: seguro de re-ejecutar si una ejecución anterior quedó parcial.
-- =============================================================================

-- 1. Agregar vehicle_type_id en vehicles (solo si la columna no existe aún)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'vehicles' AND column_name = 'vehicle_type_id'
    ) THEN
        ALTER TABLE vehicles ADD COLUMN vehicle_type_id BIGINT;
    END IF;
END $$;

-- 2. Agregar la FK (solo si no existe aún)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_vehicles_vehicle_type'
    ) THEN
        ALTER TABLE vehicles
            ADD CONSTRAINT fk_vehicles_vehicle_type
                FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types (id);
    END IF;
END $$;

-- 3. Eliminar la columna antigua del enum (solo si todavía existe)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'vehicles' AND column_name = 'vehicle_type'
    ) THEN
        ALTER TABLE vehicles DROP COLUMN vehicle_type;
    END IF;
END $$;
