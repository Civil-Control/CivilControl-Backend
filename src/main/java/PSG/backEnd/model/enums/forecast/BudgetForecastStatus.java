package PSG.backEnd.model.enums.forecast;

/**
 * Estados del ciclo de vida de una previsión de gastos.
 */
public enum BudgetForecastStatus {
    /** Editable. Items pueden agregarse/eliminarse/modificarse. NO se puede aplicar gasto. */
    BORRADOR,
    /** Solo lectura sobre items (ni agregar, ni borrar, ni cambiar monto). SÍ se puede aplicar gasto. */
    CONFIRMADA,
    /** Período terminado, todos los items aplicados o decididos. Solo lectura total. */
    CERRADA
}
