-- V90: Realign account_movements originals with the current document total.
--
-- Root cause: linked-record changes (Repair/FuelLoad/SalaryPayment/StockPurchase)
-- recalculated doc.total without syncing the ledger movement, leaving stale amounts
-- (zero or otherwise) on ORIGINAL document movements. V86 only fixed amount = 0;
-- this realigns ALL diverging originals. Sign convention matches LedgerService.signedAmountFor.

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
  AND  am.reversal_of_id IS NULL
  AND  am.movement_type     <> 'REVERSAL'
  AND  td.deleted            = false
  AND  am.amount <> CASE
           WHEN td.document_type IN ('BILL_A','BILL_B','BILL_C',
                                     'DEBIT_NOTE_A','DEBIT_NOTE_B','DEBIT_NOTE_C')
               THEN  td.total
           WHEN td.document_type IN ('CREDIT_NOTE_A','CREDIT_NOTE_B','CREDIT_NOTE_C')
               THEN -td.total
           ELSE 0
       END;
