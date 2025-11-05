package PSG.backEnd.exception.serviceSupplier;

public class DuplicateServicePaymentException extends RuntimeException {
    public DuplicateServicePaymentException(String message) {
        super(message);
    }
}


