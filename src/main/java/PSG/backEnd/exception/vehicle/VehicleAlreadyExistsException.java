package PSG.backEnd.exception.vehicle;

public class VehicleAlreadyExistsException extends RuntimeException {
    public VehicleAlreadyExistsException(String message) {
        super(message);
    }
}
