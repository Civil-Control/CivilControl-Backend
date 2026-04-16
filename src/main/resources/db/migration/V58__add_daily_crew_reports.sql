-- V58__add_daily_crew_reports.sql
-- Adds parent "daily_crew_reports" table to support multiple partes diarios per day.
-- Each report represents one sector's crew for a day (or a MIXED report across sectors).

-- 1. Create daily_crew_reports parent table
CREATE TABLE daily_crew_reports (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    date            DATE         NOT NULL,
    project_area_id BIGINT       NULL,
    type            VARCHAR(10)  NOT NULL DEFAULT 'SECTOR',
    departure_time  TIME         NULL,
    return_time     TIME         NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_crew_report_project_area FOREIGN KEY (project_area_id) REFERENCES project_areas(id)
);

CREATE INDEX idx_crew_report_date ON daily_crew_reports(tenant_id, date);
CREATE INDEX idx_crew_report_area_date ON daily_crew_reports(tenant_id, project_area_id, date);

-- 2. Add crew_report_id FK and time fields to crew_assignments
ALTER TABLE crew_assignments
    ADD COLUMN crew_report_id BIGINT NULL,
    ADD COLUMN departure_time TIME NULL,
    ADD COLUMN return_time    TIME NULL;

ALTER TABLE crew_assignments
    ADD CONSTRAINT fk_crew_assignment_report FOREIGN KEY (crew_report_id) REFERENCES daily_crew_reports(id);

CREATE INDEX idx_crew_assignment_report ON crew_assignments(crew_report_id);

-- 3. Drop the unique constraint that prevents multiple assignments per employee per day
ALTER TABLE crew_assignments DROP CONSTRAINT IF EXISTS uk_crew_employee_date;

-- 4. Migrate existing data: create a report for each unique (tenant_id, date, project_area_id) combo
INSERT INTO daily_crew_reports (tenant_id, date, project_area_id, type, deleted)
SELECT DISTINCT ca.tenant_id, ca.date, ca.project_area_id, 'SECTOR', false
FROM crew_assignments ca
WHERE ca.deleted = false;

-- 5. Link existing assignments to their newly created reports
UPDATE crew_assignments ca
SET crew_report_id = dr.id
FROM daily_crew_reports dr
WHERE ca.tenant_id = dr.tenant_id
  AND ca.date = dr.date
  AND ca.project_area_id = dr.project_area_id
  AND ca.deleted = false
  AND dr.deleted = false;
