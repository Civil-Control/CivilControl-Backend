package PSG.backEnd.exception.insurance;

import PSG.backEnd.service.util.MessageSourceHelper;

public class DuplicateVehicleInPolicyException extends RuntimeException {
    public DuplicateVehicleInPolicyException(Long vehicleId, Long autoPolicyId) {
        super(MessageSourceHelper.getMessageStatic("policyVehicle.duplicate", vehicleId, autoPolicyId));
    }
}
