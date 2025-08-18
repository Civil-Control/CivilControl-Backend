package PSG.backEnd.model.enums;

public enum DocumentType {
    BILL_A("Bill A"),
    BILL_B("Bill B"),
    BILL_C("Bill C"),
    DEBIT_NOTE_A("Debit Note A"),
    DEBIT_NOTE_B("Debit Note B"),
    DEBIT_NOTE_C("Debit Note C"),
    CREDIT_NOTE_A("Credit Note A"),
    CREDIT_NOTE_B("Credit Note B"),
    CREDIT_NOTE_C("Credit Note C"),
    OTHER_DOCUMENT("Other Document");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
