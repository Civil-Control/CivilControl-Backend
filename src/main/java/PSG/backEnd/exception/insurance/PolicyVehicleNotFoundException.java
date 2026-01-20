package PSG.backEnd.exception.insurance;

import PSG.backEnd.service.util.MessageSourceHelper;

import PSG.backEnd.exception.NotFoundException;

public class PolicyVehicleNotFoundException extends NotFoundException {
    public PolicyVehicleNotFoundException(String message) {
        super(message);
    }

    public PolicyVehicleNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("policyVehicle.notFound", id));
    }

    public PolicyVehicleNotFoundException(String field, String value) {
        super(MessageSourceHelper.getMessageStatic("policyVehicle.notFoundByField", field, value));
    }
}
