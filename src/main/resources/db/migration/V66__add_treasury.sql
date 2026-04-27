-- V66: Treasury feature (cajas, cuentas bancarias y chequeras).
-- Adds three new aggregates plus their movement tables, and migrates existing
-- payment rows from the legacy free-form bank_name column to FKs into bank_accounts.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Bank accounts
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bank_accounts (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    name            VARCHAR(150) NOT NULL,
    bank_name       VARCHAR(150) NOT NULL,
    account_type    VARCHAR(40)  NOT NULL,
    account_number  VARCHAR(60)  NOT NULL,
    cbu             VARCHAR(40),
    alias           VARCHAR(60),
    currency        VARCHAR(10)  NOT NULL DEFAULT 'ARS',
    balance         NUMERIC(19,2) NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_bank_accounts_tenant_acc UNIQUE (tenant_id, account_number, bank_name)
);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_tenant ON bank_accounts(tenant_id);

CREATE TABLE IF NOT EXISTS bank_account_movements (
    id                  BIGSERIAL     PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    bank_account_id     BIGINT        NOT NULL REFERENCES bank_accounts(id),
    type                VARCHAR(40)   NOT NULL,
    amount              NUMERIC(19,2) NOT NULL,
    signed_amount       NUMERIC(19,2) NOT NULL,
    balance_after       NUMERIC(19,2) NOT NULL,
    movement_date       DATE          NOT NULL,
    comment             VARCHAR(500),
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id  BIGINT,
    check_payment_id    BIGINT,
    transfer_payment_id BIGINT
);
CREATE INDEX IF NOT EXISTS idx_bam_account_date ON bank_account_movements(bank_account_id, movement_date DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Cash boxes
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cash_boxes (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    currency    VARCHAR(10)  NOT NULL DEFAULT 'ARS',
    balance     NUMERIC(19,2) NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_cash_boxes_tenant_name UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_cash_boxes_tenant ON cash_boxes(tenant_id);

CREATE TABLE IF NOT EXISTS cash_box_movements (
    id                  BIGSERIAL     PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    cash_box_id         BIGINT        NOT NULL REFERENCES cash_boxes(id),
    type                VARCHAR(40)   NOT NULL,
    amount              NUMERIC(19,2) NOT NULL,
    signed_amount       NUMERIC(19,2) NOT NULL,
    balance_after       NUMERIC(19,2) NOT NULL,
    movement_date       DATE          NOT NULL,
    comment             VARCHAR(500),
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id  BIGINT,
    cash_payment_id     BIGINT
);
CREATE INDEX IF NOT EXISTS idx_cbm_box_date ON cash_box_movements(cash_box_id, movement_date DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Checkbooks
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS checkbooks (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    name                VARCHAR(150) NOT NULL,
    checkbook_number    VARCHAR(60)  NOT NULL,
    bank_account_id     BIGINT       NOT NULL REFERENCES bank_accounts(id),
    check_type          VARCHAR(20)  NOT NULL,
    range_from          BIGINT       NOT NULL,
    range_to            BIGINT       NOT NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_checkbooks_tenant_num_acc UNIQUE (tenant_id, checkbook_number, bank_account_id)
);
CREATE INDEX IF NOT EXISTS idx_checkbooks_account_active ON checkbooks(bank_account_id, active);

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Backfill bank_accounts from existing payments
--    Combines distinct bank names found on check_payments and transfer_payments,
--    and guarantees that every tenant has at least one default account.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
    -- Combine distinct (tenant_id, bank_name) pairs already present.
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'check_payments' AND column_name = 'bank_name') THEN
        INSERT INTO bank_accounts (tenant_id, name, bank_name, account_type, account_number, currency, balance, active, deleted)
        SELECT DISTINCT cp.tenant_id,
                        COALESCE(NULLIF(TRIM(cp.bank_name), ''), 'Banco Provincia'),
                        COALESCE(NULLIF(TRIM(cp.bank_name), ''), 'Banco Provincia'),
                        'CUENTA_CORRIENTE',
                        CONCAT('LEGACY-', cp.tenant_id, '-', COALESCE(NULLIF(TRIM(cp.bank_name), ''), 'Banco Provincia')),
                        'ARS', 0, TRUE, FALSE
        FROM check_payments cp
        WHERE cp.bank_name IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM bank_accounts ba
              WHERE ba.tenant_id = cp.tenant_id
                AND LOWER(ba.bank_name) = LOWER(COALESCE(NULLIF(TRIM(cp.bank_name), ''), 'Banco Provincia'))
          );
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'transfer_payments' AND column_name = 'bank_name') THEN
        INSERT INTO bank_accounts (tenant_id, name, bank_name, account_type, account_number, currency, balance, active, deleted)
        SELECT DISTINCT tp.tenant_id,
                        COALESCE(NULLIF(TRIM(tp.bank_name), ''), 'Banco Provincia'),
                        COALESCE(NULLIF(TRIM(tp.bank_name), ''), 'Banco Provincia'),
                        'CUENTA_CORRIENTE',
                        CONCAT('LEGACY-', tp.tenant_id, '-', COALESCE(NULLIF(TRIM(tp.bank_name), ''), 'Banco Provincia')),
                        'ARS', 0, TRUE, FALSE
        FROM transfer_payments tp
        WHERE tp.bank_name IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM bank_accounts ba
              WHERE ba.tenant_id = tp.tenant_id
                AND LOWER(ba.bank_name) = LOWER(COALESCE(NULLIF(TRIM(tp.bank_name), ''), 'Banco Provincia'))
          );
    END IF;

    -- Guarantee a default account per tenant.
    INSERT INTO bank_accounts (tenant_id, name, bank_name, account_type, account_number, currency, balance, active, deleted)
    SELECT t.id, 'Banco Provincia', 'Banco Provincia', 'CUENTA_CORRIENTE',
           CONCAT('DEFAULT-', t.id), 'ARS', 0, TRUE, FALSE
    FROM tenants t
    WHERE NOT EXISTS (
        SELECT 1 FROM bank_accounts ba WHERE ba.tenant_id = t.id
    );
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. check_payments: bank_account_id + checkbook_id, drop legacy bank_name
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE check_payments ADD COLUMN IF NOT EXISTS bank_account_id BIGINT;
ALTER TABLE check_payments ADD COLUMN IF NOT EXISTS checkbook_id    BIGINT;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'check_payments' AND column_name = 'bank_name') THEN
        UPDATE check_payments cp
        SET bank_account_id = ba.id
        FROM bank_accounts ba
        WHERE ba.tenant_id = cp.tenant_id
          AND LOWER(ba.bank_name) = LOWER(COALESCE(NULLIF(TRIM(cp.bank_name), ''), 'Banco Provincia'))
          AND cp.bank_account_id IS NULL;
    END IF;

    -- Fallback: any check_payments still without an account get the default tenant account.
    UPDATE check_payments cp
    SET bank_account_id = ba.id
    FROM bank_accounts ba
    WHERE ba.tenant_id = cp.tenant_id
      AND ba.bank_name = 'Banco Provincia'
      AND cp.bank_account_id IS NULL;
END $$;

ALTER TABLE check_payments ALTER COLUMN bank_account_id SET NOT NULL;
ALTER TABLE check_payments ADD CONSTRAINT fk_check_payments_bank_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id);
ALTER TABLE check_payments ADD CONSTRAINT fk_check_payments_checkbook    FOREIGN KEY (checkbook_id)    REFERENCES checkbooks(id);
ALTER TABLE check_payments DROP COLUMN IF EXISTS bank_name;

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. transfer_payments: bank_account_id, drop legacy bank_name
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE transfer_payments ADD COLUMN IF NOT EXISTS bank_account_id BIGINT;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'transfer_payments' AND column_name = 'bank_name') THEN
        UPDATE transfer_payments tp
        SET bank_account_id = ba.id
        FROM bank_accounts ba
        WHERE ba.tenant_id = tp.tenant_id
          AND LOWER(ba.bank_name) = LOWER(COALESCE(NULLIF(TRIM(tp.bank_name), ''), 'Banco Provincia'))
          AND tp.bank_account_id IS NULL;
    END IF;

    UPDATE transfer_payments tp
    SET bank_account_id = ba.id
    FROM bank_accounts ba
    WHERE ba.tenant_id = tp.tenant_id
      AND ba.bank_name = 'Banco Provincia'
      AND tp.bank_account_id IS NULL;
END $$;

ALTER TABLE transfer_payments ALTER COLUMN bank_account_id SET NOT NULL;
ALTER TABLE transfer_payments ADD CONSTRAINT fk_transfer_payments_bank_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id);
ALTER TABLE transfer_payments DROP COLUMN IF EXISTS bank_name;

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. cash_payments: cash_box_id (nullable — only required when tenant has active cash boxes)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE cash_payments ADD COLUMN IF NOT EXISTS cash_box_id BIGINT;
ALTER TABLE cash_payments ADD CONSTRAINT fk_cash_payments_cash_box FOREIGN KEY (cash_box_id) REFERENCES cash_boxes(id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. Movement FKs to payments (added after payment columns are stable).
--    The check/transfer/cash payment tables use joined-table inheritance with
--    @MapsId, so the PK column is payment_details_id (not id).
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE bank_account_movements
    ADD CONSTRAINT fk_bam_check_payment    FOREIGN KEY (check_payment_id)    REFERENCES check_payments(payment_details_id),
    ADD CONSTRAINT fk_bam_transfer_payment FOREIGN KEY (transfer_payment_id) REFERENCES transfer_payments(payment_details_id);

ALTER TABLE cash_box_movements
    ADD CONSTRAINT fk_cbm_cash_payment FOREIGN KEY (cash_payment_id) REFERENCES cash_payments(payment_details_id);
