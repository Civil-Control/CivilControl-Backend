package PSG.backEnd.model.enums;

public enum BuildingType {
    PLANTA("Planta"),
    DEPOSITO("Depósito"),
    OFICINA("Oficina"),
    SUCURSAL("Sucursal"),
    OTRO("Otro");

    private final String displayName;

    BuildingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
