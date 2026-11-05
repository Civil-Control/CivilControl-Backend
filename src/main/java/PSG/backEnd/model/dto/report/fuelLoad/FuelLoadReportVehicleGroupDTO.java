package PSG.backEnd.model.dto.report.fuelLoad;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Grouping of fuel loads by vehicle within an area")
public record FuelLoadReportVehicleGroupDTO(

    @Schema(description = "Vehicle ID (null if no vehicle/bidón)")
    Long vehicleId,

    @Schema(description = "Vehicle display name (license plate or 'Bidón')")
    String vehicleName,

    @Schema(description = "Vehicle description (brand + model)", nullable = true)
    String vehicleDescription,

    @Schema(description = "Total amount for this vehicle group")
    BigDecimal totalAmount,

    @Schema(description = "Total liters for this vehicle group")
    BigDecimal totalLiters,

    @Schema(description = "Number of loads in this vehicle group")
    int loadCount,

    @Schema(description = "Subtotals by fuel type")
    Map<String, BigDecimal> subtotalsByFuelType,

    @Schema(description = "Liters by fuel type")
    Map<String, BigDecimal> litersByFuelType,

    @Schema(description = "Individual fuel loads in this vehicle group")
    List<FuelLoadReportItemDTO> loads
) {}
