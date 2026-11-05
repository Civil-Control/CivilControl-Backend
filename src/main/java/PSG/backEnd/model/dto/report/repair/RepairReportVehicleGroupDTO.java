package PSG.backEnd.model.dto.report.repair;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Schema(description = "Grouping of repairs by vehicle within an area (layer 2)")
public record RepairReportVehicleGroupDTO(

    @Schema(description = "Vehicle ID")
    Long vehicleId,

    @Schema(description = "Vehicle license plate")
    String vehicleLicensePlate,

    @Schema(description = "Vehicle brand")
    String vehicleBrand,

    @Schema(description = "Vehicle model")
    String vehicleModel,

    @Schema(description = "Total amount for this vehicle group")
    BigDecimal totalAmount,

    @Schema(description = "Number of repairs in this vehicle group")
    int repairCount,

    @Schema(description = "Material subtotal for this vehicle group")
    BigDecimal materialSubtotal,

    @Schema(description = "Labor subtotal for this vehicle group")
    BigDecimal laborSubtotal,

    @Schema(description = "Individual repairs in this vehicle group")
    List<RepairReportItemDTO> repairs
) {}
