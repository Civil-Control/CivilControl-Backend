-- V11: Add operational 'active' status to vehicles.
-- 'deleted' remains for soft-delete (record preservation only).
-- 'active' represents whether the vehicle is currently in service.
ALTER TABLE vehicles ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
