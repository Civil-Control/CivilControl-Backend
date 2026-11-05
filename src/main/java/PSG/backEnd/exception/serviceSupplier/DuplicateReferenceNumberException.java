package PSG.backEnd.exception.serviceSupplier;

import PSG.backEnd.service.util.MessageSourceHelper;

public class DuplicateReferenceNumberException extends RuntimeException {
    public DuplicateReferenceNumberException(String referenceNumber) {
        super(MessageSourceHelper.getMessageStatic("servicePayment.referenceNumber.duplicate", referenceNumber));
    }
}

