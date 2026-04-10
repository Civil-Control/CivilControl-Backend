package PSG.backEnd.model.enums;

public enum Periodicity {
    MENSUAL("Mensual"),
    BIMESTRAL("Bimestral"),
    TRIMESTRAL("Trimestral"),
    SEMESTRAL("Semestral"),
    ANUAL("Anual"),
    IRREGULAR("Irregular");

    private final String displayName;

    Periodicity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
