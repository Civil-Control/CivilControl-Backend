package PSG.backEnd.exception.forecast;

/**
 * Lanzada cuando falla el flujo de aplicar/revertir un ítem de previsión.
 */
public class BudgetForecastApplyException extends RuntimeException {
    public BudgetForecastApplyException(String message) {
        super(message);
    }

    public BudgetForecastApplyException(String message, Throwable cause) {
        super(message, cause);
    }
}
