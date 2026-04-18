package PSG.backEnd.model.dto.report.policyPayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "Root DTO for the policy payment report")
public record PolicyPaymentReportDTO(

    @Schema(description = "Filters used to generate this report")
    PolicyPaymentReportFilterDTO filters,

    @Schema(description = "Type groups (layer 1)")
    List<PolicyPaymentReportTypeGroupDTO> typeGroups,

    @Schema(description = "Grand total paid amount")
    BigDecimal totalPaidAmount,

    @Schema(description = "Grand total expected amount (sum of premioMensual × paymentCount per policy)")
    BigDecimal totalExpectedAmount,

    @Schema(description = "Grand total difference (totalPaidAmount - totalExpectedAmount)")
    BigDecimal totalDifference,

    @Schema(description = "Total number of payments")
    int totalPaymentCount,

    @Schema(description = "Total number of distinct policies with payments")
    int totalPolicyCount,

    @Schema(description = "Report generation timestamp")
    LocalDateTime generatedAt,

    @Schema(description = "Report name")
    String reportName,

    @Schema(description = "Human-readable period description")
    String periodDescription
) {}
