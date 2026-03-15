-- Add user location columns for address autofill feature.
-- All columns are nullable so existing rows are unaffected.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS location_city      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS location_state     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS location_country   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS location_zip_code  VARCHAR(20);
