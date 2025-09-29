package PSG.backEnd.model.enums.vehicle;

public enum PaymentFrequency {
    MENSUAL("mensual"),
    BIMESTRAL("bimestral"),
    TRIMESTRAL("trimestral"),
    SEMI_ANUAL("semianual"),
    ANUAL("anual"),
    PAGO_UNICO("pago unico");

    private final String displayName;

    PaymentFrequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
