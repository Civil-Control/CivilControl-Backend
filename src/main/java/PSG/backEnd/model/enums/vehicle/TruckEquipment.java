package PSG.backEnd.model.enums.vehicle;

public enum TruckEquipment {
    NADA("nada"),
    HIDROELEVADOR("hidroelevador"),
    HIDROGRUA("hidrogrúa");

    private final String displayName;

    TruckEquipment(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}