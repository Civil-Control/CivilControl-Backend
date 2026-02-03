package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Filter Data Transfer Object for searching service payments by various criteria.")
public record ServicePaymentFilterDTO(

        @Schema(description = "Filter by service supplier ID.",
                example = "5")
        Long serviceSupplierId,

        @Schema(description = "Filter by building ID.",
                example = "12")
        Long buildingId,

        @Schema(description = "Filter by project area ID.",
                example = "5")
        Long projectAreaId,

        @Schema(description = "Filter by service type.",
                example = "LUZ")
        ServiceType serviceType,

        @Schema(description = "Filter by minimum payment date (inclusive).",
                example = "2025-01-01")
        LocalDate startDate,

        @Schema(description = "Filter by maximum payment date (inclusive).",
                example = "2025-12-31")
        LocalDate endDate,

        @Schema(description = "Filter by minimum amount (inclusive).",
                example = "1000.00")
        BigDecimal minAmount,

        @Schema(description = "Filter by maximum amount (inclusive).",
                example = "50000.00")
        BigDecimal maxAmount,

        @Schema(description = "Filter by reference number (partial match).",
                example = "INV-2025")
        String referenceNumber
) {}

