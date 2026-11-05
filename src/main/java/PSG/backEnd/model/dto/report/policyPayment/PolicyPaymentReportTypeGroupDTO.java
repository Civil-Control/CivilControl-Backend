package PSG.backEnd.model.dto.report.policyPayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Schema(description = "Grouping of policy payments by policy type (layer 1)")
public record PolicyPaymentReportTypeGroupDTO(

    @Schema(description = "Policy type display name", example = "automotor")
    String policyTypeName,

    @Schema(description = "Policy type enum key", example = "AUTOMOTOR")
    String policyTypeKey,

    @Schema(description = "Subtotal paid amount for this type")
    BigDecimal subtotalPaid,

    @Schema(description = "Subtotal expected amount for this type")
    BigDecimal subtotalExpected,

    @Schema(description = "Subtotal difference for this type")
    BigDecimal subtotalDifference,

    @Schema(description = "Number of payments in this type")
    int paymentCount,

    @Schema(description = "Number of distinct policies in this type")
    int policyCount,

    @Schema(description = "Policy groups within this type")
    List<PolicyPaymentReportPolicyGroupDTO> policyGroups
) {}
