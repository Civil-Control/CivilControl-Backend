package PSG.backEnd.exception.salaryPayment;

import PSG.backEnd.service.util.MessageSourceHelper;

public class SalaryPaymentNotFoundException extends RuntimeException {
    public SalaryPaymentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("salaryPaymentNotFound.notFound", id));
    }
}

