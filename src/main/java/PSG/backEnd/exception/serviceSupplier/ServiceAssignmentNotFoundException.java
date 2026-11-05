package PSG.backEnd.exception.serviceSupplier;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.service.util.MessageSourceHelper;

public class ServiceAssignmentNotFoundException extends NotFoundException {
    public ServiceAssignmentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("serviceAssignment.notFound", id));
    }
}
