package PSG.backEnd.exception.gasStation;

import PSG.backEnd.service.util.MessageSourceHelper;

public class CustomFuelTypeAlreadyExistsException extends RuntimeException {
    public CustomFuelTypeAlreadyExistsException(String label) {
        super(MessageSourceHelper.getMessageStatic("gasStation.customFuelType.alreadyExists", label));
    }
}
