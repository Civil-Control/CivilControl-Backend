package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Aplica varios items en una sola operación. Cada uno ejecuta su Applier.")
public record BudgetForecastItemBatchApplyDTO(
        @NotEmpty(message = "{budgetForecast.batch.items.notEmpty}", groups = OnCreate.class)
        List<Long> itemIds,

        @Schema(description = "Si true, no ejecuta — solo simula y reporta validaciones.")
        Boolean dryRun,

        @Schema(description = "Notas opcionales registradas en log de cada applier (máx 500 chars).", nullable = true)
        @Size(max = 500)
        String notes
) {}
