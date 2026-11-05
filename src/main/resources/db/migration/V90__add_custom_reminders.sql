CREATE TABLE custom_reminders (
    id                    BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    tenant_id             BIGINT       NOT NULL,
    user_id               BIGINT       NOT NULL,
    created_by_user_id    BIGINT       NOT NULL,
    title                 VARCHAR(150) NOT NULL,
    description           VARCHAR(500),
    reminder_date         DATE         NOT NULL,
    recurrence_type       VARCHAR(20)  NOT NULL,
    recurrence_end_date   DATE,
    subscription_id       BIGINT,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_custom_reminder_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_custom_reminder_tenant_user   ON custom_reminders(tenant_id, user_id);
CREATE INDEX idx_custom_reminder_active_deleted ON custom_reminders(tenant_id, active, deleted);
