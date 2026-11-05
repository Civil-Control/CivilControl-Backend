package PSG.backEnd.exception.employee;

public class EmployeeDataConflictException extends RuntimeException {
    public EmployeeDataConflictException(String message) {
        super(message);
    }

    public EmployeeDataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

