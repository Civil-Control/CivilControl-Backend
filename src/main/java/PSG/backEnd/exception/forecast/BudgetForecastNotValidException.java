package PSG.backEnd.exception.forecast;

public class BudgetForecastNotValidException extends RuntimeException {
    public BudgetForecastNotValidException(String message) {
        super(message);
    }
}
