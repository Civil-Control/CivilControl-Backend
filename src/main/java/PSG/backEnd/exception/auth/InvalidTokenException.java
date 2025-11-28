package PSG.backEnd.exception.auth;

/**
 * Exception thrown when JWT token is invalid or expired.
 */
public class InvalidTokenException extends RuntimeException {
    
    public InvalidTokenException(String message) {
        super(message);
    }
}

