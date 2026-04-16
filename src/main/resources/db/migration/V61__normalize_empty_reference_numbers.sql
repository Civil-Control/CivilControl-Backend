-- Normalize empty reference_number strings to NULL to prevent unique constraint violations
UPDATE service_payments SET reference_number = NULL WHERE reference_number = '';
