-- V18: Full tenants schema migration (idempotent — works on both fresh DBs and DBs that
-- already had a minimal tenants table created by the legacy Hibernate ddl-auto=create).

-- Step 1: Create the table with full schema if it does not exist.
-- On environments where tenants was already created by Hibernate, this is a no-op.
CREATE TABLE IF NOT EXISTS tenants (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    cuit        VARCHAR(13),
    legal_name  VARCHAR(200),
    street      VARCHAR(200),
    number      INT,
    city        VARCHAR(100),
    state       VARCHAR(100),
    country     VARCHAR(100),
    zip_code    VARCHAR(20),
    phone       VARCHAR(30),
    email       VARCHAR(100),
    logo_url    VARCHAR(500),
    founded_date DATE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE
);

-- Step 2: Add any columns that may be missing from a legacy tenants table.
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS cuit         VARCHAR(13);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS legal_name   VARCHAR(200);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS street       VARCHAR(200);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS number       INT;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS city         VARCHAR(100);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS state        VARCHAR(100);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS country      VARCHAR(100);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS zip_code     VARCHAR(20);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS phone        VARCHAR(30);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS email        VARCHAR(100);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS logo_url     VARCHAR(500);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS founded_date DATE;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS deleted      BOOLEAN NOT NULL DEFAULT FALSE;

-- Step 3: Add UNIQUE index on cuit if not already present.
-- A partial index (WHERE cuit IS NOT NULL) avoids constraint violations when cuit is null
-- on legacy rows that predate this migration.
CREATE UNIQUE INDEX IF NOT EXISTS tenants_cuit_key ON tenants(cuit) WHERE cuit IS NOT NULL;

-- Step 4: Seed default tenant (ESEA SA) to ensure company settings work from first boot.
-- ON CONFLICT: idempotent so this migration can run on a DB that already has the tenant.
INSERT INTO tenants (id, name, cuit, legal_name, city, state, country, active, deleted)
VALUES (1, 'ESEA SA', '30-12345678-9', 'ESEA Sociedad Anónima', 'Buenos Aires', 'Buenos Aires', 'Argentina', TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

