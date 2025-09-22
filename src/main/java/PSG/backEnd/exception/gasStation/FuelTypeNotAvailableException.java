package PSG.backEnd.exception.gasStation;

import PSG.backEnd.model.enums.vehicle.FuelType;

public class FuelTypeNotAvailableException extends RuntimeException {
    public FuelTypeNotAvailableException(FuelType fuelType, Long gasStationId) {
        super("Fuel type '" + fuelType.getDisplayName() + "' is not available at gas station with id: " + gasStationId);
    }
}
