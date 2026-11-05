SELECT setval(pg_get_serial_sequence('tenants', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM tenants), 1), true);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM credentials
        WHERE deleted = false
        GROUP BY username
        HAVING COUNT(*) > 1
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uq_credentials_active_username
            ON credentials (username) WHERE deleted = false;
    ELSE
        RAISE WARNING 'Duplicate active usernames across tenants found; uq_credentials_active_username not created';
    END IF;
END $$;
