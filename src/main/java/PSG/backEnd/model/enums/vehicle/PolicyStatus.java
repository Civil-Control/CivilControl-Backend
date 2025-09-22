package PSG.backEnd.model.enums.vehicle;

public enum PolicyStatus {
    QUOTED("cotizado"),
    ACTIVE("activo"),
    LAPSED("vencido"),
    CANCELLED("cancelado"),
    EXPIRED("expirado");

    private final String displayName;

    PolicyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
