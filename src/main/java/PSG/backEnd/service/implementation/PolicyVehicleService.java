package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.insurance.DuplicateVehicleInPolicyException;
import PSG.backEnd.exception.insurance.InsurancePolicyNotFoundException;
import PSG.backEnd.exception.insurance.PolicyVehicleNotFoundException;
import PSG.backEnd.model.dto.insurance.InsurancePolicyPaymentResponseDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleResponseDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleWithPaymentsDTO;
import PSG.backEnd.model.entity.insurance.AutoPolicy;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.entity.insurance.InsurancePolicyPaymentDetail;
import PSG.backEnd.model.entity.insurance.PolicyVehicle;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.mapper.PolicyVehicleMapper;
import PSG.backEnd.repository.InsurancePolicyPaymentDetailRepository;
import PSG.backEnd.repository.PolicyVehicleRepository;
import PSG.backEnd.repository.AutoPolicyRepository;
import PSG.backEnd.service.port.IInsurancePolicyService;
import PSG.backEnd.service.port.IPolicyVehicleService;
import PSG.backEnd.service.port.IVehicleService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PolicyVehicleService implements IPolicyVehicleService {

    private final PolicyVehicleRepository policyVehicleRepository;
    private final PolicyVehicleMapper policyVehicleMapper;
    private final IVehicleService vehicleService;
    private final AutoPolicyRepository autoPolicyRepository;
    private final IInsurancePolicyService insurancePolicyService;
    private final InsurancePolicyPaymentDetailRepository paymentDetailRepository;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public PolicyVehicleResponseDTO createPolicyVehicle(PolicyVehicleDTO policyVehicleDTO) {
        throw new UnsupportedOperationException(messageSourceHelper.getMessage("policyVehicle.unsupportedOperation"));
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyVehicleResponseDTO getPolicyVehicleById(Long id) {
        PolicyVehicle policyVehicle = getEntityById(id);
        return policyVehicleMapper.toResponseDto(policyVehicle);
    }

    @Override
    public PolicyVehicleResponseDTO updatePolicyVehicle(Long id, PolicyVehicleDTO policyVehicleDTO) {
        PolicyVehicle existingPolicyVehicle = getEntityById(id);
        Long autoPolicyId = existingPolicyVehicle.getAutoPolicy().getId();

        validateBusinessRulesInternal(policyVehicleDTO, autoPolicyId);

        // Validate vehicle change only if vehicleId is provided and different
        if (policyVehicleDTO.vehicleId() != null &&
            !existingPolicyVehicle.getVehicle().getId().equals(policyVehicleDTO.vehicleId())) {
            validateVehicleNotInPolicy(policyVehicleDTO.vehicleId(), autoPolicyId, id);

            // If vehicleId is changing, fetch and set the new Vehicle entity
            Vehicle newVehicle = vehicleService.getEntityById(policyVehicleDTO.vehicleId());
            existingPolicyVehicle.setVehicle(newVehicle);
        }

        // Update only allowed fields without touching relationships
        policyVehicleMapper.partialUpdate(policyVehicleDTO, existingPolicyVehicle);

        PolicyVehicle updatedPolicyVehicle = policyVehicleRepository.save(existingPolicyVehicle);
        return policyVehicleMapper.toResponseDto(updatedPolicyVehicle);
    }

    @Override
    public void deletePolicyVehicle(Long id) {
        PolicyVehicle policyVehicle = getEntityById(id);
        policyVehicle.setDeleted(true);
        policyVehicleRepository.save(policyVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyVehicleResponseDTO> getAllPolicyVehicles(
            Long vehicleId, Long autoPolicyId, String licensePlate, String vehicleBrand,
            String vehicleModel, String policyNumber, LocalDate effectiveFromStart,
            LocalDate effectiveFromEnd, LocalDate effectiveToStart, LocalDate effectiveToEnd,
            Boolean isCancelled, Pageable pageable) {

        Page<PolicyVehicle> policyVehicles = policyVehicleRepository.findAllWithFilters(
                vehicleId, autoPolicyId, licensePlate, vehicleBrand, vehicleModel,
                policyNumber, effectiveFromStart, effectiveFromEnd, effectiveToStart,
                effectiveToEnd, isCancelled, pageable
        );

        return policyVehicles.map(policyVehicleMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyVehicle getEntityById(Long id) {
        return policyVehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new PolicyVehicleNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return policyVehicleRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyVehicleResponseDTO> getByAutoPolicyId(Long autoPolicyId) {
        List<PolicyVehicle> policyVehicles = policyVehicleRepository.findByAutoPolicyId(autoPolicyId);
        return policyVehicles.stream()
                .map(policyVehicleMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyVehicleWithPaymentsDTO> getByVehicleId(Long vehicleId) {
        return policyVehicleRepository.findByVehicleIdAndDeletedFalse(vehicleId).stream()
                .map(pv -> {
                    InsurancePolicy policy = pv.getAutoPolicy().getInsurancePolicy();
                    List<InsurancePolicyPaymentResponseDTO> payments =
                            paymentDetailRepository.findByPolicyIdWithPaymentDetails(policy.getId())
                                    .stream()
                                    .map(this::toPaymentResponseDTO)
                                    .toList();
                    return new PolicyVehicleWithPaymentsDTO(
                            pv.getId(),
                            pv.getVehicle().getId(),
                            pv.getVehicle().getLicensePlate(),
                            pv.getVehicle().getBrand(),
                            pv.getVehicle().getModel(),
                            pv.getVehicle().getYear(),
                            pv.getAutoPolicy().getId(),
                            policy.getId(),
                            policy.getPolicyNumber(),
                            pv.getSumInsured(),
                            pv.getEffectiveFrom(),
                            pv.getEffectiveTo(),
                            pv.getCancellationDate(),
                            pv.getNumberOfInstallments(),
                            pv.getPremioTotal(),
                            pv.getPremioMensual(),
                            payments
                    );
                })
                .toList();
    }

    private InsurancePolicyPaymentResponseDTO toPaymentResponseDTO(InsurancePolicyPaymentDetail detail) {
        PaymentDetails pd = detail.getPaymentDetails();
        InsurancePolicy policy = detail.getInsurancePolicy();
        String paymentMethod;
        String bankName = null;
        String transactionNumber = null;
        String checkNumber = null;
        LocalDate checkDueDate = null;
        if (pd.getCashPayment() != null) {
            paymentMethod = "CASH";
        } else if (pd.getTransferPayment() != null) {
            paymentMethod = "TRANSFER";
            bankName = pd.getTransferPayment().getBankAccount() != null ? pd.getTransferPayment().getBankAccount().getBankName() : null;
            transactionNumber = pd.getTransferPayment().getTransactionNumber();
        } else if (pd.getCheckPayment() != null) {
            paymentMethod = "CHECK";
            bankName = pd.getCheckPayment().getBankAccount() != null ? pd.getCheckPayment().getBankAccount().getBankName() : null;
            checkNumber = pd.getCheckPayment().getCheckNumber();
            checkDueDate = pd.getCheckPayment().getDueDate();
        } else {
            paymentMethod = "UNKNOWN";
        }
        return new InsurancePolicyPaymentResponseDTO(
                detail.getId(), pd.getId(), policy.getId(), policy.getPolicyNumber(),
                pd.getPaymentDate(), pd.getAmount(), detail.getPeriodFrom(), detail.getPeriodTo(),
                pd.getComment(), paymentMethod, bankName, transactionNumber, checkNumber, checkDueDate
        );
    }

    @Override
    public void validateVehicleNotInPolicy(Long vehicleId, Long autoPolicyId, Long excludeId) {
        boolean exists = policyVehicleRepository.existsByVehicleIdAndAutoPolicyId(vehicleId, autoPolicyId);

        if (exists) {
            if (excludeId != null) {
                List<PolicyVehicle> existing = policyVehicleRepository.findByVehicleId(vehicleId);
                boolean isDuplicate = existing.stream()
                        .anyMatch(pv -> pv.getAutoPolicy().getId().equals(autoPolicyId) &&
                                       !pv.getId().equals(excludeId) &&
                                       !pv.getDeleted());
                if (isDuplicate) {
                    throw new DuplicateVehicleInPolicyException(vehicleId, autoPolicyId);
                }
            } else {
                throw new DuplicateVehicleInPolicyException(vehicleId, autoPolicyId);
            }
        }
    }

    @Override
    public PolicyVehicleResponseDTO addVehicleToInsurancePolicy(Long insurancePolicyId, PolicyVehicleDTO policyVehicleDTO) {
        if (!insurancePolicyService.existsById(insurancePolicyId)) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("policyVehicle.insurancePolicy.notFound", insurancePolicyId));
        }

        AutoPolicy autoPolicy = getOrCreateAutoPolicy(insurancePolicyId);

        // Check if there's a deleted PolicyVehicle with the same vehicle and autoPolicy to reactivate it
        Optional<PolicyVehicle> deletedPolicyVehicle = policyVehicleRepository
                .findByVehicleIdAndAutoPolicyIdAndDeletedTrue(policyVehicleDTO.vehicleId(), autoPolicy.getId());

        if (deletedPolicyVehicle.isPresent()) {
            // Reactivate existing PolicyVehicle
            return reactivatePolicyVehicle(deletedPolicyVehicle.get(), policyVehicleDTO);
        }

        return createPolicyVehicleInternal(policyVehicleDTO, autoPolicy.getId());
    }

    private PolicyVehicleResponseDTO reactivatePolicyVehicle(PolicyVehicle deletedPolicyVehicle, PolicyVehicleDTO newData) {
        // Validate business rules with new data
        validateBusinessRulesInternal(newData, deletedPolicyVehicle.getAutoPolicy().getId());

        // Debug log - verify we have the original ID
        System.out.println("Reactivating policy vehicle with ID: " + deletedPolicyVehicle.getId());

        // Update the deleted PolicyVehicle with new data
        Long originalId = deletedPolicyVehicle.getId();

        policyVehicleMapper.partialUpdate(newData, deletedPolicyVehicle);

        // Ensure the ID wasn't lost
        deletedPolicyVehicle.setId(originalId);

        // Reactivar el PolicyVehicle
        deletedPolicyVehicle.setDeleted(false);

        // Log para debug - verificar que seguimos teniendo el ID original
        System.out.println("About to save policy vehicle with ID: " + deletedPolicyVehicle.getId());

        // Guardar el PolicyVehicle reactivado (debe actualizar, no crear nuevo registro)
        PolicyVehicle reactivatedPolicyVehicle = policyVehicleRepository.save(deletedPolicyVehicle);

        // Log para debug - verificar el ID después de save
        System.out.println("Saved policy vehicle with ID: " + reactivatedPolicyVehicle.getId());

        return policyVehicleMapper.toResponseDto(reactivatedPolicyVehicle);
    }

    private PolicyVehicleResponseDTO createPolicyVehicleInternal(PolicyVehicleDTO policyVehicleDTO, Long autoPolicyId) {
        validateBusinessRulesInternal(policyVehicleDTO, autoPolicyId);
        validateVehicleNotInPolicy(policyVehicleDTO.vehicleId(), autoPolicyId, null);

        PolicyVehicle policyVehicle = policyVehicleMapper.toEntity(policyVehicleDTO);
        policyVehicle.setAutoPolicy(AutoPolicy.builder().id(autoPolicyId).build());

        PolicyVehicle savedPolicyVehicle = policyVehicleRepository.save(policyVehicle);
        return policyVehicleMapper.toResponseDto(savedPolicyVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyVehicleResponseDTO> getByInsurancePolicyId(Long insurancePolicyId) {
        Optional<AutoPolicy> autoPolicy = autoPolicyRepository.findByInsurancePolicyId(insurancePolicyId);

        if (autoPolicy.isEmpty()) {
            return List.of();
        }

        return getByAutoPolicyId(autoPolicy.get().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyVehicleResponseDTO> getAllPolicyVehiclesByInsurancePolicy(
            Long vehicleId, Long insurancePolicyId, String licensePlate, String vehicleBrand,
            String vehicleModel, String policyNumber, LocalDate effectiveFromStart,
            LocalDate effectiveFromEnd, LocalDate effectiveToStart, LocalDate effectiveToEnd,
            Boolean isCancelled, Pageable pageable) {

        Long autoPolicyId = null;
        if (insurancePolicyId != null) {

            if (!insurancePolicyService.existsById(insurancePolicyId)) {
                throw new InsurancePolicyNotFoundException(insurancePolicyId);
            }

            Optional<AutoPolicy> autoPolicy = autoPolicyRepository.findByInsurancePolicyId(insurancePolicyId);
            autoPolicyId = autoPolicy.map(AutoPolicy::getId).orElse(null);
        }

        return getAllPolicyVehicles(vehicleId, autoPolicyId, licensePlate, vehicleBrand,
                vehicleModel, policyNumber, effectiveFromStart, effectiveFromEnd,
                effectiveToStart, effectiveToEnd, isCancelled, pageable);
    }

    private AutoPolicy getOrCreateAutoPolicy(Long insurancePolicyId) {
        Optional<AutoPolicy> existingAutoPolicy = autoPolicyRepository.findByInsurancePolicyId(insurancePolicyId);

        if (existingAutoPolicy.isPresent()) {
            return existingAutoPolicy.get();
        }

        AutoPolicy newAutoPolicy = AutoPolicy.builder()
                .insurancePolicy(insurancePolicyService.getEntityById(insurancePolicyId))
                .deleted(false)
                .build();

        return autoPolicyRepository.save(newAutoPolicy);
    }

    private void validateBusinessRulesInternal(PolicyVehicleDTO dto, Long autoPolicyId) {
        // Solo validar vehicleId si está presente (para PATCH puede ser null)
        if (dto.vehicleId() != null && !vehicleService.existsById(dto.vehicleId())) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("policyVehicle.vehicle.notFound", dto.vehicleId()));
        }

        if (!autoPolicyRepository.existsByIdAndDeletedFalse(autoPolicyId)) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("policyVehicle.autoPolicy.notFound", autoPolicyId));
        }

        if (dto.effectiveFrom() != null && dto.effectiveTo() != null) {
            if (dto.effectiveFrom().isAfter(dto.effectiveTo())) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage("policyVehicle.effectiveFrom.beforeEffectiveTo"));
            }
        }

        if (dto.cancellationDate() != null && dto.effectiveFrom() != null) {
            if (dto.cancellationDate().isBefore(dto.effectiveFrom())) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage("policyVehicle.cancellationDate.beforeEffectiveFrom"));
            }
        }

        if (dto.sumInsured() != null && dto.sumInsured().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("policyVehicle.sumInsured.positive"));
        }

        if (dto.numberOfInstallments() != null && (dto.numberOfInstallments() < 1 || dto.numberOfInstallments() > 12)) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("policyVehicle.numberOfInstallments.range"));
        }
    }
}
