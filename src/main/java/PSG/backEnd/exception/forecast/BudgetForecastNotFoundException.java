package PSG.backEnd.exception.forecast;

import PSG.backEnd.service.util.MessageSourceHelper;

public class BudgetForecastNotFoundException extends RuntimeException {
    public BudgetForecastNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("budgetForecast.notFound", id));
    }
}
