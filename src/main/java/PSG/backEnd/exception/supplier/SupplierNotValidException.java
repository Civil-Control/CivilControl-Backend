package PSG.backEnd.exception.supplier;

public class SupplierNotValidException extends RuntimeException {
    public SupplierNotValidException(Long supplierId) {
        super("Supplier with ID " + supplierId + " does not exist or is not valid.");
    }
}

