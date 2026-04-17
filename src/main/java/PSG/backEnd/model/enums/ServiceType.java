package PSG.backEnd.model.enums;

public enum ServiceType {
    LUZ("Luz"),
    AGUA("Agua"),
    GAS("Gas"),
    INTERNET("Internet"),
    TELEFONIA("Telefonia"),
    MUNICIPALES("Tasas Municipales"),
    PROVINCIALES("Servicios Provinciales"),
    NACIONALES("Servicios Nacionales"),
    OTRO("Otro");


    private final String displayName;

    ServiceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
