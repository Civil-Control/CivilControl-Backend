package PSG.backEnd.exception.vehicle;

public class RepairNotFoundException extends RuntimeException {
    public RepairNotFoundException(Long id) {
        super("No repair found for ID: " + id);
    }
}

