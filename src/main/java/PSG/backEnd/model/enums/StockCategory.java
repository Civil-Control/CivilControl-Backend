package PSG.backEnd.model.enums;

public enum StockCategory {
    HERRAMIENTAS_MANUALES("Herramientas manuales"),
    HERRAMIENTAS_ELECTRICAS("Herramientas eléctricas"),
    EQUIPOS_PESADOS("Equipos pesados"),
    MAQUINARIA("Maquinaria"),
    INSUMOS("Insumos"),
    SEGURIDAD_PERSONAL("Elementos de seguridad personal"),
    ROPA_TRABAJO("Ropa de trabajo"),
    EQUIPAMIENTO_OBRA("Equipamiento de obra"),
    ACCESORIO_VEHICULAR("Accesorios vehiculares"),
    LIMPIEZA_MANTENIMIENTO("Limpieza y mantenimiento"),
    REPUESTOS("Repuestos y componentes"),
    OTROS("Otros");

    private final String displayName;

    StockCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
