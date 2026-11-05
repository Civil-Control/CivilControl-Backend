package PSG.backEnd.exception.gasStation;

import PSG.backEnd.service.util.MessageSourceHelper;

import PSG.backEnd.exception.NotFoundException;

public class FuelLoadNotFoundException extends NotFoundException {
    public FuelLoadNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("fuelLoadNotFound.notFound", id));
    }
}
