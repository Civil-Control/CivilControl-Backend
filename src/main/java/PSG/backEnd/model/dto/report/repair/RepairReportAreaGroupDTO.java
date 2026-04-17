package PSG.backEnd.model.dto.report.repair;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Schema(description = "Grouping of repairs by project area (layer 1)")
public record RepairReportAreaGroupDTO(

    @Schema(description = "Project area ID (null if no area)")
    Long projectAreaId,

    @Schema(description = "Project area name")
    String projectAreaName,

    @Schema(description = "Project area color")
    String projectAreaColor,

    @Schema(description = "Subtotal amount for this area")
    BigDecimal subtotalAmount,

    @Schema(description = "Number of repairs in this area")
    int repairCount,

    @Schema(description = "Material subtotal for this area")
    BigDecimal materialSubtotal,

    @Schema(description = "Labor subtotal for this area")
    BigDecimal laborSubtotal,

    @Schema(description = "Vehicle groups within this area")
    List<RepairReportVehicleGroupDTO> vehicleGroups
) {}
