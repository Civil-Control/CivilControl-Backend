-- fuel_type is no longer a closed enum (see custom_fuel_types / V98): drop the CHECK
-- constraints from V60 that still restricted it to the 7 original built-in values,
-- which silently rejected any custom fuel type's price/load rows.

ALTER TABLE gas_station_prices DROP CONSTRAINT IF EXISTS gas_station_prices_fuel_type_check;
ALTER TABLE fuel_loads DROP CONSTRAINT IF EXISTS fuel_loads_fuel_type_check;
