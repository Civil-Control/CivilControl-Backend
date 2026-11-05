package PSG.backEnd.model.dto.report.fuelLoad;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Root DTO for the fuel load report")
public record FuelLoadReportDTO(

    @Schema(description = "Filters used to generate this report")
    FuelLoadReportFilterDTO filters,

    @Schema(description = "Area groups (layer 1 when not grouped by gas station)")
    List<FuelLoadReportAreaGroupDTO> areaGroups,

    @Schema(description = "Gas station groups (layer 1 when grouped by gas station)")
    List<FuelLoadReportGasStationGroupDTO> gasStationGroups,

    @Schema(description = "Grand total amount")
    BigDecimal totalAmount,

    @Schema(description = "Grand total liters")
    BigDecimal totalLiters,

    @Schema(description = "Total number of loads")
    int totalCount,

    @Schema(description = "Totals by fuel type (amount)")
    Map<String, BigDecimal> totalsByFuelType,

    @Schema(description = "Liters by fuel type")
    Map<String, BigDecimal> litersByFuelType,

    @Schema(description = "Totals by gas station (amount)")
    Map<String, BigDecimal> totalsByGasStation,

    @Schema(description = "Report generation timestamp")
    LocalDateTime generatedAt,

    @Schema(description = "Report name")
    String reportName,

    @Schema(description = "Human-readable period description")
    String periodDescription
) {}
