-- Add periodic due day to insurance_policies (day of month for recurring payments)
ALTER TABLE insurance_policies ADD COLUMN periodic_due_day INTEGER;

-- Create policy_payments table
CREATE TABLE policy_payments (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    insurance_policy_id BIGINT      NOT NULL REFERENCES insurance_policies(id),
    payment_date    DATE            NOT NULL,
    amount          DECIMAL(15, 2)  NOT NULL,
    period_from     DATE            NOT NULL,
    period_to       DATE            NOT NULL,
    notes           VARCHAR(500),
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_policy_payment_policy FOREIGN KEY (insurance_policy_id) REFERENCES insurance_policies(id)
);

CREATE INDEX idx_policy_payments_policy_id ON policy_payments(insurance_policy_id);
CREATE INDEX idx_policy_payments_tenant_id ON policy_payments(tenant_id);
CREATE INDEX idx_policy_payments_period ON policy_payments(period_from, period_to);
