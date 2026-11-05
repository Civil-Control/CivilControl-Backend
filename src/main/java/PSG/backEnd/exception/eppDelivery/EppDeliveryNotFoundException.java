package PSG.backEnd.exception.eppDelivery;

import PSG.backEnd.service.util.MessageSourceHelper;

public class EppDeliveryNotFoundException extends RuntimeException {
    public EppDeliveryNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("eppDeliveryNotFound.notFound", id));
    }
}

