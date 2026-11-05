package PSG.backEnd.exception.client;

import PSG.backEnd.service.util.MessageSourceHelper;

public class SalesDocumentNotFoundException extends RuntimeException {
    public SalesDocumentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("salesDocument.notFound", id));
    }
}
