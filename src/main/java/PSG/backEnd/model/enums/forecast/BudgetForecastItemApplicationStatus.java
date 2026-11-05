package PSG.backEnd.model.enums.forecast;

/**
 * Estado de materialización de un ítem de previsión.
 */
public enum BudgetForecastItemApplicationStatus {
    /** Default — aún no se aplicó. */
    PENDIENTE,
    /** Ya generó su registro real (appliedEntityId != null). */
    APLICADO,
    /** Decidido no aplicar (con motivo en skipReason). */
    OMITIDO
}
