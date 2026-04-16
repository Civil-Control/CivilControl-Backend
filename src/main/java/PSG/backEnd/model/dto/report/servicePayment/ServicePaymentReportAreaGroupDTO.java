package PSG.backEnd.model.dto.report.servicePayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Grouping of service payments by project area (layer 1)")
public record ServicePaymentReportAreaGroupDTO(

    @Schema(description = "Project area ID (null if no area)")
    Long projectAreaId,

    @Schema(description = "Project area name")
    String projectAreaName,

    @Schema(description = "Project area color")
    String projectAreaColor,

    @Schema(description = "Subtotal amount for this area")
    BigDecimal subtotalAmount,

    @Schema(description = "Number of payments in this area")
    int paymentCount,

    @Schema(description = "Subtotals by service type")
    Map<String, BigDecimal> subtotalsByServiceType,

    @Schema(description = "Building groups within this area")
    List<ServicePaymentReportBuildingGroupDTO> buildingGroups
) {}
