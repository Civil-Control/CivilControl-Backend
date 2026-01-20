package PSG.backEnd.exception.vehicle;

import PSG.backEnd.service.util.MessageSourceHelper;

public class VehicleNotValidException extends RuntimeException {
    public VehicleNotValidException(Long vehicleId) {
        super(MessageSourceHelper.getMessageStatic("vehicle.notValid", vehicleId));
    }
}

