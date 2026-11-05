package PSG.backEnd.exception.transactionalDocument;

public class TransactionalDocumentAlreadyActiveException extends RuntimeException {
    public TransactionalDocumentAlreadyActiveException(String message) {
        super(message);
    }
}
