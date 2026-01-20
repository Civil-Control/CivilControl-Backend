package PSG.backEnd.exception.employee;

import PSG.backEnd.service.util.MessageSourceHelper;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("employeeNotFound.notFound", id));
    }
}

