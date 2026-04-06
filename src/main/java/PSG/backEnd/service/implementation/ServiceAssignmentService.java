package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.building.BuildingNotFoundException;
import PSG.backEnd.exception.serviceSupplier.ServiceAssignmentAlreadyExistsException;
import PSG.backEnd.exception.serviceSupplier.ServiceAssignmentNotFoundException;
import PSG.backEnd.exception.serviceSupplier.ServicePaymentNotValidException;
import PSG.backEnd.exception.serviceSupplier.ServiceSupplierNotFoundException;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentResponseDTO;
import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.serviceSupplier.ServiceSupplier;
import PSG.backEnd.model.mapper.ServiceAssignmentMapper;
import PSG.backEnd.repository.BuildingRepository;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.ServiceAssignmentRepository;
import PSG.backEnd.repository.ServiceSupplierRepository;
import PSG.backEnd.service.port.IServiceAssignmentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceAssignmentService implements IServiceAssignmentService {

    private final ServiceAssignmentRepository serviceAssignmentRepository;
    private final ServiceSupplierRepository serviceSupplierRepository;
    private final BuildingRepository buildingRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final ServiceAssignmentMapper serviceAssignmentMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public ServiceAssignmentResponseDTO createServiceAssignment(ServiceAssignmentDTO dto) {
        validateServiceSupplierExists(dto.serviceSupplierId());
        validateBuildingExists(dto.buildingId());
        validateServiceTypeProvidedBySupplier(dto.serviceSupplierId(), dto.serviceType());

        if (dto.paymentLocationId() != null) {
            validateBuildingExists(dto.paymentLocationId());
        }
        if (dto.projectAreaId() != null) {
            validateProjectAreaExists(dto.projectAreaId());
        }

        // Check for deleted assignment to reactivate
        Optional<ServiceAssignment> deletedAssignment =
                serviceAssignmentRepository.findByServiceSupplier_IdAndBuilding_IdAndServiceTypeAndDeletedTrue(
                        dto.serviceSupplierId(), dto.buildingId(), dto.serviceType());

        if (deletedAssignment.isPresent()) {
            return reactivateAssignment(deletedAssignment.get(), dto);
        }

        // Check for existing active assignment
        validateAssignmentNotExists(dto.serviceSupplierId(), dto.buildingId(), dto.serviceType());

        return createNewAssignment(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceAssignmentResponseDTO getServiceAssignmentById(Long id) {
        ServiceAssignment assignment = serviceAssignmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceAssignmentNotFoundException(id));
        return serviceAssignmentMapper.toResponseDto(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceAssignmentResponseDTO> getAllServiceAssignments(
            ServiceAssignmentFilterDTO filterDTO, Pageable pageable) {
        Page<ServiceAssignment> assignments = serviceAssignmentRepository.findAllWithFilters(
                filterDTO.serviceSupplierId(),
                filterDTO.buildingId(),
                filterDTO.serviceType(),
                filterDTO.serviceCategory(),
                filterDTO.projectAreaId(),
                filterDTO.search(),
                pageable
        );
        return assignments.map(serviceAssignmentMapper::toResponseDto);
    }

    @Override
    public ServiceAssignmentResponseDTO updateServiceAssignment(Long id, ServiceAssignmentDTO dto) {
        ServiceAssignment existing = serviceAssignmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceAssignmentNotFoundException(id));

        // Validate relationship changes
        Long newSupplierId = dto.serviceSupplierId() != null ? dto.serviceSupplierId() : existing.getServiceSupplier().getId();
        Long newBuildingId = dto.buildingId() != null ? dto.buildingId() : existing.getBuilding().getId();
        PSG.backEnd.model.enums.ServiceType newServiceType = dto.serviceType() != null ? dto.serviceType() : existing.getServiceType();

        if (dto.serviceSupplierId() != null) {
            validateServiceSupplierExists(dto.serviceSupplierId());
            validateServiceTypeProvidedBySupplier(dto.serviceSupplierId(), newServiceType);
        }
        if (dto.buildingId() != null) {
            validateBuildingExists(dto.buildingId());
        }
        if (dto.paymentLocationId() != null) {
            validateBuildingExists(dto.paymentLocationId());
        }
        if (dto.projectAreaId() != null) {
            validateProjectAreaExists(dto.projectAreaId());
        }

        // If key fields changed, check uniqueness
        boolean keyChanged = !newSupplierId.equals(existing.getServiceSupplier().getId())
                || !newBuildingId.equals(existing.getBuilding().getId())
                || !newServiceType.equals(existing.getServiceType());

        if (keyChanged) {
            validateAssignmentNotExists(newSupplierId, newBuildingId, newServiceType);
        }

        // Update relationships
        if (dto.serviceSupplierId() != null && !existing.getServiceSupplier().getId().equals(dto.serviceSupplierId())) {
            ServiceSupplier supplier = serviceSupplierRepository.findByIdAndDeletedFalse(dto.serviceSupplierId())
                    .orElseThrow(() -> new ServiceSupplierNotFoundException(dto.serviceSupplierId()));
            existing.setServiceSupplier(supplier);
        }
        if (dto.buildingId() != null && !existing.getBuilding().getId().equals(dto.buildingId())) {
            Building building = buildingRepository.findByIdAndDeletedFalse(dto.buildingId())
                    .orElseThrow(() -> new BuildingNotFoundException(dto.buildingId()));
            existing.setBuilding(building);
        }
        if (dto.paymentLocationId() != null) {
            Building paymentLocation = buildingRepository.findByIdAndDeletedFalse(dto.paymentLocationId())
                    .orElseThrow(() -> new BuildingNotFoundException(dto.paymentLocationId()));
            existing.setPaymentLocation(paymentLocation);
        }
        if (dto.projectAreaId() != null) {
            ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", dto.projectAreaId())));
            existing.setProjectArea(projectArea);
        }

        serviceAssignmentMapper.partialUpdate(dto, existing);
        return serviceAssignmentMapper.toResponseDto(serviceAssignmentRepository.save(existing));
    }

    @Override
    public void deleteServiceAssignment(Long id) {
        ServiceAssignment assignment = serviceAssignmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceAssignmentNotFoundException(id));
        assignment.setDeleted(true);
        serviceAssignmentRepository.save(assignment);
    }

    // ============= Private validation methods =============

    private void validateServiceSupplierExists(Long serviceSupplierId) {
        if (!serviceSupplierRepository.existsByIdAndDeletedFalse(serviceSupplierId)) {
            throw new ServiceSupplierNotFoundException(serviceSupplierId);
        }
    }

    private void validateBuildingExists(Long buildingId) {
        if (!buildingRepository.existsByIdAndDeletedFalse(buildingId)) {
            throw new BuildingNotFoundException(buildingId);
        }
    }

    private void validateProjectAreaExists(Long projectAreaId) {
        if (!projectAreaRepository.existsByIdAndDeletedFalse(projectAreaId)) {
            throw new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", projectAreaId));
        }
    }

    private void validateServiceTypeProvidedBySupplier(Long serviceSupplierId, PSG.backEnd.model.enums.ServiceType serviceType) {
        ServiceSupplier supplier = serviceSupplierRepository.findByIdAndDeletedFalse(serviceSupplierId)
                .orElseThrow(() -> new ServiceSupplierNotFoundException(serviceSupplierId));

        if (!supplier.getProvidedServices().contains(serviceType)) {
            throw new ServicePaymentNotValidException(
                    messageSourceHelper.getMessage("servicePayment.serviceTypeNotProvided"));
        }
    }

    private void validateAssignmentNotExists(Long serviceSupplierId, Long buildingId, PSG.backEnd.model.enums.ServiceType serviceType) {
        if (serviceAssignmentRepository.existsByServiceSupplier_IdAndBuilding_IdAndServiceTypeAndDeletedFalse(
                serviceSupplierId, buildingId, serviceType)) {
            throw new ServiceAssignmentAlreadyExistsException(
                    messageSourceHelper.getMessage("serviceAssignment.alreadyExists"));
        }
    }

    private ServiceAssignmentResponseDTO reactivateAssignment(ServiceAssignment existing, ServiceAssignmentDTO dto) {
        // Check active record doesn't exist
        validateAssignmentNotExists(dto.serviceSupplierId(), dto.buildingId(), dto.serviceType());

        serviceAssignmentMapper.partialUpdate(dto, existing);

        // Update relationships
        ServiceSupplier supplier = serviceSupplierRepository.findByIdAndDeletedFalse(dto.serviceSupplierId())
                .orElseThrow(() -> new ServiceSupplierNotFoundException(dto.serviceSupplierId()));
        existing.setServiceSupplier(supplier);

        Building building = buildingRepository.findByIdAndDeletedFalse(dto.buildingId())
                .orElseThrow(() -> new BuildingNotFoundException(dto.buildingId()));
        existing.setBuilding(building);

        if (dto.paymentLocationId() != null) {
            Building paymentLocation = buildingRepository.findByIdAndDeletedFalse(dto.paymentLocationId())
                    .orElseThrow(() -> new BuildingNotFoundException(dto.paymentLocationId()));
            existing.setPaymentLocation(paymentLocation);
        }
        if (dto.projectAreaId() != null) {
            ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", dto.projectAreaId())));
            existing.setProjectArea(projectArea);
        }

        existing.setDeleted(false);
        return serviceAssignmentMapper.toResponseDto(serviceAssignmentRepository.save(existing));
    }

    private ServiceAssignmentResponseDTO createNewAssignment(ServiceAssignmentDTO dto) {
        ServiceAssignment assignment = serviceAssignmentMapper.toEntity(dto);

        ServiceSupplier supplier = serviceSupplierRepository.findByIdAndDeletedFalse(dto.serviceSupplierId())
                .orElseThrow(() -> new ServiceSupplierNotFoundException(dto.serviceSupplierId()));
        assignment.setServiceSupplier(supplier);

        Building building = buildingRepository.findByIdAndDeletedFalse(dto.buildingId())
                .orElseThrow(() -> new BuildingNotFoundException(dto.buildingId()));
        assignment.setBuilding(building);

        if (dto.paymentLocationId() != null) {
            Building paymentLocation = buildingRepository.findByIdAndDeletedFalse(dto.paymentLocationId())
                    .orElseThrow(() -> new BuildingNotFoundException(dto.paymentLocationId()));
            assignment.setPaymentLocation(paymentLocation);
        }

        if (dto.projectAreaId() != null) {
            ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", dto.projectAreaId())));
            assignment.setProjectArea(projectArea);
        }

        assignment.setDeleted(false);
        return serviceAssignmentMapper.toResponseDto(serviceAssignmentRepository.save(assignment));
    }
}
