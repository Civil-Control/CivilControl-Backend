-- V16: Fix repair_types data to match current RepairType enum constants.
-- The V14 migration copied old enum values verbatim; some were renamed during refactoring.

-- Rename known old values to their current enum name
UPDATE repair_types SET type = 'TANQUE_COMBUSTIBLE'   WHERE type = 'SISTEMA_COMBUSTIBLE';
UPDATE repair_types SET type = 'SUSPENSION_DIRECCION' WHERE type = 'SUSPENSION';
UPDATE repair_types SET type = 'TRANSMISION'          WHERE type = 'CAJA_TRANSMISION';
UPDATE repair_types SET type = 'INSPECCION_VTV'       WHERE type = 'VTV';
UPDATE repair_types SET type = 'HIDRAULICA'           WHERE type = 'SISTEMA_HIDRAULICO';
UPDATE repair_types SET type = 'SISTEMA_ELECTRICO'    WHERE type = 'ELECTRICO';
UPDATE repair_types SET type = 'REPARACION_SINIESTRO' WHERE type = 'SINIESTRO';

-- Catch-all: any remaining value not in the current valid enum set → OTROS
UPDATE repair_types
SET type = 'OTROS'
WHERE type NOT IN (
    'ARRANQUE', 'ALTERNADOR', 'BARRAS_DIRECCION', 'CAMBIO_ACEITE', 'CAMBIO_FILTROS',
    'CERRADURA', 'CHAPA_PINTURA', 'FRENOS', 'GASTOS_HOMOLOGACION', 'HIDRAULICA',
    'INSPECCION_VTV', 'MOTOR', 'NEUMATICOS', 'REPARACION_SINIESTRO', 'SISTEMA_ELECTRICO',
    'TANQUE_COMBUSTIBLE', 'SUSPENSION_DIRECCION', 'TALLER_EXTERNO', 'TRANSMISION', 'OTROS'
);
