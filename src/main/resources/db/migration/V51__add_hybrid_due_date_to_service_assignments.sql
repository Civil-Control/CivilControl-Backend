-- Add hybrid due date model to service_assignments
ALTER TABLE service_assignments
    ADD COLUMN due_date_mode VARCHAR(20),
    ADD COLUMN periodicity VARCHAR(20);

-- Migrate existing data: assignments with estimated_due_day get ESTIMATED mode
UPDATE service_assignments
SET due_date_mode = 'ESTIMATED'
WHERE estimated_due_day IS NOT NULL;

-- Create specific_due_dates table for exact date mode
CREATE TABLE specific_due_dates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    due_date DATE NOT NULL,
    service_assignment_id BIGINT NOT NULL,
    CONSTRAINT fk_specific_due_date_assignment
        FOREIGN KEY (service_assignment_id) REFERENCES service_assignments(id)
);

CREATE INDEX idx_specific_due_dates_assignment ON specific_due_dates(service_assignment_id);
