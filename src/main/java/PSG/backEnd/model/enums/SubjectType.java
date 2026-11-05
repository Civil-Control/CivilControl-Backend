package PSG.backEnd.model.enums;

public enum SubjectType {
    BUILDING("Inmueble"),
    VEHICLE("Rodado");

    private final String displayName;

    SubjectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
