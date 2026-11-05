package PSG.backEnd.model.enums.vehicle;

public enum RepairItemType {
    MATERIAL("Material"),
    MANO_DE_OBRA("Mano de Obra");

    private final String displayName;

    RepairItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
