package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.insurance.DuplicatePolicyNumberException;
import PSG.backEnd.exception.insurance.InsurancePolicyNotFoundException;
import PSG.backEnd.model.dto.insurance.InsurancePolicyDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyFilterDTO;
import PSG.backEnd.model.dto.insurance.InsurancePolicyResponseDTO;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.mapper.InsurancePolicyMapper;
import PSG.backEnd.repository.InsurancePolicyRepository;
import PSG.backEnd.service.port.IInsurancePolicyService;
import lombok.RequiredArgsConstructor;
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

    @Override
    public InsurancePolicyResponseDTO createInsurancePolicy(InsurancePolicyDTO insurancePolicyDTO) {
        // Verificar si existe una póliza eliminada con el mismo número para reactivarla
        Optional<InsurancePolicy> deletedPolicy = insurancePolicyRepository
                .findByPolicyNumberAndDeletedTrue(insurancePolicyDTO.policyNumber());

        if (deletedPolicy.isPresent()) {
            // Reactivar la póliza existente
            return reactivateInsurancePolicy(deletedPolicy.get(), insurancePolicyDTO);
        }

        // Si no existe póliza eliminada, validar que no hay duplicados activos
        validatePolicyNumber(insurancePolicyDTO.policyNumber(), null);
        validateBusinessRules(insurancePolicyDTO);

        // Crear nueva póliza
        InsurancePolicy insurancePolicy = insurancePolicyMapper.toEntity(insurancePolicyDTO);
        InsurancePolicy savedPolicy = insurancePolicyRepository.save(insurancePolicy);

        return insurancePolicyMapper.toResponseDto(savedPolicy);
    }

    private InsurancePolicyResponseDTO reactivateInsurancePolicy(InsurancePolicy deletedPolicy, InsurancePolicyDTO newData) {
        // Validar reglas de negocio con los nuevos datos
        validateBusinessRules(newData);

        // Log para debug - verificar que tenemos el ID original
        System.out.println("Reactivating policy with ID: " + deletedPolicy.getId());

        // Actualizar la póliza eliminada con los nuevos datos
        // Asegurar que mantenemos el ID original
        Long originalId = deletedPolicy.getId();

        insurancePolicyMapper.partialUpdate(newData, deletedPolicy);

        // Asegurar que el ID no se perdió
        deletedPolicy.setId(originalId);

        // Reactivar la póliza
        deletedPolicy.setDeleted(false);

        // Log para debug - verificar que seguimos teniendo el ID original
        System.out.println("About to save policy with ID: " + deletedPolicy.getId());

        // Guardar la póliza reactivada (debe actualizar, no crear nuevo registro)
        InsurancePolicy reactivatedPolicy = insurancePolicyRepository.save(deletedPolicy);

        // Log para debug - verificar el ID después de save
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

        // Validar número de póliza si cambió
        if (!existingPolicy.getPolicyNumber().equals(insurancePolicyDTO.policyNumber())) {
            validatePolicyNumber(insurancePolicyDTO.policyNumber(), id);
        }

        validateBusinessRules(insurancePolicyDTO);

        insurancePolicyMapper.partialUpdate(insurancePolicyDTO, existingPolicy);
        InsurancePolicy updatedPolicy = insurancePolicyRepository.save(existingPolicy);

        return insurancePolicyMapper.toResponseDto(updatedPolicy);
    }

    @Override
    public void deleteInsurancePolicy(Long id) {
        InsurancePolicy insurancePolicy = getEntityById(id);
        // Soft delete - no eliminamos físicamente
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
        // Buscar si existe una póliza activa (no eliminada) con el mismo número
        boolean exists = insurancePolicyRepository.existsByPolicyNumberAndDeletedFalse(policyNumber);
        if (exists) {
            // Si hay un ID a excluir, verificar que no sea el mismo registro
            if (excludeId != null) {
                // Buscar la póliza activa específica con ese número
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
        // Validar que la fecha de vigencia desde sea anterior a la fecha hasta
        if (dto.effectiveFrom() != null && dto.effectiveTo() != null) {
            if (dto.effectiveFrom().isAfter(dto.effectiveTo())) {
                throw new IllegalArgumentException("Effective from date must be before effective to date");
            }
        }

        // Validar que la fecha de cancelación no sea anterior a la fecha de vigencia
        if (dto.cancellationDate() != null && dto.effectiveFrom() != null) {
            if (dto.cancellationDate().isBefore(dto.effectiveFrom())) {
                throw new IllegalArgumentException("Cancellation date cannot be before effective from date");
            }
        }
    }
}
