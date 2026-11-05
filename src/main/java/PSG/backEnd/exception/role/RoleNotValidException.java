package PSG.backEnd.exception.role;

/**
 * Exception thrown when a role is invalid or does not comply with business rules.
 */
public class RoleNotValidException extends RuntimeException {

    public RoleNotValidException(String message) {
        super(message);
    }
}

