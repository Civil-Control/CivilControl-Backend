package PSG.backEnd.model.enums;

public enum PaymentMethod {
    CASH("efectivo"),
    TRANSFER("transferencia"),
    CHECK("cheque"),;

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}