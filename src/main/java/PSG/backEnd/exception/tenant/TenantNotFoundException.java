package PSG.backEnd.exception.tenant;

import PSG.backEnd.service.util.MessageSourceHelper;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("tenant.notFound", id));
    }
    public TenantNotFoundException(String message) {
        super(message);
    }
}
