package PSG.backEnd.exception.gasStation;

import PSG.backEnd.service.util.MessageSourceHelper;

public class FuelTypeNotAvailableException extends RuntimeException {
    /** @param fuelTypeLabel the fuel type's display label (already resolved by the caller). */
    public FuelTypeNotAvailableException(String fuelTypeLabel, Long gasStationId) {
        super(MessageSourceHelper.getMessageStatic("gasStation.fuelType.notAvailable", fuelTypeLabel, gasStationId));
    }
}
