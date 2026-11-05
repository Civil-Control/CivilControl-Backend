package PSG.backEnd.exception.employee;

public class EmployeeNotValidException extends RuntimeException {
    public EmployeeNotValidException(String message) {
        super(message);
    }
}

