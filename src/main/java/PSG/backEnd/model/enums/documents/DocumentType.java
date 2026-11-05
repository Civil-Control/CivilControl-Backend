package PSG.backEnd.model.enums.documents;

public enum DocumentType {
    BILL_A("Factura A"),
    BILL_B("Factura B"),
    BILL_C("Factura C"),
    DEBIT_NOTE_A("Nota de débito A"),
    DEBIT_NOTE_B("Nota de débito B"),
    DEBIT_NOTE_C("Nota de débito C"),
    CREDIT_NOTE_A("Nota de crédito A"),
    CREDIT_NOTE_B("Nota de crédito B"),
    CREDIT_NOTE_C("Nota de crédito C"),
    OTHER_DOCUMENT("Otros documentos");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
