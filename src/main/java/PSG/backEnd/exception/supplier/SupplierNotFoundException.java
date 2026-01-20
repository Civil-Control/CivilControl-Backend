package PSG.backEnd.exception.supplier;

import PSG.backEnd.service.util.MessageSourceHelper;

public class SupplierNotFoundException extends RuntimeException {
    public SupplierNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("supplier.notFound", id));
    }
}
