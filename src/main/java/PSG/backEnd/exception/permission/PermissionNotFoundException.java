package PSG.backEnd.exception.permission;

/**
 * Exception thrown when a requested permission is not found.
 */
public class PermissionNotFoundException extends RuntimeException {

    public PermissionNotFoundException(Long id) {
        super("Permission not found with id: " + id);
    }

    public PermissionNotFoundException(String name) {
        super("Permission not found with name: " + name);
    }
}

