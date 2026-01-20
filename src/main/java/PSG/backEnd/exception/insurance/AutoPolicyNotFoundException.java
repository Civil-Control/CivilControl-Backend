package PSG.backEnd.exception.insurance;

import PSG.backEnd.service.util.MessageSourceHelper;
import PSG.backEnd.exception.NotFoundException;

public class AutoPolicyNotFoundException extends NotFoundException {
    public AutoPolicyNotFoundException(String message) {
        super(message);
    }

    public AutoPolicyNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("autoPolicyNotFound.notFound", id));
    }

    public AutoPolicyNotFoundException(String field, String value) {
        super(MessageSourceHelper.getMessageStatic("autoPolicy.notFoundByField", field, value));
    }
}
