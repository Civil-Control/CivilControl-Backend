package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Filter criteria for querying vehicle licence plate payments. All fields are optional and can be combined.")
public record LicencePlatePaymentFilterDTO(

        @Schema(description = "Filter payments from this date (inclusive).",
                example = "2024-01-01",
                nullable = true)
        LocalDate dateFrom,

        @Schema(description = "Filter payments to this date (inclusive).",
                example = "2024-12-31",
                nullable = true)
        LocalDate dateTo,

        @Schema(description = "Filter by vehicle ID.",
                example = "25",
                nullable = true)
        Long vehicleId,

        @Schema(description = "Filter by vehicle license plate. Partial matches are supported.",
                example = "XYZ789",
                nullable = true)
        String vehicleLicensePlate,

        @Schema(description = "Filter by project area ID.",
                example = "5",
                nullable = true)
        Long projectAreaId,

        @Schema(description = "Minimum payment amount (inclusive).",
                example = "1000.00",
                nullable = true)
        BigDecimal minAmount,

        @Schema(description = "Maximum payment amount (inclusive).",
                example = "10000.00",
                nullable = true)
        BigDecimal maxAmount,

        @Schema(description = "Filter by payment year.",
                example = "2024",
                nullable = true)
        Integer year,

        @Schema(description = "Filter by payment period (month). 1 = January, 12 = December.",
                example = "5",
                nullable = true)
        Integer period,

        @Schema(description = "Filter by jurisdiction type.",
                example = "PROVINCIAL",
                allowableValues = {"PROVINCIAL", "MUNICIPAL", "NATIONAL"},
                nullable = true)
        String jurisdictionType
) {}
