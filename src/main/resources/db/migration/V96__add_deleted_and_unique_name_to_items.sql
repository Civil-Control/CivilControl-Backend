-- =============================================================================
-- V58__add_deleted_and_unique_name_to_items.sql
-- Adds soft-delete support to items and enforces unique item names per tenant
-- =============================================================================

ALTER TABLE items ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE items ADD CONSTRAINT uk_items_tenant_name UNIQUE (tenant_id, name);
