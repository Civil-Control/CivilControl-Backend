package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.enums.forecast.BudgetForecastStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Vista completa de una previsión, con items.")
public record BudgetForecastResponseDTO(
        Long id,
        String name,
        String description,
        LocalDate periodFrom,
        LocalDate periodTo,
        BudgetForecastStatus status,
        BigDecimal totalAmount,
        BigDecimal appliedAmount,
        BigDecimal pendingAmount,
        Integer totalItemCount,
        Integer appliedItemCount,
        Integer skippedItemCount,
        Integer pendingItemCount,
        Long createdFromTemplateId,
        LocalDateTime confirmedAt,
        Long confirmedByUserId,
        LocalDateTime closedAt,
        Long closedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BudgetForecastItemResponseDTO> items
) {}
