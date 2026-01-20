package PSG.backEnd.exception.gasStation;

import PSG.backEnd.service.util.MessageSourceHelper;
import PSG.backEnd.exception.NotFoundException;

public class GasStationNotFoundException extends NotFoundException {
    public GasStationNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("gasStation.notFound", id));
    }
}
