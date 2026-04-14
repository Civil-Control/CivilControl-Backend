-- =============================================================================
-- V56__create_project_area_tasks.sql
-- Creates the project_area_tasks table for sub-tasks within project areas
-- and adds project_area_task_id FK column to all entities that reference project_areas
-- =============================================================================

CREATE TABLE IF NOT EXISTS project_area_tasks (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    project_area_id     BIGINT          NOT NULL,
    name                VARCHAR(150)    NOT NULL,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_project_area_task_area FOREIGN KEY (project_area_id) REFERENCES project_areas(id),
    CONSTRAINT uk_project_area_task UNIQUE (tenant_id, project_area_id, name)
);

-- Add project_area_task_id to all entities that have project_area_id
ALTER TABLE buildings ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE buildings ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);

ALTER TABLE work_contracts ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE work_contracts ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);

ALTER TABLE fuel_loads ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE fuel_loads ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);

ALTER TABLE sales_documents ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE sales_documents ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);

ALTER TABLE service_payments ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE service_payments ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);

ALTER TABLE transactional_documents ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE transactional_documents ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);

ALTER TABLE salary_payments ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE salary_payments ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);

ALTER TABLE crew_assignments ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE crew_assignments ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);

ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE vehicles ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);

ALTER TABLE employees ADD COLUMN IF NOT EXISTS project_area_task_id BIGINT NULL;
ALTER TABLE employees ADD FOREIGN KEY (project_area_task_id) REFERENCES project_area_tasks(id);
