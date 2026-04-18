package PSG.backEnd.model.dto.report.policyPayment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A single policy payment entry within the report")
public record PolicyPaymentReportPaymentDTO(

    @Schema(description = "Payment ID")
    Long id,

    @Schema(description = "Date of the payment")
    LocalDate paymentDate,

    @Schema(description = "Payment amount")
    BigDecimal amount,

    @Schema(description = "Period start date")
    LocalDate periodFrom,

    @Schema(description = "Period end date")
    LocalDate periodTo,

    @Schema(description = "Payment notes", nullable = true)
    String notes,

    @Schema(description = "Policy number (for flat view)")
    String policyNumber,

    @Schema(description = "Policy type display name (for flat view)")
    String policyTypeName,

    @Schema(description = "Expected monthly premium of the policy")
    BigDecimal premioMensual,

    @Schema(description = "Difference (amount - premioMensual)")
    BigDecimal difference
) {}
