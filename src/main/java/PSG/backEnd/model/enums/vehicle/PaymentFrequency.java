package PSG.backEnd.model.enums.vehicle;

public enum PaymentFrequency {
    MONTHLY("mensual"),
    BIMONTHLY("bimestral"),
    QUARTERLY("trimestral"),
    SEMIANNUAL("semestral"),
    ANNUAL("anual"),
    SINGLE_PAYMENT("pago unico");

    private final String displayName;

    PaymentFrequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
