package PSG.backEnd.model.enums.employee;

public enum SalaryFrecuency {
    MENSUAL("Mensual"),
    QUINCENAL("Quincenal"),
    SEMANAL("Semanal");

    private final String displayName;

    SalaryFrecuency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
