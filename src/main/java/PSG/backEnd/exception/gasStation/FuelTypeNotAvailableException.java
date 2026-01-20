package PSG.backEnd.exception.gasStation;

import PSG.backEnd.service.util.MessageSourceHelper;
import PSG.backEnd.model.enums.vehicle.FuelType;

public class FuelTypeNotAvailableException extends RuntimeException {
    public FuelTypeNotAvailableException(FuelType fuelType, Long gasStationId) {
        super(MessageSourceHelper.getMessageStatic("gasStation.fuelType.notAvailable", fuelType.getDisplayName(), gasStationId));
    }
}
