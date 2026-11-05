package PSG.backEnd.model.dto.forecast;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Marca un item como OMITIDO (no se materializará).")
public record BudgetForecastItemSkipDTO(
        @NotBlank(message = "{budgetForecast.skip.reason.required}")
        @Size(max = 500, message = "{budgetForecast.skip.reason.size}")
        String reason
) {}
