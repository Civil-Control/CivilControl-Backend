package PSG.backEnd.exception.projectarea;

public class ProjectAreaDataConflictException extends RuntimeException {
    public ProjectAreaDataConflictException(String message) {
        super(message);
    }

    public ProjectAreaDataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

