package PSG.backEnd.exception.insurance;

public class InsurancePolicyDataConflictException extends RuntimeException {
    public InsurancePolicyDataConflictException(String message) {
        super(message);
    }

    public InsurancePolicyDataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

