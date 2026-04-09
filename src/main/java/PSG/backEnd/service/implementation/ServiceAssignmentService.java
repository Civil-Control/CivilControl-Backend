package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.building.BuildingNotFoundException;
import PSG.backEnd.exception.serviceSupplier.ServiceAssignmentAlreadyExistsException;
import PSG.backEnd.exception.serviceSupplier.ServiceAssignmentNotFoundException;
import PSG.backEnd.exception.serviceSupplier.ServicePaymentNotValidException;
import PSG.backEnd.exception.serviceSupplier.ServiceSupplierNotFoundException;
import PSG.backEnd.exception.vehicle.VehicleNotValidException;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceAssignmentResponseDTO;
import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.serviceSupplier.ServiceSupplier;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.mapper.ServiceAssignmentMapper;
import PSG.backEnd.repository.BuildingRepository;
import PSG.backEnd.repository.ServiceAssignmentRepository;
import PSG.backEnd.repository.ServiceSupplierRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.service.port.IServiceAssignmentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceAssignmentService implements IServiceAssignmentService {

    private final ServiceAssignmentRepository serviceAssignmentRepository;
    private final ServiceSupplierRepository serviceSupplierRepository;
    private final BuildingRepository buildingRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceAssignmentMapper serviceAssignmentMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public ServiceAssignmentResponseDTO createServiceAssignment(ServiceAssignmentDTO dto) {
        validateServiceSupplierExists(dto.serviceSupplierId());
        validateSubjectReference(dto.subjectType(), dto.buildingId(), dto.vehicleId());
        validateServiceTypeProvidedBySupplier(dto.serviceSupplierId(), dto.serviceType());

        if (dto.paymentLocationId() != null) {
            validateBuildingExists(dto.paymentLocationId());
        }

        validateAccountNumberUnique(dto.accountNumber(), null);

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
                filterDTO.subjectType(),
                filterDTO.serviceSupplierId(),
                filterDTO.buildingId(),
                filterDTO.vehicleId(),
                filterDTO.serviceType(),
                filterDTO.search(),
                pageable
        );
        return assignments.map(serviceAssignmentMapper::toResponseDto);
    }

    @Override
    public ServiceAssignmentResponseDTO updateServiceAssignment(Long id, ServiceAssignmentDTO dto) {
        ServiceAssignment existing = serviceAssignmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceAssignmentNotFoundException(id));

        PSG.backEnd.model.enums.ServiceType newServiceType = dto.serviceType() != null ? dto.serviceType() : existing.getServiceType();

        if (dto.serviceSupplierId() != null) {
            validateServiceSupplierExists(dto.serviceSupplierId());
            validateServiceTypeProvidedBySupplier(dto.serviceSupplierId(), newServiceType);
        }

        SubjectType effectiveSubjectType = dto.subjectType() != null ? dto.subjectType() : existing.getSubjectType();
        Long effectiveBuildingId = dto.buildingId() != null ? dto.buildingId() : (existing.getBuilding() != null ? existing.getBuilding().getId() : null);
        Long effectiveVehicleId = dto.vehicleId() != null ? dto.vehicleId() : (existing.getVehicle() != null ? existing.getVehicle().getId() : null);

        if (dto.subjectType() != null || dto.buildingId() != null || dto.vehicleId() != null) {
            validateSubjectReference(effectiveSubjectType, effectiveBuildingId, effectiveVehicleId);
        }

        if (dto.paymentLocationId() != null) {
            validateBuildingExists(dto.paymentLocationId());
        }

        validateAccountNumberUnique(dto.accountNumber(), id);

        // Update relationships
        if (dto.serviceSupplierId() != null && !existing.getServiceSupplier().getId().equals(dto.serviceSupplierId())) {
            ServiceSupplier supplier = serviceSupplierRepository.findByIdAndDeletedFalse(dto.serviceSupplierId())
                    .orElseThrow(() -> new ServiceSupplierNotFoundException(dto.serviceSupplierId()));
            existing.setServiceSupplier(supplier);
        }

        // Handle subject type change
        if (dto.subjectType() != null) {
            existing.setSubjectType(dto.subjectType());
        }

        if (effectiveSubjectType == SubjectType.BUILDING) {
            if (dto.buildingId() != null) {
                Building building = buildingRepository.findByIdAndDeletedFalse(dto.buildingId())
                        .orElseThrow(() -> new BuildingNotFoundException(dto.buildingId()));
                existing.setBuilding(building);
            }
            existing.setVehicle(null);
        } else if (effectiveSubjectType == SubjectType.VEHICLE) {
            if (dto.vehicleId() != null) {
                Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(dto.vehicleId())
                        .orElseThrow(() -> new VehicleNotValidException(dto.vehicleId()));
                existing.setVehicle(vehicle);
            }
            existing.setBuilding(null);
        }

        if (dto.paymentLocationId() != null) {
            Building paymentLocation = buildingRepository.findByIdAndDeletedFalse(dto.paymentLocationId())
                    .orElseThrow(() -> new BuildingNotFoundException(dto.paymentLocationId()));
            existing.setPaymentLocation(paymentLocation);
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

    private void validateSubjectReference(SubjectType subjectType, Long buildingId, Long vehicleId) {
        if (subjectType == SubjectType.BUILDING) {
            if (buildingId == null) {
                throw new ServicePaymentNotValidException(
                        messageSourceHelper.getMessage("serviceAssignment.building.required"));
            }
            validateBuildingExists(buildingId);
        } else if (subjectType == SubjectType.VEHICLE) {
            if (vehicleId == null) {
                throw new ServicePaymentNotValidException(
                        messageSourceHelper.getMessage("serviceAssignment.vehicle.required"));
            }
            validateVehicleExists(vehicleId);
        }
    }

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

    private void validateVehicleExists(Long vehicleId) {
        if (!vehicleRepository.existsByIdAndDeletedFalse(vehicleId)) {
            throw new VehicleNotValidException(vehicleId);
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

    private void validateAccountNumberUnique(String accountNumber, Long excludeId) {
        if (accountNumber == null || accountNumber.isBlank()) return;
        boolean exists = excludeId != null
                ? serviceAssignmentRepository.existsByAccountNumberAndDeletedFalseAndIdNot(accountNumber, excludeId)
                : serviceAssignmentRepository.existsByAccountNumberAndDeletedFalse(accountNumber);
        if (exists) {
            throw new ServiceAssignmentAlreadyExistsException(
                    messageSourceHelper.getMessage("serviceAssignment.accountNumber.duplicate", accountNumber));
        }
    }

    private ServiceAssignmentResponseDTO createNewAssignment(ServiceAssignmentDTO dto) {
        ServiceAssignment assignment = serviceAssignmentMapper.toEntity(dto);

        ServiceSupplier supplier = serviceSupplierRepository.findByIdAndDeletedFalse(dto.serviceSupplierId())
                .orElseThrow(() -> new ServiceSupplierNotFoundException(dto.serviceSupplierId()));
        assignment.setServiceSupplier(supplier);

        if (dto.subjectType() == SubjectType.BUILDING) {
            Building building = buildingRepository.findByIdAndDeletedFalse(dto.buildingId())
                    .orElseThrow(() -> new BuildingNotFoundException(dto.buildingId()));
            assignment.setBuilding(building);
        } else if (dto.subjectType() == SubjectType.VEHICLE) {
            Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(dto.vehicleId())
                    .orElseThrow(() -> new VehicleNotValidException(dto.vehicleId()));
            assignment.setVehicle(vehicle);
        }

        if (dto.paymentLocationId() != null) {
            Building paymentLocation = buildingRepository.findByIdAndDeletedFalse(dto.paymentLocationId())
                    .orElseThrow(() -> new BuildingNotFoundException(dto.paymentLocationId()));
            assignment.setPaymentLocation(paymentLocation);
        }

        assignment.setDeleted(false);
        return serviceAssignmentMapper.toResponseDto(serviceAssignmentRepository.save(assignment));
    }
}
