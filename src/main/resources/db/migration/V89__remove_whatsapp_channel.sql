DELETE FROM notification_subscription_channels WHERE channel = 'WHATSAPP';
DELETE FROM user_verification_tokens WHERE channel = 'WHATSAPP';

ALTER TABLE users DROP COLUMN IF EXISTS whatsapp_number;
ALTER TABLE users DROP COLUMN IF EXISTS whatsapp_verified;
