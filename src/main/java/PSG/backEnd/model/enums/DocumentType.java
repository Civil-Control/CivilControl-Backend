package PSG.backEnd.model.enums;

public enum DocumentType {
    BILL_A("factura A"),
    BILL_B("factura B"),
    BILL_C("factura C"),
    DEBIT_NOTE_A("nota de debito A"),
    DEBIT_NOTE_B("nota de debito B"),
    DEBIT_NOTE_C("nota de debito C"),
    CREDIT_NOTE_A("nota de credito A"),
    CREDIT_NOTE_B("nota de credito B"),
    CREDIT_NOTE_C("nota de credito C"),
    OTHER_DOCUMENT("otros documentos");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
