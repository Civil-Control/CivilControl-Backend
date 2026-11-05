package PSG.backEnd.model.enums.vehicle;

public enum JurisdictionType {
    MUNICIPAL("municipal"),
    PROVINCIAL("provincial");

    private final String displayName;

    JurisdictionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}