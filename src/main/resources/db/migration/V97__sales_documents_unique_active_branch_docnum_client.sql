-- =============================================================================
-- V97__sales_documents_unique_active_branch_docnum_client.sql
-- Enforces uniqueness of (tenant_id, branch_code, document_number, client_id)
-- among ACTIVE (non-deleted) sales_documents only, matching what
-- SalesDocumentService.createSalesDocument actually needs (it already checks
-- for an active duplicate before insert and reactivates any soft-deleted
-- match in place). A blanket constraint (like transactional_documents has)
-- cannot be created on this table: its history has several soft-deleted rows
-- left over from before that check existed. Scoping the index to
-- deleted = false ignores that historical junk while still preventing new
-- active duplicates going forward.
-- =============================================================================

CREATE UNIQUE INDEX uk_sales_documents_active_tenant_branch_docnum_client
    ON sales_documents (tenant_id, branch_code, document_number, client_id)
    WHERE deleted = false;
