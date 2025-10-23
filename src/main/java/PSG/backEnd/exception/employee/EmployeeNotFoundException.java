package PSG.backEnd.exception.employee;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(Long id) {
        super("No employee found for ID: " + id);
    }
}

