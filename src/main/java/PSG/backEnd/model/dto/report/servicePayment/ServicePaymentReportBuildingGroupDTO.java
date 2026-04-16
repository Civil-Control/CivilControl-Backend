package PSG.backEnd.model.dto.report.servicePayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Grouping of service payments by building within an area")
public record ServicePaymentReportBuildingGroupDTO(

    @Schema(description = "Building ID (null if no building assigned)")
    Long buildingId,

    @Schema(description = "Building name ('Sin Edificio asignado' if null)")
    String buildingName,

    @Schema(description = "Total amount for this building group")
    BigDecimal totalAmount,

    @Schema(description = "Number of payments in this building group")
    int paymentCount,

    @Schema(description = "Subtotals by service type")
    Map<String, BigDecimal> subtotalsByServiceType,

    @Schema(description = "Individual payments in this building group")
    List<ServicePaymentReportItemDTO> payments
) {}
