package PSG.backEnd.exception.contracts;

public class WorkContractAlreadyExistsException extends RuntimeException {
    public WorkContractAlreadyExistsException(String message) {
        super(message);
    }
}
