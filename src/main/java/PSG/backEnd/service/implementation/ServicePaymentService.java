package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.serviceSupplier.DuplicateReferenceNumberException;
import PSG.backEnd.exception.serviceSupplier.ServiceAssignmentNotFoundException;
import PSG.backEnd.exception.serviceSupplier.ServicePaymentNotFoundException;
import PSG.backEnd.exception.serviceSupplier.ServicePaymentNotValidException;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentResponseDTO;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.mapper.ServicePaymentMapper;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.ProjectAreaTaskRepository;
import PSG.backEnd.repository.ServiceAssignmentRepository;
import PSG.backEnd.repository.ServicePaymentRepository;
import PSG.backEnd.service.port.IServicePaymentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class ServicePaymentService implements IServicePaymentService {

    private final ServicePaymentRepository servicePaymentRepository;
    private final ServiceAssignmentRepository serviceAssignmentRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final ProjectAreaTaskRepository projectAreaTaskRepository;
    private final ServicePaymentMapper servicePaymentMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public ServicePaymentResponseDTO createServicePayment(ServicePaymentDTO servicePaymentDTO) {
        validateBusinessRules(servicePaymentDTO);
        validateUniqueReferenceNumber(servicePaymentDTO.referenceNumber(), null);

        ServicePayment servicePayment = servicePaymentMapper.toEntity(servicePaymentDTO);
        servicePayment.setDeleted(false);

        // Resolve assignment (required)
        ServiceAssignment assignment = resolveAssignment(servicePaymentDTO.serviceAssignmentId());
        servicePayment.setServiceAssignment(assignment);

        // Resolve project area: explicit > auto-fill from subject
        resolveProjectArea(servicePayment, servicePaymentDTO, assignment);

        // Resolve project area task if provided
        if (servicePaymentDTO.projectAreaTaskId() != null) {
            ProjectAreaTask task = projectAreaTaskRepository.findByIdAndDeletedFalse(servicePaymentDTO.projectAreaTaskId())
                    .orElseThrow(() -> new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", servicePaymentDTO.projectAreaTaskId())));
            servicePayment.setProjectAreaTask(task);
        }

        return servicePaymentMapper.toResponseDto(servicePaymentRepository.save(servicePayment));
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePaymentResponseDTO getServicePaymentById(Long id) {
        ServicePayment servicePayment = servicePaymentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServicePaymentNotFoundException(id));

        return servicePaymentMapper.toResponseDto(servicePayment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServicePaymentResponseDTO> getAllServicePayments(
            ServicePaymentFilterDTO filterDTO, Pageable pageable) {
        Page<ServicePayment> servicePayments = servicePaymentRepository.findAllWithFilters(
                filterDTO.subjectType(),
                filterDTO.serviceAssignmentId(),
                filterDTO.serviceSupplierId(),
                filterDTO.buildingId(),
                filterDTO.projectAreaId(),
                filterDTO.serviceType(),
                filterDTO.vehicleId(),
                filterDTO.year(),
                filterDTO.period(),
                filterDTO.startDate(),
                filterDTO.endDate(),
                filterDTO.minAmount(),
                filterDTO.maxAmount(),
                filterDTO.referenceNumber(),
                filterDTO.supplierName(),
                filterDTO.search(),
                pageable
        );

        return servicePayments.map(servicePaymentMapper::toResponseDto);
    }

    @Override
    public ServicePaymentResponseDTO updateServicePayment(Long id, ServicePaymentDTO servicePaymentDTO) {
        ServicePayment existingServicePayment = servicePaymentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServicePaymentNotFoundException(id));

        // Update assignment if provided
        if (servicePaymentDTO.serviceAssignmentId() != null) {
            ServiceAssignment assignment = resolveAssignment(servicePaymentDTO.serviceAssignmentId());
            existingServicePayment.setServiceAssignment(assignment);
        }

        // Update project area if provided
        if (servicePaymentDTO.projectAreaId() != null) {
            ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(servicePaymentDTO.projectAreaId())
                    .orElseThrow(() -> new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", servicePaymentDTO.projectAreaId())));
            existingServicePayment.setProjectArea(projectArea);
        }

        // Update project area task if provided
        if (servicePaymentDTO.projectAreaTaskId() != null) {
            ProjectAreaTask task = projectAreaTaskRepository.findByIdAndDeletedFalse(servicePaymentDTO.projectAreaTaskId())
                    .orElseThrow(() -> new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", servicePaymentDTO.projectAreaTaskId())));
            existingServicePayment.setProjectAreaTask(task);
        } else {
            existingServicePayment.setProjectAreaTask(null);
        }

        if (servicePaymentDTO.referenceNumber() != null &&
            !servicePaymentDTO.referenceNumber().equals(existingServicePayment.getReferenceNumber())) {
            validateUniqueReferenceNumber(servicePaymentDTO.referenceNumber(), id);
        }

        validateBusinessRulesForUpdate(servicePaymentDTO);

        servicePaymentMapper.partialUpdate(servicePaymentDTO, existingServicePayment);
        return servicePaymentMapper.toResponseDto(servicePaymentRepository.save(existingServicePayment));
    }

    @Override
    public void deleteServicePayment(Long id) {
        ServicePayment servicePayment = servicePaymentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServicePaymentNotFoundException(id));

        servicePayment.setDeleted(true);
        servicePaymentRepository.save(servicePayment);
    }

    // ============= Private helper methods =============

    private ServiceAssignment resolveAssignment(Long serviceAssignmentId) {
        if (serviceAssignmentId == null) {
            throw new ServicePaymentNotValidException(
                    messageSourceHelper.getMessage("servicePayment.serviceAssignment.required"));
        }
        return serviceAssignmentRepository.findByIdAndDeletedFalse(serviceAssignmentId)
                .orElseThrow(() -> new ServiceAssignmentNotFoundException(serviceAssignmentId));
    }

    private void resolveProjectArea(ServicePayment payment, ServicePaymentDTO dto, ServiceAssignment assignment) {
        if (dto.projectAreaId() != null) {
            ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", dto.projectAreaId())));
            payment.setProjectArea(projectArea);
        } else {
            // Auto-fill from assignment's subject
            if (assignment.getSubjectType() == SubjectType.BUILDING
                    && assignment.getBuilding() != null && assignment.getBuilding().getProjectArea() != null) {
                payment.setProjectArea(assignment.getBuilding().getProjectArea());
            } else if (assignment.getSubjectType() == SubjectType.VEHICLE
                    && assignment.getVehicle() != null && assignment.getVehicle().getProjectArea() != null) {
                payment.setProjectArea(assignment.getVehicle().getProjectArea());
            }
        }
    }

    // ============= Private validation methods =============

    private void validateBusinessRules(ServicePaymentDTO servicePaymentDTO) {
        if (servicePaymentDTO.paymentDate().isAfter(LocalDate.now())) {
            throw new ServicePaymentNotValidException(messageSourceHelper.getMessage("servicePayment.paymentDate.future"));
        }

        if (servicePaymentDTO.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServicePaymentNotValidException(messageSourceHelper.getMessage("servicePayment.amount.positive"));
        }

        if (servicePaymentDTO.amount().compareTo(BigDecimal.valueOf(99999999.99)) > 0) {
            throw new ServicePaymentNotValidException(messageSourceHelper.getMessage("servicePayment.amount.exceedsMaximum"));
        }
    }

    private void validateBusinessRulesForUpdate(ServicePaymentDTO servicePaymentDTO) {
        if (servicePaymentDTO.paymentDate() != null &&
            servicePaymentDTO.paymentDate().isAfter(LocalDate.now())) {
            throw new ServicePaymentNotValidException(messageSourceHelper.getMessage("servicePayment.paymentDate.future"));
        }

        if (servicePaymentDTO.amount() != null) {
            if (servicePaymentDTO.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServicePaymentNotValidException(messageSourceHelper.getMessage("servicePayment.amount.positive"));
            }

            if (servicePaymentDTO.amount().compareTo(BigDecimal.valueOf(99999999.99)) > 0) {
                throw new ServicePaymentNotValidException(messageSourceHelper.getMessage("servicePayment.amount.exceedsMaximum"));
            }
        }
    }

    private void validateUniqueReferenceNumber(String referenceNumber, Long excludePaymentId) {
        if (referenceNumber != null && !referenceNumber.trim().isEmpty()) {
            boolean exists;
            if (excludePaymentId != null) {
                exists = servicePaymentRepository.existsByReferenceNumberAndDeletedFalseExcludingId(referenceNumber, excludePaymentId);
            } else {
                exists = servicePaymentRepository.existsByReferenceNumberAndDeletedFalse(referenceNumber);
            }

            if (exists) {
                throw new DuplicateReferenceNumberException(referenceNumber);
            }
        }
    }
}

