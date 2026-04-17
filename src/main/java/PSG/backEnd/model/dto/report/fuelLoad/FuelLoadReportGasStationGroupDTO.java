package PSG.backEnd.model.dto.report.fuelLoad;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Grouping of fuel loads by gas station (layer 1)")
public record FuelLoadReportGasStationGroupDTO(

    @Schema(description = "Gas station ID")
    Long gasStationId,

    @Schema(description = "Gas station name (supplier legal name)")
    String gasStationName,

    @Schema(description = "Subtotal amount for this gas station")
    BigDecimal subtotalAmount,

    @Schema(description = "Total liters for this gas station")
    BigDecimal subtotalLiters,

    @Schema(description = "Number of loads in this gas station")
    int loadCount,

    @Schema(description = "Subtotals by fuel type")
    Map<String, BigDecimal> subtotalsByFuelType,

    @Schema(description = "Liters by fuel type")
    Map<String, BigDecimal> litersByFuelType,

    @Schema(description = "Area groups within this gas station")
    List<FuelLoadReportAreaGroupDTO> areaGroups
) {}
