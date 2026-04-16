-- Default departure/return times per sector for crew reports
CREATE TABLE crew_schedule_defaults (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    project_area_id BIGINT NOT NULL REFERENCES project_areas(id),
    departure_time  TIME,
    return_time     TIME,
    CONSTRAINT uk_schedule_default_tenant_area UNIQUE (tenant_id, project_area_id)
);
