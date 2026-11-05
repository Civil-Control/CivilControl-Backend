package PSG.backEnd.exception.employeeVacation;

import PSG.backEnd.service.util.MessageSourceHelper;
public class EmployeeVacationNotFoundException extends RuntimeException {
    public EmployeeVacationNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("employeeVacationNotFound.notFound", id));
    }
}
