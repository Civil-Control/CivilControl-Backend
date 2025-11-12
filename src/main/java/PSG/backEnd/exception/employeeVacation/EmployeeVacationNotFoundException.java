package PSG.backEnd.exception.employeeVacation;
public class EmployeeVacationNotFoundException extends RuntimeException {
    public EmployeeVacationNotFoundException(Long id) {
        super("No employee vacation found for ID: " + id);
    }
}
