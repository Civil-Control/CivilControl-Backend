package PSG.backEnd.model.dto.report.servicePayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Root DTO for the service payment report")
public record ServicePaymentReportDTO(

    @Schema(description = "Filters used to generate this report")
    ServicePaymentReportFilterDTO filters,

    @Schema(description = "Area groups (layer 1)")
    List<ServicePaymentReportAreaGroupDTO> areaGroups,

    @Schema(description = "Grand total amount")
    BigDecimal totalAmount,

    @Schema(description = "Total number of payments")
    int totalCount,

    @Schema(description = "Totals by service type")
    Map<String, BigDecimal> totalsByServiceType,

    @Schema(description = "Totals by subject type (BUILDING, VEHICLE)")
    Map<String, BigDecimal> totalsBySubjectType,

    @Schema(description = "Report generation timestamp")
    LocalDateTime generatedAt,

    @Schema(description = "Report name")
    String reportName,

    @Schema(description = "Human-readable period description")
    String periodDescription
) {}
