package PSG.backEnd.model.enums;

public enum PaymentMethod {
    CASH("Cash"),
    TRANSFER("Transfer"),
    CHECK("Check");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}