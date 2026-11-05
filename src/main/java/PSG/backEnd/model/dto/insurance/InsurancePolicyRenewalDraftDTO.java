package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.enums.vehicle.PaymentFrequency;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.enums.vehicle.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Pre-filled data to renew an insurance policy. Nothing is persisted when this draft is built.")
public record InsurancePolicyRenewalDraftDTO(
        Long sourcePolicyId,
        String sourcePolicyNumber,
        LocalDate sourceEffectiveFrom,
        LocalDate sourceEffectiveTo,
        @Schema(description = "Id of the policy that already renewed the source policy, if any.")
        Long renewedByPolicyId,
        String policyNumber,
        String termNumber,
        PolicyType policyType,
        PolicyStatus policyStatus,
        PaymentFrequency paymentFrequency,
        Integer numberOfInstallments,
        Integer periodicDueDay,
        Boolean dueAtStartOfPeriod,
        Long supplierId,
        String supplierName,
        LocalDate issueDate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal previousSumInsured,
        BigDecimal previousPremioTotal,
        BigDecimal previousPremioMensual,
        List<RenewalDraftVehicleDTO> vehicles,
        List<RenewalFleetVehicleDTO> uncoveredVehicles
) {}
