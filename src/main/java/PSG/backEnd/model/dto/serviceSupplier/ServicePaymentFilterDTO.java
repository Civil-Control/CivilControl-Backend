package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Filter Data Transfer Object for searching service payments by various criteria.")
public record ServicePaymentFilterDTO(

        @Schema(description = "Filter by service assignment ID.")
        Long serviceAssignmentId,

        @Schema(description = "Filter by service supplier ID.")
        Long serviceSupplierId,

        @Schema(description = "Filter by building ID.")
        Long buildingId,

        @Schema(description = "Filter by project area ID.")
        Long projectAreaId,

        @Schema(description = "Filter by service type.")
        ServiceType serviceType,

        @Schema(description = "Filter by minimum payment date (inclusive).")
        LocalDate startDate,

        @Schema(description = "Filter by maximum payment date (inclusive).")
        LocalDate endDate,

        @Schema(description = "Filter by minimum amount (inclusive).")
        BigDecimal minAmount,

        @Schema(description = "Filter by maximum amount (inclusive).")
        BigDecimal maxAmount,

        @Schema(description = "Filter by reference number (partial match).")
        String referenceNumber,

        @Schema(description = "Filter by service supplier name.")
        String supplierName,

        @Schema(description = "Generic search across supplier legal name, trade name and CUIT (case-insensitive partial match).",
                example = "Edesur")
        String search
) {}

