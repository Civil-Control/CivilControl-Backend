-- V101: client-side credit-note applications + client account ledger.
-- Mirrors V65 (credit_note_applications) + V82 (account_movements/account_imputations) +
-- V67 (manually_applied), scoped to sales_documents/clients. No PAYMENT/RETENTION/on-account
-- movement types exist here because there is no client-side Payment entity yet.

CREATE TABLE sales_credit_note_applications (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT         NOT NULL,
    credit_note_id  BIGINT         NOT NULL REFERENCES sales_documents(id),
    invoice_id      BIGINT         NOT NULL REFERENCES sales_documents(id),
    amount_applied  NUMERIC(19, 2) NOT NULL CHECK (amount_applied > 0),
    CONSTRAINT uq_sales_credit_note_applications_pair UNIQUE (credit_note_id, invoice_id)
);

CREATE INDEX idx_sales_credit_note_applications_credit_note ON sales_credit_note_applications(credit_note_id);
CREATE INDEX idx_sales_credit_note_applications_invoice     ON sales_credit_note_applications(invoice_id);
CREATE INDEX idx_sales_credit_note_applications_tenant      ON sales_credit_note_applications(tenant_id);

CREATE TABLE client_account_movements (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    client_id           BIGINT          NOT NULL  REFERENCES clients(id),
    movement_type       VARCHAR(20)     NOT NULL,
    amount              NUMERIC(19, 2)  NOT NULL,
    movement_date       DATE            NOT NULL,
    source_document_id  BIGINT                    REFERENCES sales_documents(id),
    reversal_of_id      BIGINT                    REFERENCES client_account_movements(id),
    created_at          TIMESTAMP       NOT NULL  DEFAULT NOW(),
    CONSTRAINT chk_client_account_movements_type CHECK (
        movement_type IN ('INVOICE','DEBIT_NOTE','CREDIT_NOTE','REVERSAL')
    )
);

CREATE INDEX idx_client_account_movements_client      ON client_account_movements(client_id);
CREATE INDEX idx_client_account_movements_tenant      ON client_account_movements(tenant_id);
CREATE INDEX idx_client_account_movements_date        ON client_account_movements(movement_date);
CREATE INDEX idx_client_account_movements_source_doc  ON client_account_movements(source_document_id);

CREATE TABLE client_account_imputations (
    id                        BIGSERIAL       PRIMARY KEY,
    tenant_id                 BIGINT          NOT NULL,
    origin_movement_id        BIGINT          NOT NULL  REFERENCES client_account_movements(id),
    destination_movement_id   BIGINT          NOT NULL  REFERENCES client_account_movements(id),
    amount_applied            NUMERIC(19, 2)  NOT NULL  CHECK (amount_applied > 0),
    created_at                TIMESTAMP       NOT NULL  DEFAULT NOW(),
    CONSTRAINT uk_client_account_imputations_pair UNIQUE (origin_movement_id, destination_movement_id)
);

CREATE INDEX idx_client_account_imputations_origin       ON client_account_imputations(origin_movement_id);
CREATE INDEX idx_client_account_imputations_destination  ON client_account_imputations(destination_movement_id);
CREATE INDEX idx_client_account_imputations_tenant       ON client_account_imputations(tenant_id);

ALTER TABLE sales_documents
    ADD COLUMN manually_applied BOOLEAN NOT NULL DEFAULT FALSE;
