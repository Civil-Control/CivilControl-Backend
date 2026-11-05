package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.enums.forecast.BudgetForecastItemApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de aplicar (o intentar aplicar) un item.")
public record BudgetForecastItemApplyResultDTO(
        Long itemId,
        boolean success,
        BudgetForecastItemApplicationStatus newStatus,
        String appliedEntityType,
        Long appliedEntityId,
        String errorMessage
) {
    public static BudgetForecastItemApplyResultDTO ok(Long itemId, BudgetForecastItemApplicationStatus status,
                                                      String type, Long id) {
        return new BudgetForecastItemApplyResultDTO(itemId, true, status, type, id, null);
    }
    public static BudgetForecastItemApplyResultDTO error(Long itemId, String error) {
        return new BudgetForecastItemApplyResultDTO(itemId, false, null, null, null, error);
    }
}
