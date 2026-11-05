ALTER TABLE credit_note_applications
    ADD COLUMN payment_details_id BIGINT NULL,
    ADD CONSTRAINT fk_cna_payment_details
        FOREIGN KEY (payment_details_id) REFERENCES payment_details (id);

CREATE INDEX idx_cna_payment_details_id ON credit_note_applications (payment_details_id);
