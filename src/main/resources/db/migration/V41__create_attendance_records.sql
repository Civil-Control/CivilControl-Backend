CREATE TABLE attendance_records (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    employee_id     BIGINT       NOT NULL,
    date            DATE         NOT NULL,
    time            TIME         NOT NULL,
    movement_type   VARCHAR(10)  NOT NULL,
    building_id     BIGINT       NULL,
    observation     VARCHAR(500) NULL,

    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_attendance_building FOREIGN KEY (building_id) REFERENCES buildings(id),
    CONSTRAINT uk_attendance_record   UNIQUE (tenant_id, employee_id, date, time, movement_type)
);
