-- V42__create_crew_assignments.sql

CREATE TABLE crew_assignments (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    employee_id     BIGINT       NOT NULL,
    vehicle_id      BIGINT       NOT NULL,
    project_area_id BIGINT       NOT NULL,
    date            DATE         NOT NULL,
    is_driver       BOOLEAN      NOT NULL DEFAULT FALSE,
    observation     VARCHAR(500) NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_crew_employee     FOREIGN KEY (employee_id)     REFERENCES employees(id),
    CONSTRAINT fk_crew_vehicle      FOREIGN KEY (vehicle_id)      REFERENCES vehicles(id),
    CONSTRAINT fk_crew_project_area FOREIGN KEY (project_area_id) REFERENCES project_areas(id),
    CONSTRAINT uk_crew_employee_date UNIQUE (tenant_id, employee_id, date)
);

CREATE INDEX idx_crew_date ON crew_assignments(tenant_id, date);
CREATE INDEX idx_crew_vehicle_date ON crew_assignments(tenant_id, vehicle_id, date);

-- Permisos
INSERT INTO permissions (name, module, work_module, description, spanish_translation, spanish_description) VALUES
    ('CREW_ASSIGNMENT_READ', 'Crew Assignments', 'vehicles',
     'Read crew assignments',
     'Ver Parte Diario',
     'Permite ver el parte diario de salida y el historial'),
    ('CREW_ASSIGNMENT_WRITE', 'Crew Assignments', 'vehicles',
     'Create and edit crew assignments',
     'Crear/Editar Parte Diario',
     'Permite armar el parte diario y modificar asignaciones'),
    ('CREW_ASSIGNMENT_DELETE', 'Crew Assignments', 'vehicles',
     'Delete crew assignments',
     'Eliminar Parte Diario',
     'Permite eliminar asignaciones del parte diario');
