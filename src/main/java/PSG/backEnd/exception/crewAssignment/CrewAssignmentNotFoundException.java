package PSG.backEnd.exception.crewAssignment;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.service.util.MessageSourceHelper;

public class CrewAssignmentNotFoundException extends NotFoundException {
    public CrewAssignmentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("crewAssignment.notFound", id));
    }
}
