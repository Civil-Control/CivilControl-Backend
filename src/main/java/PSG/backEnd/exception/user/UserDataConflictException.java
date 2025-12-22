package PSG.backEnd.exception.user;

public class UserDataConflictException extends RuntimeException {
    public UserDataConflictException(String message) {
        super(message);
    }

    public UserDataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

