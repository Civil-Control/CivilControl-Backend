package PSG.backEnd.exception.supplier;

public class SupplierDataConflictException extends RuntimeException {
    public SupplierDataConflictException(String message) {
        super(message);
    }

    public SupplierDataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

