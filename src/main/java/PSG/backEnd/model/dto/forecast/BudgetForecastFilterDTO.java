package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.enums.forecast.BudgetForecastStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filtros de búsqueda de previsiones.")
public record BudgetForecastFilterDTO(
        LocalDate periodFromGte,
        LocalDate periodToLte,
        List<BudgetForecastStatus> statuses,
        String search,
        Boolean hasAppliedItems
) {}
