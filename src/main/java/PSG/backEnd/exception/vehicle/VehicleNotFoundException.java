package PSG.backEnd.exception.vehicle;

import PSG.backEnd.service.util.MessageSourceHelper;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("vehicleNotFound.notFound", id));
    }
}