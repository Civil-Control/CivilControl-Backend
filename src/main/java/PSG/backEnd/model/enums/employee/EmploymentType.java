package PSG.backEnd.model.enums.employee;

public enum EmploymentType {
    TIEMPO_COMPLETO("Tiempo Completo"),
    MEDIO_TIEMPO("Medio Tiempo"),
    CONTRATO_TEMPORAL("Contrato Temporal"),
    PASANTIA("Pasantía"),
    SUB_CONTRATADO("Subcontratado");

    private final String displayName;

    EmploymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
