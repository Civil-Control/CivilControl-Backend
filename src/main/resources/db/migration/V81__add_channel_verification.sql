ALTER TABLE users
    ADD COLUMN email_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN whatsapp_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE user_verification_tokens (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    channel    VARCHAR(20) NOT NULL,
    code       VARCHAR(72) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_uvt_user_channel
    ON user_verification_tokens (user_id, channel);
