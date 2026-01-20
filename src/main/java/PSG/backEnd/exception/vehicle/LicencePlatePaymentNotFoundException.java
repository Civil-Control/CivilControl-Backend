package PSG.backEnd.exception.vehicle;

import PSG.backEnd.service.util.MessageSourceHelper;

public class LicencePlatePaymentNotFoundException extends RuntimeException {
    public LicencePlatePaymentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("licencePlatePaymentNotFound.notFound", id));
    }
}

