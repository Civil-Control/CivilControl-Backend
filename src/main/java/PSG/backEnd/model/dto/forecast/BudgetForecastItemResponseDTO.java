package PSG.backEnd.model.dto.forecast;

import PSG.backEnd.model.enums.forecast.BudgetForecastItemApplicationStatus;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Vista enriquecida de un item de previsión.")
public record BudgetForecastItemResponseDTO(
        Long id,
        Long budgetForecastId,
        Integer rowOrder,
        BudgetForecastItemType itemType,
        String description,
        LocalDate expectedDate,
        BigDecimal expectedAmount,

        Long employeeId,
        String employeeFullName,

        Long supplierId,
        String supplierLegalName,

        Long serviceAssignmentId,
        String serviceAssignmentLabel,

        Long vehicleId,
        String vehicleLicensePlate,

        Long stockId,
        String stockName,
        BigDecimal stockQuantity,

        BudgetForecastItemApplicationStatus applicationStatus,
        String appliedEntityType,
        Long appliedEntityId,
        LocalDateTime appliedAt,
        Long appliedByUserId,
        String skipReason,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
