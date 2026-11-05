-- V82: Account Ledger — parallel schema for new accounting architecture.
-- Creates account_movements (append-only ledger) and account_imputations (bridge).
-- No existing tables are modified. No data is inserted. Backfill in V83.

CREATE TABLE account_movements (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    supplier_id         BIGINT          NOT NULL  REFERENCES suppliers(id),
    movement_type       VARCHAR(20)     NOT NULL,
    amount              NUMERIC(19, 2)  NOT NULL,
    movement_date       DATE            NOT NULL,
    source_document_id  BIGINT                    REFERENCES transactional_documents(id),
    source_payment_id   BIGINT                    REFERENCES payment_details(id),
    reversal_of_id      BIGINT                    REFERENCES account_movements(id),
    created_at          TIMESTAMP       NOT NULL  DEFAULT NOW(),
    CONSTRAINT chk_account_movements_type CHECK (
        movement_type IN ('INVOICE','DEBIT_NOTE','CREDIT_NOTE','PAYMENT','RETENTION','OTHER','REVERSAL')
    )
);

CREATE INDEX idx_account_movements_supplier     ON account_movements(supplier_id);
CREATE INDEX idx_account_movements_tenant       ON account_movements(tenant_id);
CREATE INDEX idx_account_movements_date         ON account_movements(movement_date);
CREATE INDEX idx_account_movements_source_doc   ON account_movements(source_document_id);
CREATE INDEX idx_account_movements_source_pay   ON account_movements(source_payment_id);

CREATE TABLE account_imputations (
    id                        BIGSERIAL       PRIMARY KEY,
    tenant_id                 BIGINT          NOT NULL,
    origin_movement_id        BIGINT          NOT NULL  REFERENCES account_movements(id),
    destination_movement_id   BIGINT          NOT NULL  REFERENCES account_movements(id),
    amount_applied            NUMERIC(19, 2)  NOT NULL  CHECK (amount_applied > 0),
    created_at                TIMESTAMP       NOT NULL  DEFAULT NOW(),
    CONSTRAINT uk_account_imputations_pair UNIQUE (origin_movement_id, destination_movement_id)
);

CREATE INDEX idx_account_imputations_origin       ON account_imputations(origin_movement_id);
CREATE INDEX idx_account_imputations_destination  ON account_imputations(destination_movement_id);
CREATE INDEX idx_account_imputations_tenant       ON account_imputations(tenant_id);
