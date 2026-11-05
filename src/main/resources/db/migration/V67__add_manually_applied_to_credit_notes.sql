-- V67: lets a credit note be marked as "applied" without linking any invoice/debit-note.
-- When manually_applied = true, the credit note's status is APPLIED ("Aplicada") and it
-- no longer appears as available credit, even though its supplier balance impact was
-- already subtracted at creation time. Only meaningful for CREDIT_NOTE_* documents.

ALTER TABLE transactional_documents
    ADD COLUMN manually_applied BOOLEAN NOT NULL DEFAULT FALSE;
