package PSG.backEnd.model.validation;
import PSG.backEnd.model.dto.employee.EmployeeDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
public class EmergencyContactConflictValidator implements ConstraintValidator<ValidEmergencyContact, EmployeeDTO> {
    @Override
    public boolean isValid(EmployeeDTO dto, ConstraintValidatorContext context) {
        if (dto == null || dto.emergencyContact() == null) {
            return true;
        }
        String employeePhone = dto.phoneNumber();
        String contactPhone  = dto.emergencyContact().phoneNumber();
        if (employeePhone != null && contactPhone != null
                && normalise(employeePhone).equals(normalise(contactPhone))) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "{employee.emergencyContact.phoneNumber.conflict}"
            ).addPropertyNode("emergencyContact.phoneNumber").addConstraintViolation();
            return false;
        }
        return true;
    }
    /** Strips whitespace, dashes, dots and parentheses for a format-agnostic comparison. */
    private String normalise(String value) {
        return value.replaceAll("[\\s.\\-()]", "");
    }
}