package PSG.backEnd.model.dto.report.fuelLoad;

import PSG.backEnd.model.enums.vehicle.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filters for the fuel load report")
public record FuelLoadReportFilterDTO(

    @Schema(description = "Start date (inclusive)")
    LocalDate startDate,

    @Schema(description = "End date (inclusive)")
    LocalDate endDate,

    @Schema(description = "Filter by project area IDs")
    List<Long> projectAreaIds,

    @Schema(description = "Filter by fuel type")
    FuelType fuelType,

    @Schema(description = "Filter by gas station ID")
    Long gasStationId,

    @Schema(description = "Filter by vehicle ID")
    Long vehicleId,

    @Schema(description = "Filter by vehicle type ID")
    Long vehicleTypeId,

    @Schema(description = "Minimum amount (inclusive)")
    BigDecimal minAmount,

    @Schema(description = "Maximum amount (inclusive)")
    BigDecimal maxAmount
) {}
