package PSG.backEnd.exception.vehicle;

public class VehicleDataConflictException extends RuntimeException {
    public VehicleDataConflictException(String message) {
        super(message);
    }

    public VehicleDataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

