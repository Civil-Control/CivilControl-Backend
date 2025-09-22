package PSG.backEnd.exception.insurance;

public class DuplicateVehicleInPolicyException extends RuntimeException {
    public DuplicateVehicleInPolicyException(Long vehicleId, Long autoPolicyId) {
        super("Vehicle with ID " + vehicleId + " is already assigned to Auto Policy " + autoPolicyId);
    }
}
