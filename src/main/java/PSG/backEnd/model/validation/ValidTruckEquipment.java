package PSG.backEnd.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TruckEquipmentValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTruckEquipment {
    String message() default "Truck equipment validation failed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

