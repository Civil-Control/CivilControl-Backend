ALTER TABLE notification_logs ADD COLUMN read BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification_logs ADD COLUMN dismissed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_notif_log_inbox
    ON notification_logs (subscription_id, channel, dismissed, sent_at DESC);
