ALTER TABLE insurance_policies
    ADD COLUMN due_at_start_of_period BOOLEAN NOT NULL DEFAULT FALSE;
