package PSG.backEnd.model.enums.employee;

public enum MovementType {
    ENTRADA("Entrada"),
    SALIDA("Salida");

    private final String displayName;

    MovementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
