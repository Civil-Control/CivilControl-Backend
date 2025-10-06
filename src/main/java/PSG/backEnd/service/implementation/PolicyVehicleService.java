package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.insurance.DuplicateVehicleInPolicyException;
import PSG.backEnd.exception.insurance.InsurancePolicyNotFoundException;
import PSG.backEnd.exception.insurance.PolicyVehicleNotFoundException;
import PSG.backEnd.model.dto.insurance.PolicyVehicleDTO;
import PSG.backEnd.model.dto.insurance.PolicyVehicleResponseDTO;
import PSG.backEnd.model.entity.insurance.AutoPolicy;
import PSG.backEnd.model.entity.insurance.PolicyVehicle;
import PSG.backEnd.model.entity.Vehicle;
import PSG.backEnd.model.mapper.PolicyVehicleMapper;
import PSG.backEnd.repository.PolicyVehicleRepository;
import PSG.backEnd.repository.AutoPolicyRepository;
import PSG.backEnd.service.port.IInsurancePolicyService;
import PSG.backEnd.service.port.IPolicyVehicleService;
import PSG.backEnd.service.port.IVehicleService;
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

    @Override
    public PolicyVehicleResponseDTO createPolicyVehicle(PolicyVehicleDTO policyVehicleDTO) {
        throw new UnsupportedOperationException("Use addVehicleToInsurancePolicy instead");
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

        // Validar duplicación solo si cambió el vehículo (si vehicleId está presente en el DTO)
        if (policyVehicleDTO.vehicleId() != null &&
            !existingPolicyVehicle.getVehicle().getId().equals(policyVehicleDTO.vehicleId())) {
            validateVehicleNotInPolicy(policyVehicleDTO.vehicleId(), autoPolicyId, id);

            // Si se cambia el vehículo, cargar la nueva entidad Vehicle completa
            Vehicle newVehicle = vehicleService.getEntityById(policyVehicleDTO.vehicleId());
            existingPolicyVehicle.setVehicle(newVehicle);
        }

        // Actualizar solo los campos permitidos sin tocar las relaciones
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
    public List<PolicyVehicleResponseDTO> getByVehicleId(Long vehicleId) {
        List<PolicyVehicle> policyVehicles = policyVehicleRepository.findByVehicleId(vehicleId);
        return policyVehicles.stream()
                .map(policyVehicleMapper::toResponseDto)
                .toList();
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
            throw new IllegalArgumentException("Insurance Policy with ID " + insurancePolicyId + " does not exist");
        }

        AutoPolicy autoPolicy = getOrCreateAutoPolicy(insurancePolicyId);

        // Verificar si existe un PolicyVehicle eliminado con el mismo vehículo y autoPolicy para reactivarlo
        Optional<PolicyVehicle> deletedPolicyVehicle = policyVehicleRepository
                .findByVehicleIdAndAutoPolicyIdAndDeletedTrue(policyVehicleDTO.vehicleId(), autoPolicy.getId());

        if (deletedPolicyVehicle.isPresent()) {
            // Reactivar el PolicyVehicle existente
            return reactivatePolicyVehicle(deletedPolicyVehicle.get(), policyVehicleDTO);
        }

        return createPolicyVehicleInternal(policyVehicleDTO, autoPolicy.getId());
    }

    private PolicyVehicleResponseDTO reactivatePolicyVehicle(PolicyVehicle deletedPolicyVehicle, PolicyVehicleDTO newData) {
        // Validar reglas de negocio con los nuevos datos
        validateBusinessRulesInternal(newData, deletedPolicyVehicle.getAutoPolicy().getId());

        // Log para debug - verificar que tenemos el ID original
        System.out.println("Reactivating policy vehicle with ID: " + deletedPolicyVehicle.getId());

        // Actualizar el PolicyVehicle eliminado con los nuevos datos
        Long originalId = deletedPolicyVehicle.getId();

        policyVehicleMapper.partialUpdate(newData, deletedPolicyVehicle);

        // Asegurar que el ID no se perdió
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
            throw new IllegalArgumentException("Vehicle with ID " + dto.vehicleId() + " does not exist");
        }

        if (!autoPolicyRepository.existsByIdAndDeletedFalse(autoPolicyId)) {
            throw new IllegalArgumentException("Auto Policy with ID " + autoPolicyId + " does not exist");
        }

        if (dto.effectiveFrom() != null && dto.effectiveTo() != null) {
            if (dto.effectiveFrom().isAfter(dto.effectiveTo())) {
                throw new IllegalArgumentException("Effective from date must be before effective to date");
            }
        }

        if (dto.cancellationDate() != null && dto.effectiveFrom() != null) {
            if (dto.cancellationDate().isBefore(dto.effectiveFrom())) {
                throw new IllegalArgumentException("Cancellation date cannot be before effective from date");
            }
        }

        if (dto.sumInsured() != null && dto.sumInsured().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sum insured must be greater than zero");
        }

        if (dto.numberOfInstallments() != null && (dto.numberOfInstallments() < 1 || dto.numberOfInstallments() > 12)) {
            throw new IllegalArgumentException("Number of installments must be between 1 and 12");
        }
    }
}
