package PSG.backEnd.exception.vehicle;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(Long id) {
        super("No vehicle found for ID: " + id);
    }
}