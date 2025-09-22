package PSG.backEnd.exception.gasStation;

import PSG.backEnd.exception.NotFoundException;

public class FuelLoadNotFoundException extends NotFoundException {
    public FuelLoadNotFoundException(Long id) {
        super("Fuel load not found with id: " + id);
    }
}
