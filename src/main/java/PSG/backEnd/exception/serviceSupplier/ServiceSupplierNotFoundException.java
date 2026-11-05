package PSG.backEnd.exception.serviceSupplier;

import PSG.backEnd.service.util.MessageSourceHelper;

import PSG.backEnd.exception.NotFoundException;

public class ServiceSupplierNotFoundException extends NotFoundException {
    public ServiceSupplierNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("serviceSupplierNotFound.notFound", id));
    }
}

