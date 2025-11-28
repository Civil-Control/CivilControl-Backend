package PSG.backEnd.exception.user;

/**
 * Exception thrown when a user is invalid or does not comply with business rules.
 */
public class UserNotValidException extends RuntimeException {

    public UserNotValidException(String message) {
        super(message);
    }
}

