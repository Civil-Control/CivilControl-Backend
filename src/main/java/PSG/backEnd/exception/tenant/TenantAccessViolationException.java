package PSG.backEnd.exception.tenant;

public class TenantAccessViolationException extends RuntimeException {
    public TenantAccessViolationException(String message) {
        super(message);
    }
}
