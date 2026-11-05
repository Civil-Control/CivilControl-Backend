package PSG.backEnd.exception.tenant;

public class TenantDataConflictException extends RuntimeException {
    public TenantDataConflictException(String message) {
        super(message);
    }
    public TenantDataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
