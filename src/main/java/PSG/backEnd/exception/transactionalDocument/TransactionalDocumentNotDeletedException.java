package PSG.backEnd.exception.transactionalDocument;

public class TransactionalDocumentNotDeletedException extends RuntimeException {
    public TransactionalDocumentNotDeletedException(String branchCode, String documentNumber) {
        super(String.format("TransactionalDocument with branch code %s and document number %s was not found in deleted state",
                branchCode, documentNumber));
    }
}
