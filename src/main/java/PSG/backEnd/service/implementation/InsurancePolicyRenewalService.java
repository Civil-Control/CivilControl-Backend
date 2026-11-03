package PSG.backEnd.service.implementation;

import PSG.backEnd.model.dto.insurance.InsurancePolicyRenewalDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyRenewalDraftDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyResponseDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleDTO;
import PSG.backEnd.model.dto.insurance.RenewalDraftVehicleDTO;
import PSG.backEnd.model.dto.insurance.RenewalFleetVehicleDTO;
import PSG.backEnd.model.dto.insurance.RenewalVehicleDTO;
import PSG.backEnd.model.entity.insurance.AutoPolicy;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.entity.insurance.PolicyVehicle;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.mapper.InsurancePolicyMapper;
import PSG.backEnd.repository.AutoPolicyRepository;
import PSG.backEnd.repository.InsurancePolicyRepository;
import PSG.backEnd.repository.PolicyVehicleRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.service.port.IInsurancePolicyRenewalService;
import PSG.backEnd.service.port.IInsurancePolicyService;
import PSG.backEnd.service.port.IPolicyVehicleService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class InsurancePolicyRenewalService implements IInsurancePolicyRenewalService {

    private final InsurancePolicyRepository insurancePolicyRepository;
    private final AutoPolicyRepository autoPolicyRepository;
    private final PolicyVehicleRepository policyVehicleRepository;
    private final VehicleRepository vehicleRepository;
    private final IInsurancePolicyService insurancePolicyService;
    private final IPolicyVehicleService policyVehicleService;
    private final InsurancePolicyMapper insurancePolicyMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional(readOnly = true)
    public Optional<InsurancePolicyResponseDTO> findDefaultBasePolicy() {
        return insurancePolicyRepository.findFirstByDeletedFalseOrderByEffectiveToDescIdDesc()
                .map(insurancePolicyMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public InsurancePolicyRenewalDraftDTO buildDraft(Long sourcePolicyId) {
        InsurancePolicy source = insurancePolicyService.getEntityById(sourcePolicyId);

        LocalDate effectiveFrom = source.getEffectiveTo();
        LocalDate effectiveTo = effectiveFrom.plus(Period.between(source.getEffectiveFrom(), source.getEffectiveTo()));

        Long renewedBy = insurancePolicyRepository.findFirstByRenewedFromPolicyIdAndDeletedFalse(sourcePolicyId)
                .map(InsurancePolicy::getId)
                .orElse(null);

        Map<Long, String> coveredElsewhere = coverageByVehicle(effectiveFrom, sourcePolicyId);

        List<PolicyVehicle> sourceVehicles = activeVehiclesOf(sourcePolicyId);
        Set<Long> carriedVehicleIds = new HashSet<>();
        List<RenewalDraftVehicleDTO> vehicles = sourceVehicles.stream()
                .peek(pv -> carriedVehicleIds.add(pv.getVehicle().getId()))
                .map(pv -> new RenewalDraftVehicleDTO(
                        pv.getVehicle().getId(),
                        pv.getVehicle().getLicensePlate(),
                        pv.getVehicle().getBrand(),
                        pv.getVehicle().getModel(),
                        pv.getVehicle().getYear(),
                        pv.getSumInsured(),
                        pv.getPremioTotal(),
                        pv.getPremioMensual(),
                        pv.getNumberOfInstallments(),
                        coveredElsewhere.get(pv.getVehicle().getId())))
                .toList();

        List<RenewalFleetVehicleDTO> uncovered = vehicleRepository.findByDeletedFalse().stream()
                .filter(Vehicle::isActive)
                .filter(v -> !carriedVehicleIds.contains(v.getId()))
                .filter(v -> !coveredElsewhere.containsKey(v.getId()))
                .sorted(Comparator.comparing(v -> String.valueOf(v.getLicensePlate())))
                .map(v -> new RenewalFleetVehicleDTO(v.getId(), v.getLicensePlate(), v.getBrand(), v.getModel(), v.getYear()))
                .toList();

        return new InsurancePolicyRenewalDraftDTO(
                source.getId(),
                source.getPolicyNumber(),
                source.getEffectiveFrom(),
                source.getEffectiveTo(),
                renewedBy,
                source.getPolicyNumber(),
                nextTerm(source.getTermNumber()),
                source.getPolicyType(),
                PolicyStatus.ACTIVO,
                source.getPaymentFrequency(),
                source.getNumberOfInstallments(),
                source.getPeriodicDueDay(),
                source.getDueAtStartOfPeriod(),
                source.getSupplier() != null ? source.getSupplier().getId() : null,
                source.getSupplier() != null ? source.getSupplier().getLegalName() : null,
                LocalDate.now(),
                effectiveFrom,
                effectiveTo,
                source.getSumInsured(),
                source.getPremioTotal(),
                source.getPremioMensual(),
                vehicles,
                uncovered);
    }

    @Override
    public InsurancePolicyResponseDTO renew(Long sourcePolicyId, InsurancePolicyRenewalDTO renewalDTO) {
        InsurancePolicy source = insurancePolicyService.getEntityById(sourcePolicyId);

        insurancePolicyRepository.findFirstByRenewedFromPolicyIdAndDeletedFalse(sourcePolicyId).ifPresent(existing -> {
            throw new IllegalArgumentException(messageSourceHelper.getMessage(
                    "insurancePolicy.renew.alreadyRenewed", label(source), label(existing)));
        });

        Set<Long> seenVehicleIds = new HashSet<>();
        for (RenewalVehicleDTO vehicle : renewalDTO.vehicles()) {
            if (!seenVehicleIds.add(vehicle.vehicleId())) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage(
                        "insurancePolicy.renew.duplicateVehicle", vehicle.vehicleId()));
            }
        }

        InsurancePolicy renewal = insurancePolicyService.createRenewalPolicy(renewalDTO.policy(), sourcePolicyId);

        for (RenewalVehicleDTO vehicle : renewalDTO.vehicles()) {
            policyVehicleService.addVehicleToInsurancePolicy(renewal.getId(), new PolicyVehicleDTO(
                    vehicle.vehicleId(),
                    vehicle.sumInsured(),
                    vehicle.effectiveFrom() != null ? vehicle.effectiveFrom() : renewal.getEffectiveFrom(),
                    vehicle.effectiveTo() != null ? vehicle.effectiveTo() : renewal.getEffectiveTo(),
                    null,
                    vehicle.numberOfInstallments(),
                    vehicle.premioTotal(),
                    vehicle.premioMensual()));
        }

        return insurancePolicyMapper.toResponseDto(renewal);
    }

    private List<PolicyVehicle> activeVehiclesOf(Long policyId) {
        Optional<AutoPolicy> autoPolicy = autoPolicyRepository.findByInsurancePolicyId(policyId);
        if (autoPolicy.isEmpty()) {
            return List.of();
        }
        return policyVehicleRepository.findByAutoPolicyId(autoPolicy.get().getId()).stream()
                .filter(pv -> !Boolean.TRUE.equals(pv.getDeleted()))
                .filter(pv -> pv.getCancellationDate() == null)
                .filter(pv -> pv.getVehicle() != null && !pv.getVehicle().isDeleted() && pv.getVehicle().isActive())
                .sorted(Comparator.comparing(pv -> String.valueOf(pv.getVehicle().getLicensePlate())))
                .toList();
    }

    private Map<Long, String> coverageByVehicle(LocalDate from, Long excludePolicyId) {
        Map<Long, String> coverage = new HashMap<>();
        for (PolicyVehicle pv : policyVehicleRepository.findActiveCoverageExcludingPolicy(from, excludePolicyId)) {
            coverage.putIfAbsent(pv.getVehicle().getId(), pv.getAutoPolicy().getInsurancePolicy().getPolicyNumber());
        }
        return coverage;
    }

    private static String label(InsurancePolicy policy) {
        String term = policy.getTermNumber();
        return term == null || term.isBlank()
                ? policy.getPolicyNumber()
                : policy.getPolicyNumber() + " (renovacion " + term + ")";
    }

    private static String nextTerm(String termNumber) {
        if (termNumber == null || termNumber.isBlank()) {
            return "1";
        }
        try {
            return String.valueOf(Long.parseLong(termNumber.trim()) + 1);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
