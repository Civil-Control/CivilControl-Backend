package PSG.backEnd.service.implementation.forecast.applier;

import PSG.backEnd.exception.forecast.BudgetForecastApplyException;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resuelve el {@link BudgetForecastItemApplier} correcto según {@link BudgetForecastItemType}.
 * Auto-registra todos los appliers presentes en el ApplicationContext al arrancar.
 */
@Component
@Slf4j
public class BudgetForecastItemApplierRegistry {

    private final Map<BudgetForecastItemType, BudgetForecastItemApplier> appliers = new EnumMap<>(BudgetForecastItemType.class);

    public BudgetForecastItemApplierRegistry(List<BudgetForecastItemApplier> all) {
        for (BudgetForecastItemApplier a : all) {
            appliers.put(a.supportedType(), a);
            log.info("Registered BudgetForecastItemApplier for type {}: {}", a.supportedType(), a.getClass().getSimpleName());
        }
    }

    public BudgetForecastItemApplier resolve(BudgetForecastItemType type) {
        BudgetForecastItemApplier a = appliers.get(type);
        if (a == null) {
            throw new BudgetForecastApplyException(
                    "No applier available for item type " + type
                    + ". OTRO is informativo y no debe aplicarse.");
        }
        return a;
    }

    public boolean hasApplier(BudgetForecastItemType type) {
        return appliers.containsKey(type);
    }
}
