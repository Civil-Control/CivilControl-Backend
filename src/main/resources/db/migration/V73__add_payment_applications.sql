-- V73: Payment applications — N:M with amount per link + on-account credit.
--
-- Replaces the implicit semantics of payment_details_paid_documents (which only said
-- "this payment touches this document" without saying how much) with an explicit
-- payment_applications table that stores the amount imputed to each document.
--
-- Adds payment_details.on_account_amount to track the portion of a payment that is
-- left as supplier credit (saldo a favor del proveedor).
--
-- The legacy payment_details_paid_documents join table is KEPT in place during the
-- back-compat window: the service layer dual-writes to both tables so existing read
-- queries (PaymentRepository.findPaymentIdByDocumentId, etc.) keep working unchanged.
-- A future migration will drop the legacy table once all readers are migrated.
--
-- Backfill strategy (proportional split, capped at document.total):
--   For each existing payment with N linked documents, distribute the payment.amount
--   proportionally to each document.total, never exceeding document.total. Any remainder
--   (rounding diff or genuine overpay) flows into on_account_amount. Independent payments
--   (no linked documents) become 100% on-account.

-- ────────────────────────────────────────────────────────────────────────────
-- 1. Schema
-- ────────────────────────────────────────────────────────────────────────────

CREATE TABLE payment_applications (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    payment_details_id  BIGINT          NOT NULL REFERENCES payment_details(id),
    document_id         BIGINT          NOT NULL REFERENCES transactional_documents(id),
    amount_applied      NUMERIC(19, 2)  NOT NULL CHECK (amount_applied > 0),
    applied_at          TIMESTAMP       NOT NULL,
    CONSTRAINT uk_payment_applications_pair UNIQUE (payment_details_id, document_id)
);

CREATE INDEX idx_payment_applications_payment  ON payment_applications(payment_details_id);
CREATE INDEX idx_payment_applications_document ON payment_applications(document_id);
CREATE INDEX idx_payment_applications_tenant   ON payment_applications(tenant_id);

ALTER TABLE payment_details
    ADD COLUMN on_account_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

-- ────────────────────────────────────────────────────────────────────────────
-- 2. Backfill from legacy join table (proportional, capped at document.total)
-- ────────────────────────────────────────────────────────────────────────────

WITH payment_doc_sums AS (
    SELECT j.payment_details_id,
           SUM(td.total) AS docs_total_sum
    FROM payment_details_paid_documents j
    JOIN transactional_documents td ON td.id = j.transactional_document_id
    GROUP BY j.payment_details_id
)
INSERT INTO payment_applications (tenant_id, payment_details_id, document_id, amount_applied, applied_at)
SELECT pd.tenant_id,
       pd.id,
       td.id,
       LEAST(
           td.total,
           ROUND(pd.amount * td.total / NULLIF(pds.docs_total_sum, 0), 2)
       ) AS amount_applied,
       (pd.payment_date::timestamp) AS applied_at
FROM payment_details pd
JOIN payment_details_paid_documents j ON j.payment_details_id = pd.id
JOIN transactional_documents td       ON td.id = j.transactional_document_id
JOIN payment_doc_sums pds             ON pds.payment_details_id = pd.id
WHERE COALESCE(td.deleted, FALSE) = FALSE
  AND LEAST(td.total, ROUND(pd.amount * td.total / NULLIF(pds.docs_total_sum, 0), 2)) > 0
ON CONFLICT (payment_details_id, document_id) DO NOTHING;

-- Any leftover from rounding or genuine overpay goes to on_account.
UPDATE payment_details pd
SET on_account_amount = GREATEST(
    pd.amount - COALESCE(
        (SELECT SUM(pa.amount_applied) FROM payment_applications pa WHERE pa.payment_details_id = pd.id),
        0
    ),
    0
);
