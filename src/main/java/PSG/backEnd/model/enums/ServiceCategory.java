package PSG.backEnd.model.enums;

public enum ServiceCategory {
    RODADO("Rodado"),
    INMUEBLE("Inmueble"),
    SERVICIO("Servicio"),
    TASA("Tasa"),
    IMPUESTO("Impuesto"),
    OTRO("Otro");

    private final String displayName;

    ServiceCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
