package PSG.backEnd.exception.serviceSupplier;

public class DuplicateServiceTypeException extends RuntimeException {
    public DuplicateServiceTypeException(String message) {
        super(message);
    }
}

