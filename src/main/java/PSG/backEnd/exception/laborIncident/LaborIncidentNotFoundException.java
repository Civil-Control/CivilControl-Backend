package PSG.backEnd.exception.laborIncident;

import PSG.backEnd.service.util.MessageSourceHelper;

public class LaborIncidentNotFoundException extends RuntimeException {
    public LaborIncidentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("laborIncident.notFound", id));
    }
}
