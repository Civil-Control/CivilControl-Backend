package PSG.backEnd.model.dto.gasStation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Filter criteria for querying fuel load transactions. All fields are optional and can be combined.")
public record FuelLoadFilterDTO(

        @Schema(description = "Filter fuel loads from this date (inclusive).",
                example = "2024-01-01",
                nullable = true)
        LocalDate dateFrom,

        @Schema(description = "Filter fuel loads to this date (inclusive).",
                example = "2024-12-31",
                nullable = true)
        LocalDate dateTo,

        @Schema(description = "Filter by branch code. Partial matches are supported.",
                example = "12345",
                nullable = true)
        String branchCode,

        @Schema(description = "Filter by ticket number. Partial matches are supported.",
                example = "87654321",
                nullable = true)
        String ticketNumber,

        @Schema(description = "Filter by fuel type.",
                example = "NAFTA_SUPER",
                allowableValues = {"NAFTA_SUPER", "NAFTA_COMUN", "DIESEL", "GNC"},
                nullable = true)
        String fuelType,

        @Schema(description = "Filter by vehicle ID.",
                example = "25",
                nullable = true)
        Long vehicleId,

        @Schema(description = "Filter by vehicle license plate. Partial matches are supported.",
                example = "ABC 123",
                nullable = true)
        String vehicleLicensePlate,

        @Schema(description = "Filter by project area ID.",
                example = "10",
                nullable = true)
        Long projectAreaId,

        @Schema(description = "Filter by project area name. Partial matches are supported.",
                example = "Operations",
                nullable = true)
        String projectAreaName,

        @Schema(description = "Filter by gas station ID.",
                example = "5",
                nullable = true)
        Long gasStationId,

        @Schema(description = "Generic search across vehicle license plate and fuel type (case-insensitive partial match).",
                example = "NAFTA",
                nullable = true)
        String search,

        @Schema(description = "Filter by gas station name. Partial matches are supported.",
                example = "YPF Centro",
                nullable = true)
        String gasStationName,

        @Schema(description = "Filter fuel loads with total amount >= this value.",
                example = "1000.00",
                nullable = true)
        BigDecimal totalAmountMin,

        @Schema(description = "Filter fuel loads with total amount <= this value.",
                example = "50000.00",
                nullable = true)
        BigDecimal totalAmountMax,

        @Schema(description = "Filter by linked transactional document ID.",
                example = "7",
                nullable = true)
        Long transactionalDocumentId,

        @Schema(description = "Filter by total amount (partial string match). Typing '50' matches 10050.00, 5000.00, etc.",
                example = "50",
                nullable = true)
        String totalAmountLike,

        @Schema(description = "If true, only return fuel loads not yet linked to any transactional document.",
                example = "true",
                nullable = true)
        Boolean unlinked
) {}
