-- V28: Fix REPAIR_ORDER_* spanish_translation to consistent "Module - Action" format
-- Previous V24 used descriptive labels ("Crear Órdenes de Reparación") instead of
-- the standard format used by all other permissions ("Órdenes de Reparación - Creación")

UPDATE permissions SET spanish_translation = 'Órdenes de Reparación - Creación'
WHERE name = 'REPAIR_ORDER_CREATE';

UPDATE permissions SET spanish_translation = 'Órdenes de Reparación - Lectura'
WHERE name = 'REPAIR_ORDER_READ';

UPDATE permissions SET spanish_translation = 'Órdenes de Reparación - Escritura'
WHERE name = 'REPAIR_ORDER_WRITE';
