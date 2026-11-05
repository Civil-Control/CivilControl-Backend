-- Add webhook_token column to tenants table for secure webhook URL identification
ALTER TABLE tenants ADD COLUMN webhook_token VARCHAR(36);

-- Populate existing tenants with unique tokens
UPDATE tenants SET webhook_token = gen_random_uuid()::VARCHAR WHERE webhook_token IS NULL;

-- Now enforce NOT NULL and UNIQUE constraints
ALTER TABLE tenants ALTER COLUMN webhook_token SET NOT NULL;
ALTER TABLE tenants ADD CONSTRAINT uq_tenants_webhook_token UNIQUE (webhook_token);
