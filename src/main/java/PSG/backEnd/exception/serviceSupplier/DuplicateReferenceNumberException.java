package PSG.backEnd.exception.serviceSupplier;

public class DuplicateReferenceNumberException extends RuntimeException {
    public DuplicateReferenceNumberException(String referenceNumber) {
        super("A service payment with reference number '" + referenceNumber + "' already exists");
    }
}

