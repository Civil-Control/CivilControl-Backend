-- =============================================================================
-- V57__add_description_to_project_area_tasks.sql
-- Adds description column to project_area_tasks table
-- =============================================================================

ALTER TABLE project_area_tasks ADD COLUMN IF NOT EXISTS description VARCHAR(500) NULL;
