package PSG.backEnd.model.dto.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InsurancePolicyResponseDTO(
        Long id,
        String policyNumber,
        String termNumber,
        String endorsementSecuence,
        String policyType,
        String policyStatus,
        String paymentFrequency,
        BigDecimal sumInsured,
        LocalDate issueDate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        LocalDate cancellationDate,
        Integer numberOfInstallments
) {}
