ALTER TABLE users ADD COLUMN whatsapp_number VARCHAR(20);

CREATE TABLE notification_subscriptions (
    id                    BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    tenant_id             BIGINT      NOT NULL,
    user_id               BIGINT      NOT NULL,
    subscribed_by_user_id BIGINT      NOT NULL,
    subject_type          VARCHAR(30) NOT NULL,
    subject_id            BIGINT,
    active                BOOLEAN     NOT NULL DEFAULT TRUE,
    deleted               BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_notif_sub_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE UNIQUE INDEX idx_unique_subscription
    ON notification_subscriptions (tenant_id, user_id, subject_type, subject_id)
    NULLS NOT DISTINCT;

CREATE INDEX idx_notif_sub_tenant_user    ON notification_subscriptions(tenant_id, user_id);
CREATE INDEX idx_notif_sub_tenant_type    ON notification_subscriptions(tenant_id, subject_type);
CREATE INDEX idx_notif_sub_active_deleted ON notification_subscriptions(tenant_id, active, deleted);

CREATE TABLE notification_subscription_channels (
    subscription_id BIGINT      NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    PRIMARY KEY (subscription_id, channel),
    CONSTRAINT fk_nsc_subscription FOREIGN KEY (subscription_id)
        REFERENCES notification_subscriptions(id) ON DELETE CASCADE
);

CREATE TABLE notification_alerts (
    id                BIGINT   NOT NULL GENERATED ALWAYS AS IDENTITY,
    subscription_id   BIGINT   NOT NULL,
    days_before_alert INTEGER  NOT NULL,
    active            BOOLEAN  NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_subscription FOREIGN KEY (subscription_id)
        REFERENCES notification_subscriptions(id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_alert_sub ON notification_alerts(subscription_id);

CREATE TABLE notification_logs (
    id              BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    tenant_id       BIGINT       NOT NULL,
    subscription_id BIGINT       NOT NULL,
    alert_id        BIGINT       NOT NULL,
    subject_type    VARCHAR(30)  NOT NULL,
    subject_id      BIGINT,
    channel         VARCHAR(20)  NOT NULL,
    sent_at         TIMESTAMP    NOT NULL,
    log_date        DATE         NOT NULL,
    due_date        DATE,
    status          VARCHAR(10)  NOT NULL,
    error_message   VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT fk_notif_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_notif_log_dedup ON notification_logs(subscription_id, alert_id, subject_id, log_date);
CREATE INDEX idx_notif_log_user  ON notification_logs(tenant_id, subscription_id, channel, log_date DESC);
