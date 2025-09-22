package PSG.backEnd.exception.insurance;

public class DuplicatePolicyNumberException extends RuntimeException {
    public DuplicatePolicyNumberException(String policyNumber) {
        super("Insurance Policy with number '" + policyNumber + "' already exists");
    }
}
