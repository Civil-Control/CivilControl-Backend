ALTER TABLE suppliers ADD COLUMN iva_condition VARCHAR(30);
ALTER TABLE tenants ADD COLUMN iva_condition VARCHAR(30);

UPDATE suppliers SET iva_condition = 'RESPONSABLE_INSCRIPTO' WHERE iva_condition IS NULL;
UPDATE tenants SET iva_condition = 'RESPONSABLE_INSCRIPTO' WHERE iva_condition IS NULL;
