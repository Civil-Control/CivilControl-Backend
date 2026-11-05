package PSG.backEnd.exception.serviceSupplier;

import PSG.backEnd.service.util.MessageSourceHelper;

import PSG.backEnd.exception.NotFoundException;

public class ServicePaymentNotFoundException extends NotFoundException {
    public ServicePaymentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("servicePaymentNotFound.notFound", id));
    }
}