-- Remove user_roles entries that reference soft-deleted roles.
-- These orphan associations cause 404 errors when updating users
-- because the frontend sends back deleted role IDs.
DELETE FROM user_roles
WHERE role_id IN (
    SELECT id FROM roles WHERE deleted = true
);
