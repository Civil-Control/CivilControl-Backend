ALTER TABLE policy_vehicles
    ADD COLUMN IF NOT EXISTS premio_total  DECIMAL(15, 2),
    ADD COLUMN IF NOT EXISTS premio_mensual DECIMAL(15, 2);
