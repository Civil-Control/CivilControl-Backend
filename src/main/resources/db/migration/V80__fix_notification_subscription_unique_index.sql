DROP INDEX idx_unique_subscription;

CREATE UNIQUE INDEX idx_unique_subscription
    ON notification_subscriptions (tenant_id, user_id, subject_type, subject_id)
    NULLS NOT DISTINCT
    WHERE deleted = false;
