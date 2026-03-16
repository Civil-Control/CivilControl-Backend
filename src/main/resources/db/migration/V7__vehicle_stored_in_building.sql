-- Migrate vehicle stored_in from free text to FK reference to buildings
ALTER TABLE vehicles ADD COLUMN building_id BIGINT;
ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_building FOREIGN KEY (building_id) REFERENCES buildings(id);
ALTER TABLE vehicles DROP COLUMN stored_in;
