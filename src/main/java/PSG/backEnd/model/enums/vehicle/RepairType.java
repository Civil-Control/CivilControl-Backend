package PSG.backEnd.model.enums.vehicle;

public enum RepairType {
    ARRANQUE("Arranque"),
    ALTERNADOR("Alternador"),
    BARRAS_DIRECCION("Barras de Dirección"),
    CAMBIO_ACEITE("Cambio de Aceite"),
    CAMBIO_FILTROS("Cambio de Filtros"),
    CERRADURA("Cerradura y Llaves"),
    CHAPA_PINTURA("Chapa y Pintura"),
    FRENOS("Frenos"),
    GASTOS_HOMOLOGACION("Gastos de Homologación"),
    HIDRAULICA("Sistema Hidráulico"),
    INSPECCION_VTV("Inspección / VTV"),
    MOTOR("Reparación de Motor"),
    NEUMATICOS("Neumáticos"),
    REPARACION_SINIESTRO("Reparación por Siniestro"),
    SISTEMA_ELECTRICO("Sistema Eléctrico"),
    TANQUE_COMBUSTIBLE("Tanque de Combustible"),
    SUSPENSION_DIRECCION("Suspensión y Dirección"),
    TALLER_EXTERNO("Taller Externo"),
    TRANSMISION("Transmisión / Caja"),
    OTROS("Otros");

    private final String displayName;

    RepairType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}