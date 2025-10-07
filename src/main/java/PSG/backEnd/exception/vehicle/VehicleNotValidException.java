package PSG.backEnd.exception.vehicle;

public class VehicleNotValidException extends RuntimeException {
    public VehicleNotValidException(Long vehicleId) {
        super("Vehicle with ID " + vehicleId + " does not exist or is not valid.");
    }
}

