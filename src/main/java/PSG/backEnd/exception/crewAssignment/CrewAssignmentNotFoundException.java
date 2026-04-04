package PSG.backEnd.exception.crewAssignment;

import PSG.backEnd.service.util.MessageSourceHelper;

public class CrewAssignmentNotFoundException extends RuntimeException {
    public CrewAssignmentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("crewAssignment.notFound", id));
    }
}
