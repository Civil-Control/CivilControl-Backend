package PSG.backEnd.model.validation;

import PSG.backEnd.model.dto.insurance.InsurancePolicyDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EffectiveDatesValidator implements ConstraintValidator<ValidEffectiveDates, InsurancePolicyDTO> {
    @Override
    public boolean isValid(InsurancePolicyDTO dto, ConstraintValidatorContext context) {
        if (dto == null) return true;
        if (dto.effectiveFrom() == null || dto.effectiveTo() == null) return true;
        return !dto.effectiveFrom().isAfter(dto.effectiveTo());
    }
}

