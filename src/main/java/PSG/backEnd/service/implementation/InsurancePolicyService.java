package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.insurance.DuplicatePolicyNumberException;
import PSG.backEnd.exception.insurance.InsurancePolicyDataConflictException;
import PSG.backEnd.exception.insurance.InsurancePolicyNotFoundException;
import PSG.backEnd.model.dto.insurance.InsurancePolicyDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyFilterDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyResponseDTO;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.mapper.InsurancePolicyMapper;
import PSG.backEnd.repository.InsurancePolicyRepository;
import PSG.backEnd.service.port.IInsurancePolicyService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class InsurancePolicyService implements IInsurancePolicyService {

    private final InsurancePolicyRepository insurancePolicyRepository;
    private final InsurancePolicyMapper insurancePolicyMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public InsurancePolicyResponseDTO createInsurancePolicy(InsurancePolicyDTO insurancePolicyDTO) {
        // Check if there's a deleted policy with the same number to reactivate it
        Optional<InsurancePolicy> deletedPolicy = insurancePolicyRepository
                .findByPolicyNumberAndDeletedTrue(insurancePolicyDTO.policyNumber());

        if (deletedPolicy.isPresent()) {
            // Reactivate existing policy
            return reactivateInsurancePolicy(deletedPolicy.get(), insurancePolicyDTO);
        }

        // If no deleted policy exists, validate there are no active duplicates
        validatePolicyNumber(insurancePolicyDTO.policyNumber(), null);
        validateBusinessRules(insurancePolicyDTO);

        // Create new policy
        InsurancePolicy insurancePolicy = insurancePolicyMapper.toEntity(insurancePolicyDTO);
        InsurancePolicy savedPolicy = insurancePolicyRepository.save(insurancePolicy);

        return insurancePolicyMapper.toResponseDto(savedPolicy);
    }

    private InsurancePolicyResponseDTO reactivateInsurancePolicy(InsurancePolicy deletedPolicy, InsurancePolicyDTO newData) {
        // Validate business rules with new data
        validateBusinessRules(newData);

        // Debug log - verify we have the original ID
        System.out.println("Reactivating policy with ID: " + deletedPolicy.getId());

        // Update the deleted policy with new data
        // Ensure we keep the original ID
        Long originalId = deletedPolicy.getId();

        insurancePolicyMapper.partialUpdate(newData, deletedPolicy);

        // Ensure the ID wasn't lost
        deletedPolicy.setId(originalId);

        // Reactivate the policy
        deletedPolicy.setDeleted(false);

        // Debug log - verify we still have the original ID
        System.out.println("About to save policy with ID: " + deletedPolicy.getId());

        // Save the reactivated policy (should update, not create a new record)
        InsurancePolicy reactivatedPolicy = insurancePolicyRepository.save(deletedPolicy);

        // Debug log - verify the ID after save
        System.out.println("Saved policy with ID: " + reactivatedPolicy.getId());

        return insurancePolicyMapper.toResponseDto(reactivatedPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public InsurancePolicyResponseDTO getInsurancePolicyById(Long id) {
        InsurancePolicy insurancePolicy = getEntityById(id);
        return insurancePolicyMapper.toResponseDto(insurancePolicy);
    }

    @Override
    public InsurancePolicyResponseDTO updateInsurancePolicy(Long id, InsurancePolicyDTO insurancePolicyDTO) {
        InsurancePolicy existingPolicy = getEntityById(id);

        // Validate policy number if it changed (skip if not provided in partial update)
        if (insurancePolicyDTO.policyNumber() != null
                && !existingPolicy.getPolicyNumber().equals(insurancePolicyDTO.policyNumber())) {
            validatePolicyNumber(insurancePolicyDTO.policyNumber(), id);
        }

        validateBusinessRules(insurancePolicyDTO);

        try {
            insurancePolicyMapper.partialUpdate(insurancePolicyDTO, existingPolicy);

            // Al reactivar (cambiar de CANCELADO a otro estado), limpiar la fecha de cancelación
            if (insurancePolicyDTO.policyStatus() != null
                    && insurancePolicyDTO.policyStatus() != PSG.backEnd.model.enums.vehicle.PolicyStatus.CANCELADO
                    && existingPolicy.getCancellationDate() != null) {
                existingPolicy.setCancellationDate(null);
            }

            InsurancePolicy updatedPolicy = insurancePolicyRepository.save(existingPolicy);
            return insurancePolicyMapper.toResponseDto(updatedPolicy);
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, insurancePolicyDTO);
            throw e;
        }
    }

    @Override
    public void deleteInsurancePolicy(Long id) {
        InsurancePolicy insurancePolicy = getEntityById(id);
        // Soft delete - don't physically delete
        insurancePolicy.setDeleted(true);
        insurancePolicyRepository.save(insurancePolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InsurancePolicyResponseDTO> getAllInsurancePolicies(InsurancePolicyFilterDTO filterDTO, Pageable pageable) {
        Page<InsurancePolicy> policies = insurancePolicyRepository.findAllWithFilters(
                filterDTO.policyNumber(),
                filterDTO.termNumber(),
                filterDTO.policyType() != null ? filterDTO.policyType().name() : null,
                filterDTO.policyStatus() != null ? filterDTO.policyStatus().name() : null,
                filterDTO.paymentFrequency() != null ? filterDTO.paymentFrequency().name() : null,
                filterDTO.issueDateFrom(),
                filterDTO.issueDateTo(),
                filterDTO.effectiveFromStart(),
                filterDTO.effectiveFromEnd(),
                filterDTO.effectiveToStart(),
                filterDTO.effectiveToEnd(),
                filterDTO.isCancelled(),
                pageable
        );

        return policies.map(insurancePolicyMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public InsurancePolicy getEntityById(Long id) {
        return insurancePolicyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new InsurancePolicyNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return insurancePolicyRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public void validatePolicyNumber(String policyNumber, Long excludeId) {
        // Check if there's an active policy (not deleted) with the same number
        boolean exists = insurancePolicyRepository.existsByPolicyNumberAndDeletedFalse(policyNumber);
        if (exists) {
            // If there's an ID to exclude, verify it's not the same record
            if (excludeId != null) {
                // Find the specific active policy with that number
                Optional<InsurancePolicy> existing = insurancePolicyRepository.findByPolicyNumberAndDeletedFalse(policyNumber);
                if (existing.isPresent() && !existing.get().getId().equals(excludeId)) {
                    throw new DuplicatePolicyNumberException(policyNumber);
                }
            } else {
                throw new DuplicatePolicyNumberException(policyNumber);
            }
        }
    }

    private void validateBusinessRules(InsurancePolicyDTO dto) {
        // Validate that effective from date is before effective to date
        if (dto.effectiveFrom() != null && dto.effectiveTo() != null) {
            if (dto.effectiveFrom().isAfter(dto.effectiveTo())) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage("insurancePolicy.effectiveFrom.beforeEffectiveTo"));
            }
        }

        // Validate that cancellation date is not before effective from date
        if (dto.cancellationDate() != null && dto.effectiveFrom() != null) {
            if (dto.cancellationDate().isBefore(dto.effectiveFrom())) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage("insurancePolicy.cancellationDate.beforeEffectiveFrom"));
            }
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e, InsurancePolicyDTO insurancePolicyDTO) {
        String errorMessage = e.getMessage().toLowerCase();

        // Detectar violación de constraint de policyNumber
        if (errorMessage.contains("policy_number") || errorMessage.contains("uk_") && errorMessage.contains("policy")) {
            throw new InsurancePolicyDataConflictException(
                messageSourceHelper.getMessage("insurancePolicy.update.conflict.policyNumber", insurancePolicyDTO.policyNumber()),
                e
            );
        }

        // Si es una violación de integridad pero no podemos determinar el campo específico
        throw new InsurancePolicyDataConflictException(
            messageSourceHelper.getMessage("insurancePolicy.update.conflict.generic"),
            e
        );
    }
}
