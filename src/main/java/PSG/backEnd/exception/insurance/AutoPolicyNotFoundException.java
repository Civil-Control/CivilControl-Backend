package PSG.backEnd.exception.insurance;

import PSG.backEnd.exception.NotFoundException;

public class AutoPolicyNotFoundException extends NotFoundException {
    public AutoPolicyNotFoundException(String message) {
        super(message);
    }

    public AutoPolicyNotFoundException(Long id) {
        super("Auto Policy with ID " + id + " not found");
    }

    public AutoPolicyNotFoundException(String field, String value) {
        super("Auto Policy with " + field + " '" + value + "' not found");
    }
}
