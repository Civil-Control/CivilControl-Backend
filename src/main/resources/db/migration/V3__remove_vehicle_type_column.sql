-- =============================================================================
-- V3__remove_vehicle_type_column.sql
-- Eliminacion definitiva de la columna vehicle_type (VARCHAR NOT NULL) en vehicles.
-- Esta columna era el tipo de vehiculo almacenado como enum en texto plano.
-- Fue reemplazada por vehicle_type_id (FK a vehicle_types) en V2, pero la columna
-- original puede seguir existiendo en bases de datos de produccion donde V2 no
-- pudo ejecutar el DROP por errores de estado previo.
-- Idempotente: seguro de re-ejecutar sin importar el estado actual.
-- =============================================================================

-- Eliminar la columna vehicle_type si todavia existe
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'vehicles' AND column_name = 'vehicle_type'
    ) THEN
        ALTER TABLE vehicles DROP COLUMN vehicle_type;
    END IF;
END $$;

