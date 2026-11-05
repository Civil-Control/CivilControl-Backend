-- V86: Repair account_movements rows with incorrect zero amounts.
--
-- Two problems introduced after the V83 ledger backfill:
--   1. Movements created for new documents while doc.total was transiently null/zero
--      ended up with amount = 0 despite the document having a real total.
--   2. Some active documents never got a movement row (movement creation was skipped
--      when doc.total was 0 at insertion time but was corrected later).
--
-- Step 1: Update existing zero-amount ORIGINAL movements whose source document now
--         has a positive total.  Sign convention matches LedgerService.signedAmountFor.
-- Step 2: Insert movements for active documents that have no original movement at all.

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 1: Fix zero-amount movements
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE account_movements am
SET    amount = CASE
           WHEN td.document_type IN ('BILL_A','BILL_B','BILL_C',
                                     'DEBIT_NOTE_A','DEBIT_NOTE_B','DEBIT_NOTE_C')
               THEN  td.total
           WHEN td.document_type IN ('CREDIT_NOTE_A','CREDIT_NOTE_B','CREDIT_NOTE_C')
               THEN -td.total
           ELSE 0
       END
FROM   transactional_documents td
WHERE  am.source_document_id = td.id
  AND  am.movement_type     <> 'REVERSAL'
  AND  am.amount             = 0
  AND  td.total              > 0
  AND  td.deleted            = false;

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 2: Insert missing movements for active documents
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO account_movements
    (tenant_id, supplier_id, movement_type, amount, movement_date, source_document_id, created_at)
SELECT
    td.tenant_id,
    td.supplier_id,
    CASE td.document_type
        WHEN 'BILL_A'        THEN 'INVOICE'
        WHEN 'BILL_B'        THEN 'INVOICE'
        WHEN 'BILL_C'        THEN 'INVOICE'
        WHEN 'DEBIT_NOTE_A'  THEN 'DEBIT_NOTE'
        WHEN 'DEBIT_NOTE_B'  THEN 'DEBIT_NOTE'
        WHEN 'DEBIT_NOTE_C'  THEN 'DEBIT_NOTE'
        WHEN 'CREDIT_NOTE_A' THEN 'CREDIT_NOTE'
        WHEN 'CREDIT_NOTE_B' THEN 'CREDIT_NOTE'
        WHEN 'CREDIT_NOTE_C' THEN 'CREDIT_NOTE'
        ELSE 'OTHER'
    END,
    CASE
        WHEN td.document_type IN ('BILL_A','BILL_B','BILL_C',
                                  'DEBIT_NOTE_A','DEBIT_NOTE_B','DEBIT_NOTE_C')   THEN  td.total
        WHEN td.document_type IN ('CREDIT_NOTE_A','CREDIT_NOTE_B','CREDIT_NOTE_C') THEN -td.total
        ELSE 0
    END,
    td.date,
    td.id,
    NOW()
FROM transactional_documents td
WHERE td.deleted = false
  AND NOT EXISTS (
      SELECT 1
      FROM   account_movements am
      WHERE  am.source_document_id = td.id
        AND  am.movement_type     <> 'REVERSAL'
  );
