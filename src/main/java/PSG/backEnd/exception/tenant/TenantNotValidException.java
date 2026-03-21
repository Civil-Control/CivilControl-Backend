package PSG.backEnd.exception.tenant;

public class TenantNotValidException extends RuntimeException {
    public TenantNotValidException(String message) {
        super(message);
    }
}
