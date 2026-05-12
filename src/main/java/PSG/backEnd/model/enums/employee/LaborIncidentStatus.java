package PSG.backEnd.model.enums.employee;

public enum LaborIncidentStatus {
    PENDIENTE("Pendiente"),
    EN_INVESTIGACION("En investigación"),
    RESUELTO("Resuelto");

    private final String displayName;

    LaborIncidentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
