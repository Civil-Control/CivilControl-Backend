package PSG.backEnd.exception.gasStation;

import PSG.backEnd.service.util.MessageSourceHelper;

public class CustomFuelTypeNotFoundException extends RuntimeException {
    public CustomFuelTypeNotFoundException(String key) {
        super(MessageSourceHelper.getMessageStatic("gasStation.customFuelType.notFound", key));
    }
}
