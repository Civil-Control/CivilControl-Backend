package PSG.backEnd.exception.vehicle;

import PSG.backEnd.service.util.MessageSourceHelper;

public class RepairOrderNotFoundException extends RuntimeException {
    public RepairOrderNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("repairOrder.notFound", id));
    }
}
