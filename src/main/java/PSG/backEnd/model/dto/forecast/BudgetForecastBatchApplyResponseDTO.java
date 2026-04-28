package PSG.backEnd.model.dto.forecast;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Respuesta agregada de una operación batch sobre items.")
public record BudgetForecastBatchApplyResponseDTO(
        int totalRequested,
        int totalSuccessful,
        int totalFailed,
        List<BudgetForecastItemApplyResultDTO> results
) {}
