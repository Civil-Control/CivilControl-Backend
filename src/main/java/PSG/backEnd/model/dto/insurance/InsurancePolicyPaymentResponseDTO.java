package PSG.backEnd.model.dto.insurance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response DTO for a policy payment linked to the PaymentDetails system.")
public record InsurancePolicyPaymentResponseDTO(
        Long id,
        Long paymentDetailsId,
        Long insurancePolicyId,
        String policyNumber,
        LocalDate paymentDate,
        BigDecimal amount,
        LocalDate periodFrom,
        LocalDate periodTo,
        String notes,
        String paymentMethod,
        String bankName,
        String transactionNumber,
        String checkNumber,
        LocalDate checkDueDate
) {}
