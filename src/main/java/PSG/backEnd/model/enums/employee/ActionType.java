package PSG.backEnd.model.enums.employee;

public enum ActionType {
    SUSPENSION("Suspensión"),
    AMONESTACION("Amonestación");

    private final String displayName;

    ActionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
