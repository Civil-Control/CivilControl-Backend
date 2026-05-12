package PSG.backEnd.model.enums.employee;

public enum LaborIncidentType {
    ACCIDENTE_VEHICULAR("Accidente vehicular"),
    MULTA_TRANSITO("Multa de tránsito"),
    DANO_INFRAESTRUCTURA("Daño a infraestructura"),
    DANO_REDES_SERVICIO("Daño a redes de servicio"),
    DANO_MATERIAL("Daño a material o equipo"),
    INFRACCION_NORMATIVA("Infracción normativa"),
    OTRO("Otro");

    private final String displayName;

    LaborIncidentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
