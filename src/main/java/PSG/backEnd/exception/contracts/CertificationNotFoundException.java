package PSG.backEnd.exception.contracts;

import PSG.backEnd.service.util.MessageSourceHelper;

public class CertificationNotFoundException extends RuntimeException {
    public CertificationNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("certification.notFound", id));
    }
}
