package PSG.backEnd.exception.serviceSupplier;

import PSG.backEnd.exception.NotFoundException;

public class ServicePaymentNotFoundException extends NotFoundException {
    public ServicePaymentNotFoundException(Long id) {
        super("Service payment not found with id: " + id);
    }
}