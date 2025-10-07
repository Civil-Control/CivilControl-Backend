package PSG.backEnd.model.enums.vehicle;

public enum RepairType {
    MANTENIMIENTO_PREVENTIVO("Mantenimiento preventivo"),
    CAMBIO_ACEITE("Cambio de aceite"),
    CAMBIO_FILTROS("Cambio de filtros"),
    SERVICIO_FRENOS("Servicio de frenos"),
    CAMBIO_NEUMATICOS("Cambio de neumáticos"),
    SUSPENSION_DIRECCION("Suspensión y dirección"),
    REPARACION_MOTOR("Reparación de motor"),
    SISTEMA_REFRIGERACION("Sistema de refrigeración"),
    SISTEMA_COMBUSTIBLE("Sistema de combustible"),
    TRANSMISION_EMBRAGUE("Transmisión y embrague"),
    SISTEMA_ELECTRICO("Sistema eléctrico"),
    CAMBIO_BATERIA("Cambio de batería"),
    SISTEMA_ESCAPE("Sistema de escape"),
    CHAPA_PINTURA("Chapa y pintura"),
    VIDRIOS_LUCES("Vidrios y luces"),
    ACCESORIOS("Accesorios y equipamiento"),
    INSPECCION_VTV("Inspección o VTV"),
    REPARACION_SINIESTRO("Reparación por siniestro"),
    MANO_OBRA_INTERNA("Mano de obra interna"),
    TALLER_EXTERNO("taller externo"),
    OTROS("Otros");

    private final String displayName;

    RepairType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}