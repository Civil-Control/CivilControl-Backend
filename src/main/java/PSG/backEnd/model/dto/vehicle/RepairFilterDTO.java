package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Filter criteria for querying vehicle repairs. All fields are optional and can be combined.")
public record RepairFilterDTO(

        @Schema(description = "Filter repairs from this date (inclusive).",
                example = "2024-01-01",
                nullable = true)
        LocalDate dateFrom,

        @Schema(description = "Filter repairs to this date (inclusive).",
                example = "2024-12-31",
                nullable = true)
        LocalDate dateTo,

        @Schema(description = "Filter by vehicle ID.",
                example = "15",
                nullable = true)
        Long vehicleId,

        @Schema(description = "Filter by vehicle license plate. Partial matches are supported.",
                example = "ABC123",
                nullable = true)
        String vehicleLicensePlate,

        @Schema(description = "Filter by project area ID.",
                example = "5",
                nullable = true)
        Long projectAreaId,

        @Schema(description = "Minimum repair cost (inclusive).",
                example = "100.00",
                nullable = true)
        BigDecimal minCost,

        @Schema(description = "Maximum repair cost (inclusive).",
                example = "5000.00",
                nullable = true)
        BigDecimal maxCost,

        @Schema(description = "Filter by employee name who performed the repair. Partial matches are supported.",
                example = "John",
                nullable = true)
        String employee,

        @Schema(description = "Filter by supplier ID.",
                example = "8",
                nullable = true)
        Long supplierId,

        @Schema(description = "Filter by supplier legal name. Partial matches are supported.",
                example = "Auto Repairs",
                nullable = true)
        String supplierLegalName,

        @Schema(description = "Filter by repair type.",
                example = "PREVENTIVE",
                allowableValues = {"PREVENTIVE", "CORRECTIVE", "PREDICTIVE"},
                nullable = true)
        String repairType
) {}
