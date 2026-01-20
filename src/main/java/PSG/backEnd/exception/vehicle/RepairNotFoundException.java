package PSG.backEnd.exception.vehicle;

import PSG.backEnd.service.util.MessageSourceHelper;

public class RepairNotFoundException extends RuntimeException {
    public RepairNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("repairNotFound.notFound", id));
    }
}

