package PSG.backEnd.exception.insurance;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.service.util.MessageSourceHelper;

public class PolicyPaymentNotFoundException extends NotFoundException {
    public PolicyPaymentNotFoundException(String message) {
        super(message);
    }

    public PolicyPaymentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("policyPayment.notFound", id));
    }
}
