-- V65: credit note applications (links credit notes to the invoices/debit notes they offset)
-- Also reverts NCs that were marked paid=true by previous logic so they go back to paid=false
-- (semantically NCs are "Aplicada"/"Crédito disponible" — never "Pagada").

CREATE TABLE credit_note_applications (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT         NOT NULL,
    credit_note_id  BIGINT         NOT NULL REFERENCES transactional_documents(id),
    invoice_id      BIGINT         NOT NULL REFERENCES transactional_documents(id),
    amount_applied  NUMERIC(19, 2) NOT NULL CHECK (amount_applied > 0),
    CONSTRAINT uq_credit_note_applications_pair UNIQUE (credit_note_id, invoice_id)
);

CREATE INDEX idx_credit_note_applications_credit_note ON credit_note_applications(credit_note_id);
CREATE INDEX idx_credit_note_applications_invoice     ON credit_note_applications(invoice_id);
CREATE INDEX idx_credit_note_applications_tenant      ON credit_note_applications(tenant_id);

-- Revert any credit notes that the previous logic auto-marked as paid.
-- Their pendingBalance impact is unchanged: it was already subtracted at creation time.
UPDATE transactional_documents
SET paid = false
WHERE document_type IN ('CREDIT_NOTE_A', 'CREDIT_NOTE_B', 'CREDIT_NOTE_C')
  AND paid = true
  AND deleted = false;
