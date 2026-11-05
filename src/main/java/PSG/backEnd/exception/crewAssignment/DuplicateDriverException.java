package PSG.backEnd.exception.crewAssignment;

public class DuplicateDriverException extends RuntimeException {
    public DuplicateDriverException(String message) {
        super(message);
    }
}
