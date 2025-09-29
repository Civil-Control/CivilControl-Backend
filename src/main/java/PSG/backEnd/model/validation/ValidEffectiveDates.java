package PSG.backEnd.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EffectiveDatesValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEffectiveDates {
    String message() default "La fecha de inicio (effectiveFrom) debe ser anterior o igual a la fecha de fin (effectiveTo).";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

