package PSG.backEnd.exception.vehicle;

import PSG.backEnd.service.util.MessageSourceHelper;

public class VehicleTypeNotFoundException extends RuntimeException {
    public VehicleTypeNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("vehicleType.notFound", id));
    }
}

