package PSG.backEnd.exception.building;

import PSG.backEnd.service.util.MessageSourceHelper;

public class BuildingNotFoundException extends RuntimeException {
    public BuildingNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("buildingNotFound.notFound", id));
    }

    public BuildingNotFoundException(String message) {
        super(message);
    }
}

