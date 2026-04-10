package PSG.backEnd.model.enums;

public enum IvaCondition {
    RESPONSABLE_INSCRIPTO("Responsable Inscripto"),
    MONOTRIBUTISTA("Monotributista"),
    EXENTO("Exento"),
    CONSUMIDOR_FINAL("Consumidor Final");

    private final String displayName;

    IvaCondition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
