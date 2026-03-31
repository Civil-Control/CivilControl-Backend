package PSG.backEnd.model.dto.insurance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response DTO for a policy payment record.")
public record PolicyPaymentResponseDTO(
        Long id,
        Long insurancePolicyId,
        String policyNumber,
        LocalDate paymentDate,
        BigDecimal amount,
        LocalDate periodFrom,
        LocalDate periodTo,
        String notes
) {}
