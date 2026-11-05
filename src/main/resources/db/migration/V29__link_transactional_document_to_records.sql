-- V29__link_transactional_document_to_records.sql
-- Adds optional FK from fuel_loads, repairs, salary_payments, and stocks
-- to transactional_documents (1:1 per record, nullable).

ALTER TABLE fuel_loads
    ADD COLUMN transactional_document_id BIGINT REFERENCES transactional_documents(id);

ALTER TABLE repairs
    ADD COLUMN transactional_document_id BIGINT REFERENCES transactional_documents(id);

ALTER TABLE salary_payments
    ADD COLUMN transactional_document_id BIGINT REFERENCES transactional_documents(id);

ALTER TABLE stocks
    ADD COLUMN transactional_document_id BIGINT REFERENCES transactional_documents(id);
