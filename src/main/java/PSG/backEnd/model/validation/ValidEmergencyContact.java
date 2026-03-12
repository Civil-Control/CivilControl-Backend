package PSG.backEnd.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmergencyContactConflictValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmergencyContact {
    String message() default "Emergency contact phone number must differ from the employee's own phone number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

