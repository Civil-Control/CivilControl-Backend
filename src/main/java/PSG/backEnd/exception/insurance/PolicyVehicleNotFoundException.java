package PSG.backEnd.exception.insurance;

import PSG.backEnd.exception.NotFoundException;

public class PolicyVehicleNotFoundException extends NotFoundException {
    public PolicyVehicleNotFoundException(String message) {
        super(message);
    }

    public PolicyVehicleNotFoundException(Long id) {
        super("Policy Vehicle with ID " + id + " not found");
    }

    public PolicyVehicleNotFoundException(String field, String value) {
        super("Policy Vehicle with " + field + " '" + value + "' not found");
    }
}
