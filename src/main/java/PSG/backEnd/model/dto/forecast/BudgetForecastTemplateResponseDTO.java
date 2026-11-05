package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Vista enriquecida de plantilla.")
public record BudgetForecastTemplateResponseDTO(
        Long id,
        String name,
        String description,
        Integer defaultPeriodDays,
        Boolean active,
        Integer itemCount,
        BigDecimal expectedTotal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BudgetForecastTemplateItemResponseDTO> items
) {

    @Schema(description = "Item de plantilla enriquecido.")
    public record BudgetForecastTemplateItemResponseDTO(
            Long id,
            Integer rowOrder,
            BudgetForecastItemType itemType,
            String description,
            Integer dayOffset,
            BigDecimal expectedAmount,
            Long employeeId, String employeeFullName,
            Long supplierId, String supplierLegalName,
            Long serviceAssignmentId, String serviceAssignmentLabel,
            Long vehicleId, String vehicleLicensePlate,
            Long stockId, String stockName,
            BigDecimal stockQuantity
    ) {}
}
