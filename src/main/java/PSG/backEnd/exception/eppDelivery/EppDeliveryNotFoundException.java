package PSG.backEnd.exception.eppDelivery;

public class EppDeliveryNotFoundException extends RuntimeException {
    public EppDeliveryNotFoundException(Long id) {
        super("No EPP delivery found for ID: " + id);
    }
}

