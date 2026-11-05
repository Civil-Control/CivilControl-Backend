package PSG.backEnd.model.dto.report.fuelLoad;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Grouping of fuel loads by project area (layer 2)")
public record FuelLoadReportAreaGroupDTO(

    @Schema(description = "Project area ID (null if no area)")
    Long projectAreaId,

    @Schema(description = "Project area name")
    String projectAreaName,

    @Schema(description = "Project area color")
    String projectAreaColor,

    @Schema(description = "Subtotal amount for this area")
    BigDecimal subtotalAmount,

    @Schema(description = "Total liters for this area")
    BigDecimal subtotalLiters,

    @Schema(description = "Number of loads in this area")
    int loadCount,

    @Schema(description = "Subtotals by fuel type")
    Map<String, BigDecimal> subtotalsByFuelType,

    @Schema(description = "Liters by fuel type")
    Map<String, BigDecimal> litersByFuelType,

    @Schema(description = "Vehicle groups within this area")
    List<FuelLoadReportVehicleGroupDTO> vehicleGroups
) {}
