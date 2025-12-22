package PSG.backEnd.exception.role;

public class RoleDataConflictException extends RuntimeException {
    public RoleDataConflictException(String message) {
        super(message);
    }

    public RoleDataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

