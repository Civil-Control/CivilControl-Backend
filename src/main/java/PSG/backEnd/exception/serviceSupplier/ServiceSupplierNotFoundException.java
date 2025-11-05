package PSG.backEnd.exception.serviceSupplier;

import PSG.backEnd.exception.NotFoundException;

public class ServiceSupplierNotFoundException extends NotFoundException {
    public ServiceSupplierNotFoundException(Long id) {
        super("Service supplier not found with id: " + id);
    }
}

