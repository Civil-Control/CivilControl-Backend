package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.RepairOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Filter criteria for querying repair orders.")
public record RepairOrderFilterDTO(

        @Schema(description = "Filter orders from this date (inclusive).", example = "2024-01-01", nullable = true)
        LocalDate dateFrom,

        @Schema(description = "Filter orders up to this date (inclusive).", example = "2024-12-31", nullable = true)
        LocalDate dateTo,

        @Schema(description = "Filter by vehicle ID.", example = "15", nullable = true)
        Long vehicleId,

        @Schema(description = "Filter by vehicle license plate (partial match).", example = "ABC", nullable = true)
        String vehicleLicensePlate,

        @Schema(description = "Filter by repair order status.", nullable = true)
        RepairOrderStatus status,

        @Schema(description = "Free-text search on description or operator name.", nullable = true)
        String search
) {}
