package PSG.backEnd.service.webhook;

/**
 * Thrown when a biometric clock payload cannot be parsed.
 * The service layer catches this to route the raw payload to the orphan (dead-letter) table.
 */
public class BiometricParseException extends RuntimeException {

    public BiometricParseException(String message) {
        super(message);
    }

    public BiometricParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
