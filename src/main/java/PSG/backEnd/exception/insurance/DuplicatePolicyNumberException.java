package PSG.backEnd.exception.insurance;

import PSG.backEnd.service.util.MessageSourceHelper;

public class DuplicatePolicyNumberException extends RuntimeException {
    public DuplicatePolicyNumberException(String policyNumber) {
        super(MessageSourceHelper.getMessageStatic("insurancePolicy.number.duplicate", policyNumber));
    }
}
