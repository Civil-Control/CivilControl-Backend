package PSG.backEnd.model.enums;

public enum DueDateMode {
    ESTIMATED("Estimado"),
    SPECIFIC("Fechas Exactas");

    private final String displayName;

    DueDateMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
