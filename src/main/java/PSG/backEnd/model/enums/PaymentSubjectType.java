package PSG.backEnd.model.enums;

public enum PaymentSubjectType {
    BUILDING("Inmueble"),
    VEHICLE("Vehículo");

    private final String displayName;

    PaymentSubjectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
