package PSG.backEnd.exception.purchaseOrder;

public class PurchaseOrderNotValidException extends RuntimeException {
    public PurchaseOrderNotValidException(String message) {
        super(message);
    }
}
