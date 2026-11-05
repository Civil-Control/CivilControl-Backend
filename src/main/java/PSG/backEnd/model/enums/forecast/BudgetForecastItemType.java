package PSG.backEnd.model.enums.forecast;

/**
 * Tipos de gasto previsto. Cada tipo (excepto OTRO) puede materializarse en un
 * registro real mediante un {@link PSG.backEnd.service.implementation.forecast.applier.BudgetForecastItemApplier}.
 */
public enum BudgetForecastItemType {
    /** Pago de salario a empleado → genera SalaryPayment. */
    SALARIO,
    /** Pago de servicio a proveedor → genera ServicePayment (vía ServiceAssignment tipo BUILDING). */
    SERVICIO,
    /** Reparación de vehículo → genera Repair. */
    REPARACION,
    /** Compra de stock → genera StockPurchase. */
    COMPRA_STOCK,
    /** Pago de patente vehicular → genera ServicePayment (vía ServiceAssignment tipo VEHICLE). */
    PATENTE,
    /** Gasto libre informativo. NO se aplica — solo descriptivo. */
    OTRO
}
