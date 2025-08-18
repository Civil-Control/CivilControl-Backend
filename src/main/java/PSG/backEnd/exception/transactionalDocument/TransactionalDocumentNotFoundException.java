package PSG.backEnd.exception.transactionalDocument;

public class TransactionalDocumentNotFoundException extends RuntimeException {
    public TransactionalDocumentNotFoundException(Long id) {
        super("No transactional document found for ID: " + id);
    }
}
