package PSG.backEnd.exception.supplier;

import PSG.backEnd.service.util.MessageSourceHelper;

public class SupplierNotValidException extends RuntimeException {
    public SupplierNotValidException(Long supplierId) {
        super(MessageSourceHelper.getMessageStatic("supplierNotValid.notValid", supplierId));
    }
}

