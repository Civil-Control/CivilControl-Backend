package PSG.backEnd.exception.insurance;

import PSG.backEnd.service.util.MessageSourceHelper;

import PSG.backEnd.exception.NotFoundException;

public class InsurancePolicyNotFoundException extends NotFoundException {
    public InsurancePolicyNotFoundException(String message) {
        super(message);
    }

    public InsurancePolicyNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("insurancePolicy.notFound", id));
    }

    public InsurancePolicyNotFoundException(String field, String value) {
        super(MessageSourceHelper.getMessageStatic("insurancePolicy.notFoundByField", field, value));
    }
}
