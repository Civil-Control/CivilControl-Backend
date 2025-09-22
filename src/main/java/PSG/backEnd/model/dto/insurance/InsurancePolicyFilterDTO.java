package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.enums.vehicle.PaymentFrequency;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.enums.vehicle.PolicyType;

import java.time.LocalDate;

public record InsurancePolicyFilterDTO(
        String policyNumber,
        String termNumber,
        PolicyType policyType,
        PolicyStatus policyStatus,
        PaymentFrequency paymentFrequency,
        LocalDate issueDateFrom,
        LocalDate issueDateTo,
        LocalDate effectiveFromStart,
        LocalDate effectiveFromEnd,
        LocalDate effectiveToStart,
        LocalDate effectiveToEnd,
        Boolean isCancelled
) {}
