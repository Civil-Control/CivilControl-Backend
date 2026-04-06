package PSG.backEnd.exception.serviceSupplier;

public class ServiceAssignmentAlreadyExistsException extends RuntimeException {
    public ServiceAssignmentAlreadyExistsException(String message) {
        super(message);
    }
}
