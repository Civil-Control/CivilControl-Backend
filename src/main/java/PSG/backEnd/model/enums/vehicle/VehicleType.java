package PSG.backEnd.model.enums.vehicle;

public enum VehicleType {
    CAMION("camion"),
    CAMIONETA("camioneta"),
    AUTO("auto"),
    MOTO("moto"),
    OTRO("otro");

    private final String displayName;

    VehicleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
