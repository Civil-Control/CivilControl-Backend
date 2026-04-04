package PSG.backEnd.exception.crewAssignment;

public class DuplicateCrewAssignmentException extends RuntimeException {
    public DuplicateCrewAssignmentException(String message) {
        super(message);
    }
}
