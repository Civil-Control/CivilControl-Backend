package PSG.backEnd.model.enums.vehicle;

public enum PolicyType {
    AUTOMOTOR("automotor");

    private final String displayName;

    PolicyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
