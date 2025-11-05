package PSG.backEnd.exception.serviceSupplier;

public class ServicePaymentNotValidException extends RuntimeException {
    public ServicePaymentNotValidException(String message) {
        super(message);
    }
}

