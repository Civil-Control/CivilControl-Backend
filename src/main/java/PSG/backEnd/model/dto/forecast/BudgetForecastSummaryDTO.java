package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.enums.forecast.BudgetForecastStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Versión liviana para listados (sin items). */
@Schema(description = "Resumen de previsión para listados.")
public record BudgetForecastSummaryDTO(
        Long id,
        String name,
        LocalDate periodFrom,
        LocalDate periodTo,
        BudgetForecastStatus status,
        BigDecimal totalAmount,
        BigDecimal appliedAmount,
        BigDecimal pendingAmount,
        Integer totalItemCount,
        Integer appliedItemCount
) {}
