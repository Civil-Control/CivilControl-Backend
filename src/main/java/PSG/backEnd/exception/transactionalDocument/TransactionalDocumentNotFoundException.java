package PSG.backEnd.exception.transactionalDocument;

import PSG.backEnd.service.util.MessageSourceHelper;

public class TransactionalDocumentNotFoundException extends RuntimeException {
    public TransactionalDocumentNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("transactionalDocumentNotFound.notFound", id));
    }
}
