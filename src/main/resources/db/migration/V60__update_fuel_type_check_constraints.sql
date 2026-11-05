-- Fix CHECK constraints on fuel_type columns to include new FuelType enum values (OIL, DISTILLED_WATER).
-- Hibernate 6.x auto-generated these constraints based on the original enum values,
-- but they were never updated when new values were added to the Java FuelType enum.

ALTER TABLE gas_station_prices DROP CONSTRAINT IF EXISTS gas_station_prices_fuel_type_check;
ALTER TABLE gas_station_prices ADD CONSTRAINT gas_station_prices_fuel_type_check
    CHECK (fuel_type::text = ANY (ARRAY['INFINIA','SUPER','INFINIA_DIESEL','DIESEL_500','GNC','DISTILLED_WATER','OIL']::text[]));

ALTER TABLE fuel_loads DROP CONSTRAINT IF EXISTS fuel_loads_fuel_type_check;
ALTER TABLE fuel_loads ADD CONSTRAINT fuel_loads_fuel_type_check
    CHECK (fuel_type::text = ANY (ARRAY['INFINIA','SUPER','INFINIA_DIESEL','DIESEL_500','GNC','DISTILLED_WATER','OIL']::text[]));
