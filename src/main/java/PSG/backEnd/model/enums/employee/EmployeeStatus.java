package PSG.backEnd.model.enums.employee;

public enum EmployeeStatus {
    ACTIVO("Activo"),
    LICENCIA("Licencia"),
    SUSPENDIDO("Suspendido");

    private final String displayName;

    EmployeeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
