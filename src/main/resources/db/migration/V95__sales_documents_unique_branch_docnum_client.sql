-- =============================================================================
-- V95__sales_documents_unique_branch_docnum_client.sql
-- Enforces uniqueness of (tenant_id, branch_code, document_number, client_id)
-- on sales_documents, matching transactional_documents. Safe going forward
-- because SalesDocumentService now checks for an active duplicate before
-- insert and reactivates any soft-deleted match in place instead of ever
-- inserting a second row with the same key.
-- =============================================================================

ALTER TABLE sales_documents
    ADD CONSTRAINT uk_sales_documents_tenant_branch_docnum_client
    UNIQUE (tenant_id, branch_code, document_number, client_id);
