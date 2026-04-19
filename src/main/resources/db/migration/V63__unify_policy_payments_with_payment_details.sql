-- V63: Unify insurance policy payments with the PaymentDetails system
-- 1. Add supplier (insurance company) FK to insurance_policies
-- 2. Create join table insurance_policy_payment_details
-- 3. Drop legacy policy_payments table

-- Step 1: Add supplier_id to insurance_policies
ALTER TABLE insurance_policies
    ADD COLUMN supplier_id BIGINT,
    ADD CONSTRAINT fk_insurance_policies_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

-- Step 2: Create the join table linking PaymentDetails to InsurancePolicy
CREATE TABLE insurance_policy_payment_details (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    insurance_policy_id BIGINT      NOT NULL,
    payment_details_id  BIGINT      NOT NULL,
    period_from     DATE,
    period_to       DATE,
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_ippd_insurance_policy
        FOREIGN KEY (insurance_policy_id) REFERENCES insurance_policies(id),
    CONSTRAINT fk_ippd_payment_details
        FOREIGN KEY (payment_details_id) REFERENCES payment_details(id)
);

CREATE INDEX idx_ippd_insurance_policy ON insurance_policy_payment_details(insurance_policy_id);
CREATE INDEX idx_ippd_payment_details  ON insurance_policy_payment_details(payment_details_id);
CREATE INDEX idx_ippd_tenant           ON insurance_policy_payment_details(tenant_id);

-- Step 3: Drop legacy table
DROP TABLE IF EXISTS policy_payments;
