package PSG.backEnd.exception.client;

import PSG.backEnd.service.util.MessageSourceHelper;

public class SalesDocumentAlreadyActiveException extends RuntimeException {
    public SalesDocumentAlreadyActiveException(String branchCode, String documentNumber, Long clientId) {
        super(MessageSourceHelper.getMessageStatic("salesDocument.alreadyActive", branchCode, documentNumber, clientId));
    }
}
