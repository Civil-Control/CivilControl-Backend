package PSG.backEnd.exception.vehicle;

public class RepairOrderNotValidException extends RuntimeException {
    public RepairOrderNotValidException(String message) {
        super(message);
    }
}
