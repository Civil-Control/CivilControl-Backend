package PSG.backEnd.exception.role;

/**
 * Exception thrown when attempting to create a role that already exists.
 */
public class RoleAlreadyExistsException extends RuntimeException {
    
    public RoleAlreadyExistsException(String name) {
        super("Role already exists with name: " + name);
    }
}

