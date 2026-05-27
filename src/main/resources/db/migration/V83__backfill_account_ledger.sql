-- V83: Backfill account_movements and account_imputations from legacy tables.
--
-- Reads transactional_documents, payment_details, payment_applications, and
-- credit_note_applications and translates all active rows into the new ledger.
-- No existing tables are modified.
--
-- Sign convention:
--   INVOICE / DEBIT_NOTE  → +total  (we assume debt)
--   CREDIT_NOTE           → -total  (reduces debt)
--   PAYMENT               → -amount (we pay)
--   OTHER                 →  0      (informational only)
--
-- Final step is a per-supplier balance audit: if any supplier's
-- SUM(account_movements.amount) diverges from the legacy calculation by more
-- than 0.01, RAISE EXCEPTION aborts the migration and Flyway rolls everything back.

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 1: Document movements (active documents only)
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
WHERE td.deleted = false;

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 2: Payment movements (active payments only)
-- A payment is active if none of its method rows are soft-deleted.
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO account_movements
    (tenant_id, supplier_id, movement_type, amount, movement_date, source_payment_id, created_at)
SELECT
    pd.tenant_id,
    pd.supplier_id,
    'PAYMENT',
    -pd.amount,
    pd.payment_date,
    pd.id,
    NOW()
FROM payment_details pd
WHERE NOT EXISTS (SELECT 1 FROM cash_payments     cp  WHERE cp.id  = pd.id AND cp.deleted  = true)
  AND NOT EXISTS (SELECT 1 FROM transfer_payments tp  WHERE tp.id  = pd.id AND tp.deleted  = true)
  AND NOT EXISTS (SELECT 1 FROM check_payments    chp WHERE chp.id = pd.id AND chp.deleted = true);

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 3: Imputations from payment_applications
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO account_imputations
    (tenant_id, origin_movement_id, destination_movement_id, amount_applied, created_at)
SELECT
    pa.tenant_id,
    pay_mov.id,
    doc_mov.id,
    pa.amount_applied,
    NOW()
FROM payment_applications pa
JOIN account_movements pay_mov ON pay_mov.source_payment_id  = pa.payment_details_id
JOIN account_movements doc_mov ON doc_mov.source_document_id = pa.document_id
WHERE NOT EXISTS (SELECT 1 FROM cash_payments     cp  WHERE cp.id  = pa.payment_details_id AND cp.deleted  = true)
  AND NOT EXISTS (SELECT 1 FROM transfer_payments tp  WHERE tp.id  = pa.payment_details_id AND tp.deleted  = true)
  AND NOT EXISTS (SELECT 1 FROM check_payments    chp WHERE chp.id = pa.payment_details_id AND chp.deleted = true)
ON CONFLICT (origin_movement_id, destination_movement_id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 4: Imputations from credit_note_applications
-- Only include rows where both the credit note and the invoice are active.
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO account_imputations
    (tenant_id, origin_movement_id, destination_movement_id, amount_applied, created_at)
SELECT
    cna.tenant_id,
    cn_mov.id,
    inv_mov.id,
    cna.amount_applied,
    NOW()
FROM credit_note_applications cna
JOIN account_movements cn_mov  ON cn_mov.source_document_id  = cna.credit_note_id
JOIN account_movements inv_mov ON inv_mov.source_document_id = cna.invoice_id
ON CONFLICT (origin_movement_id, destination_movement_id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 5: Per-supplier balance audit
-- Rolls back the entire migration if any supplier diverges by more than $0.01.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    v_mismatch_count INT;
BEGIN
    SELECT COUNT(*)
    INTO v_mismatch_count
    FROM (
        SELECT
            s.id        AS supplier_id,
            s.tenant_id,
            COALESCE(SUM(am.amount), 0)                                         AS ledger_balance,
            COALESCE(doc_leg.doc_balance, 0) - COALESCE(pay_leg.paid_total, 0)  AS legacy_balance
        FROM suppliers s
        LEFT JOIN account_movements am
               ON am.supplier_id = s.id
              AND am.tenant_id   = s.tenant_id
        LEFT JOIN (
            SELECT
                td.supplier_id,
                td.tenant_id,
                SUM(CASE
                    WHEN td.document_type IN ('BILL_A','BILL_B','BILL_C',
                                              'DEBIT_NOTE_A','DEBIT_NOTE_B','DEBIT_NOTE_C')   THEN  td.total
                    WHEN td.document_type IN ('CREDIT_NOTE_A','CREDIT_NOTE_B','CREDIT_NOTE_C') THEN -td.total
                    ELSE 0
                END) AS doc_balance
            FROM transactional_documents td
            WHERE td.deleted = false
            GROUP BY td.supplier_id, td.tenant_id
        ) doc_leg
               ON doc_leg.supplier_id = s.id
              AND doc_leg.tenant_id   = s.tenant_id
        LEFT JOIN (
            SELECT
                pd.supplier_id,
                pd.tenant_id,
                SUM(pd.amount) AS paid_total
            FROM payment_details pd
            WHERE NOT EXISTS (SELECT 1 FROM cash_payments     cp  WHERE cp.id  = pd.id AND cp.deleted  = true)
              AND NOT EXISTS (SELECT 1 FROM transfer_payments tp  WHERE tp.id  = pd.id AND tp.deleted  = true)
              AND NOT EXISTS (SELECT 1 FROM check_payments    chp WHERE chp.id = pd.id AND chp.deleted = true)
            GROUP BY pd.supplier_id, pd.tenant_id
        ) pay_leg
               ON pay_leg.supplier_id = s.id
              AND pay_leg.tenant_id   = s.tenant_id
        GROUP BY s.id, s.tenant_id, doc_leg.doc_balance, pay_leg.paid_total
        HAVING ABS(
            COALESCE(SUM(am.amount), 0) -
            (COALESCE(doc_leg.doc_balance, 0) - COALESCE(pay_leg.paid_total, 0))
        ) > 0.01
    ) mismatches;

    IF v_mismatch_count > 0 THEN
        RAISE EXCEPTION
            'Account ledger backfill audit FAILED: % supplier(s) have a balance mismatch. Migration rolled back.',
            v_mismatch_count;
    END IF;
END $$;
