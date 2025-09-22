package PSG.backEnd.exception.insurance;

import PSG.backEnd.exception.NotFoundException;

public class InsurancePolicyNotFoundException extends NotFoundException {
    public InsurancePolicyNotFoundException(String message) {
        super(message);
    }

    public InsurancePolicyNotFoundException(Long id) {
        super("Insurance Policy with ID " + id + " not found");
    }

    public InsurancePolicyNotFoundException(String field, String value) {
        super("Insurance Policy with " + field + " '" + value + "' not found");
    }
}
