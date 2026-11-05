package PSG.backEnd.model.dto.report.policyPayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Schema(description = "Grouping of policy payments by insurance policy (layer 2)")
public record PolicyPaymentReportPolicyGroupDTO(

    @Schema(description = "Insurance policy ID")
    Long insurancePolicyId,

    @Schema(description = "Policy number")
    String policyNumber,

    @Schema(description = "Policy term number")
    String termNumber,

    @Schema(description = "Policy status display name")
    String policyStatus,

    @Schema(description = "Payment frequency display name")
    String paymentFrequency,

    @Schema(description = "Monthly premium of the policy")
    BigDecimal premioMensual,

    @Schema(description = "Total paid amount for this policy in the period")
    BigDecimal totalPaid,

    @Schema(description = "Expected amount (premioMensual × paymentCount)")
    BigDecimal expectedAmount,

    @Schema(description = "Difference (totalPaid - expectedAmount)")
    BigDecimal difference,

    @Schema(description = "Number of payments for this policy")
    int paymentCount,

    @Schema(description = "Individual payments for this policy")
    List<PolicyPaymentReportPaymentDTO> payments,

    @Schema(description = "Insured vehicles (only for AUTOMOTOR type, null otherwise)", nullable = true)
    List<PolicyPaymentReportVehicleDTO> insuredVehicles
) {}
