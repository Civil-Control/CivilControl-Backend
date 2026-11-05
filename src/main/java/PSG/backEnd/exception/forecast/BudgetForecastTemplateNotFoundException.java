package PSG.backEnd.exception.forecast;

import PSG.backEnd.service.util.MessageSourceHelper;

public class BudgetForecastTemplateNotFoundException extends RuntimeException {
    public BudgetForecastTemplateNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("budgetForecast.template.notFound", id));
    }
}
