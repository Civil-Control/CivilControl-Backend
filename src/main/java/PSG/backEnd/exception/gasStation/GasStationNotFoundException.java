package PSG.backEnd.exception.gasStation;

import PSG.backEnd.exception.NotFoundException;

public class GasStationNotFoundException extends NotFoundException {
    public GasStationNotFoundException(Long id) {
        super("Gas station not found with id: " + id);
    }
}
