package PSG.backEnd.model.enums.vehicle;

public enum FuelType {
    INFINIA("infinia"),
    SUPER("super"),
    INFINIA_DIESEL("infinia diesel"),
    DIESEL_500("diesel 500"),
    GNC("gnc"),
    DISTILLED_WATER("agua destilada"),
    OIL("aceite");

    private final String displayName;

    FuelType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
