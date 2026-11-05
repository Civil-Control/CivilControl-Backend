ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS km_date DATE;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS km_source VARCHAR(20);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS manual_km INTEGER;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS manual_km_date DATE;

UPDATE vehicles v
SET km = latest.km,
    km_date = latest.record_date,
    km_source = latest.source
FROM (
    SELECT DISTINCT ON (vehicle_id) vehicle_id, km, record_date, source
    FROM (
        SELECT ca.vehicle_id, ca.km AS km, ca.date AS record_date, 'PARTE_DIARIO' AS source
        FROM crew_assignments ca
        WHERE ca.deleted = false AND ca.km IS NOT NULL
        UNION ALL
        SELECT r.vehicle_id, r.mileage AS km, r.date AS record_date, 'REPARACION' AS source
        FROM repairs r
        WHERE r.mileage IS NOT NULL
    ) records
    ORDER BY vehicle_id, record_date DESC, km DESC
) latest
WHERE latest.vehicle_id = v.id;
