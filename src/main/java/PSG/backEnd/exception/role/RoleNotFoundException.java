package PSG.backEnd.exception.role;

/**
 * Exception thrown when a requested role is not found.
 */
public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(Long id) {
        super("Role not found with id: " + id);
    }

    public RoleNotFoundException(String name) {
        super("Role not found with name: " + name);
    }
}
