package PSG.backEnd.model.dto.forecast;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado de un import desde Excel de items de previsión.")
public record BudgetForecastImportResultDTO(
        boolean dryRun,
        Long budgetForecastId,
        int totalRows,
        int importedRows,
        int errorRows,
        List<BudgetForecastImportRowErrorDTO> errors,
        List<BudgetForecastImportRowWarningDTO> warnings
) {

    @Schema(description = "Error que impide importar la fila.")
    public record BudgetForecastImportRowErrorDTO(int rowNumber, String code, String message) {}

    @Schema(description = "Advertencia (la fila se importa).")
    public record BudgetForecastImportRowWarningDTO(int rowNumber, String code, String message) {}
}
