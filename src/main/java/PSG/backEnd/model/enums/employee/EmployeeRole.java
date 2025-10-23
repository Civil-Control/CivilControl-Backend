package PSG.backEnd.model.enums.employee;

public enum EmployeeRole {
    CHOFER("Chofer"),
    OFICIAL("Oficial"),
    AYUDANTE("Ayudante"),
    ADMINISTRATIVO("Administrativo"),
    LIMPIEZA("Limpieza"),
    MECANICO("Mecánico"),
    OTRO("Otro");

    private final String displayName;

    EmployeeRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
