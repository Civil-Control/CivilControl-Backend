package PSG.backEnd.model.enums.vehicle;

public enum PolicyStatus {
    COTIZADO("cotizado"),
    ACTIVO("activo"),
    VENCIDO("vencido"),
    CANCELADO("cancelado"),
    EXPIRADO("expirado");

    private final String displayName;

    PolicyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
