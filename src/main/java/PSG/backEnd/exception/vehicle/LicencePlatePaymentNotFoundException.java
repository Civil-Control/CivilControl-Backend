package PSG.backEnd.exception.vehicle;

public class LicencePlatePaymentNotFoundException extends RuntimeException {
    public LicencePlatePaymentNotFoundException(Long id) {
        super("No licence plate payment found for ID: " + id);
    }
}

