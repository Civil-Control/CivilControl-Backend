package PSG.backEnd.exception.contracts;

import PSG.backEnd.service.util.MessageSourceHelper;

public class WorkContractNotFoundException extends RuntimeException {
    public WorkContractNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("workContract.notFound", id));
    }
}
