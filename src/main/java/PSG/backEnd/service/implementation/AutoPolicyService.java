package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.insurance.AutoPolicyNotFoundException;
import PSG.backEnd.model.dto.insurance.AutoPolicyDTO;
import PSG.backEnd.model.dto.insurance.AutoPolicyResponseDTO;
import PSG.backEnd.model.entity.insurance.AutoPolicy;
import PSG.backEnd.model.mapper.AutoPolicyMapper;
import PSG.backEnd.repository.AutoPolicyRepository;
import PSG.backEnd.repository.PolicyVehicleRepository;
import PSG.backEnd.service.port.IAutoPolicyService;
import PSG.backEnd.service.port.IInsurancePolicyService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AutoPolicyService implements IAutoPolicyService {

    private final AutoPolicyRepository autoPolicyRepository;
    private final AutoPolicyMapper autoPolicyMapper;
    private final IInsurancePolicyService insurancePolicyService;
    private final PolicyVehicleRepository policyVehicleRepository; // Inyección directa del repository
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public AutoPolicyResponseDTO createAutoPolicy(AutoPolicyDTO autoPolicyDTO) {
        validateBusinessRules(autoPolicyDTO);
        validateInsurancePolicyExists(autoPolicyDTO.insurancePolicyId());

        AutoPolicy autoPolicy = autoPolicyMapper.toEntity(autoPolicyDTO);
        AutoPolicy savedPolicy = autoPolicyRepository.save(autoPolicy);

        return autoPolicyMapper.toResponseDto(savedPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public AutoPolicyResponseDTO getAutoPolicyById(Long id) {
        AutoPolicy autoPolicy = getEntityById(id);
        return autoPolicyMapper.toResponseDto(autoPolicy);
    }

    @Override
    public AutoPolicyResponseDTO updateAutoPolicy(Long id, AutoPolicyDTO autoPolicyDTO) {
        AutoPolicy existingPolicy = getEntityById(id);

        validateBusinessRules(autoPolicyDTO);
        validateInsurancePolicyExists(autoPolicyDTO.insurancePolicyId());

        autoPolicyMapper.partialUpdate(autoPolicyDTO, existingPolicy);
        AutoPolicy updatedPolicy = autoPolicyRepository.save(existingPolicy);

        return autoPolicyMapper.toResponseDto(updatedPolicy);
    }

    @Override
    public void deleteAutoPolicy(Long id) {
        AutoPolicy autoPolicy = getEntityById(id);

        // Eliminar directamente usando el repository para evitar dependencia circular
        var policyVehicles = policyVehicleRepository.findByAutoPolicyId(id);
        policyVehicles.forEach(pv -> {
            pv.setDeleted(true);
            policyVehicleRepository.save(pv);
        });

        // Soft delete de la póliza auto
        autoPolicy.setDeleted(true);
        autoPolicyRepository.save(autoPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AutoPolicyResponseDTO> getAllAutoPolicies(Long insurancePolicyId, String policyNumber, Pageable pageable) {
        Page<AutoPolicy> policies = autoPolicyRepository.findAllWithFilters(
                insurancePolicyId,
                policyNumber,
                pageable
        );

        return policies.map(autoPolicyMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AutoPolicy getEntityById(Long id) {
        return autoPolicyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AutoPolicyNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return autoPolicyRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AutoPolicyResponseDTO getByInsurancePolicyId(Long insurancePolicyId) {
        AutoPolicy autoPolicy = autoPolicyRepository.findByInsurancePolicyId(insurancePolicyId)
                .orElseThrow(() -> new AutoPolicyNotFoundException("insurance policy ID", insurancePolicyId.toString()));

        return autoPolicyMapper.toResponseDto(autoPolicy);
    }

    private void validateBusinessRules(AutoPolicyDTO dto) {
        // Validar que la póliza de seguro exista
        if (dto.insurancePolicyId() == null) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("autoPolicy.insurancePolicyId.required"));
        }

        // Validar que haya al menos un vehículo (si se proporcionan)
        if (dto.policyVehicles() != null && dto.policyVehicles().isEmpty()) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("autoPolicy.policyVehicles.required"));
        }

        // Validar que no haya vehículos duplicados
        if (dto.policyVehicles() != null) {
            long uniqueVehicleIds = dto.policyVehicles().stream()
                    .mapToLong(pv -> pv.vehicleId())
                    .distinct()
                    .count();

            if (uniqueVehicleIds != dto.policyVehicles().size()) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage("autoPolicy.policyVehicles.duplicates"));
            }
        }
    }

    private void validateInsurancePolicyExists(Long insurancePolicyId) {
        if (!insurancePolicyService.existsById(insurancePolicyId)) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("autoPolicy.insurancePolicy.notFound", insurancePolicyId));
        }
    }
}
