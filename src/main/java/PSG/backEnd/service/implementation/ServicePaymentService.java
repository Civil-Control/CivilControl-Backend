package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.serviceSupplier.DuplicateReferenceNumberException;
import PSG.backEnd.exception.serviceSupplier.ServiceAssignmentNotFoundException;
import PSG.backEnd.exception.serviceSupplier.ServicePaymentNotFoundException;
import PSG.backEnd.exception.serviceSupplier.ServicePaymentNotValidException;
import PSG.backEnd.exception.vehicle.VehicleNotValidException;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentResponseDTO;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.PaymentSubjectType;
import PSG.backEnd.model.mapper.ServicePaymentMapper;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.ServiceAssignmentRepository;
import PSG.backEnd.repository.ServicePaymentRepository;
import PSG.backEnd.repository.VehicleRepository;
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
    private final VehicleRepository vehicleRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final ServicePaymentMapper servicePaymentMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public ServicePaymentResponseDTO createServicePayment(ServicePaymentDTO servicePaymentDTO) {
        validateBusinessRules(servicePaymentDTO);
        validateUniqueReferenceNumber(servicePaymentDTO.referenceNumber(), null);

        ServicePayment servicePayment = servicePaymentMapper.toEntity(servicePaymentDTO);
        servicePayment.setSubjectType(servicePaymentDTO.subjectType());
        servicePayment.setDeleted(false);

        if (servicePaymentDTO.subjectType() == PaymentSubjectType.BUILDING) {
            resolveBuilding(servicePayment, servicePaymentDTO);
        } else if (servicePaymentDTO.subjectType() == PaymentSubjectType.VEHICLE) {
            resolveVehicle(servicePayment, servicePaymentDTO);
        }

        resolveProjectArea(servicePayment, servicePaymentDTO);

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

        // Handle subject type change or association updates
        if (servicePaymentDTO.subjectType() != null) {
            existingServicePayment.setSubjectType(servicePaymentDTO.subjectType());

            if (servicePaymentDTO.subjectType() == PaymentSubjectType.BUILDING) {
                existingServicePayment.setVehicle(null);
                if (servicePaymentDTO.serviceAssignmentId() != null) {
                    ServiceAssignment assignment = serviceAssignmentRepository.findByIdAndDeletedFalse(servicePaymentDTO.serviceAssignmentId())
                            .orElseThrow(() -> new ServiceAssignmentNotFoundException(servicePaymentDTO.serviceAssignmentId()));
                    existingServicePayment.setServiceAssignment(assignment);
                }
            } else if (servicePaymentDTO.subjectType() == PaymentSubjectType.VEHICLE) {
                existingServicePayment.setServiceAssignment(null);
                if (servicePaymentDTO.vehicleId() != null) {
                    Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(servicePaymentDTO.vehicleId())
                            .orElseThrow(() -> new VehicleNotValidException(servicePaymentDTO.vehicleId()));
                    existingServicePayment.setVehicle(vehicle);
                }
            }
        } else {
            // Subject type not changing, update associations if provided
            if (servicePaymentDTO.serviceAssignmentId() != null) {
                ServiceAssignment assignment = serviceAssignmentRepository.findByIdAndDeletedFalse(servicePaymentDTO.serviceAssignmentId())
                        .orElseThrow(() -> new ServiceAssignmentNotFoundException(servicePaymentDTO.serviceAssignmentId()));
                existingServicePayment.setServiceAssignment(assignment);
            }
            if (servicePaymentDTO.vehicleId() != null) {
                Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(servicePaymentDTO.vehicleId())
                        .orElseThrow(() -> new VehicleNotValidException(servicePaymentDTO.vehicleId()));
                existingServicePayment.setVehicle(vehicle);
            }
        }

        if (servicePaymentDTO.projectAreaId() != null) {
            ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(servicePaymentDTO.projectAreaId())
                    .orElseThrow(() -> new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", servicePaymentDTO.projectAreaId())));
            existingServicePayment.setProjectArea(projectArea);
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

    private void resolveBuilding(ServicePayment payment, ServicePaymentDTO dto) {
        if (dto.serviceAssignmentId() == null) {
            throw new ServicePaymentNotValidException(
                    messageSourceHelper.getMessage("servicePayment.serviceAssignment.required"));
        }
        ServiceAssignment assignment = serviceAssignmentRepository.findByIdAndDeletedFalse(dto.serviceAssignmentId())
                .orElseThrow(() -> new ServiceAssignmentNotFoundException(dto.serviceAssignmentId()));
        payment.setServiceAssignment(assignment);
    }

    private void resolveVehicle(ServicePayment payment, ServicePaymentDTO dto) {
        if (dto.vehicleId() == null) {
            throw new ServicePaymentNotValidException(
                    messageSourceHelper.getMessage("servicePayment.vehicle.required"));
        }
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(dto.vehicleId())
                .orElseThrow(() -> new VehicleNotValidException(dto.vehicleId()));
        payment.setVehicle(vehicle);
    }

    private void resolveProjectArea(ServicePayment payment, ServicePaymentDTO dto) {
        if (dto.projectAreaId() != null) {
            ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException(messageSourceHelper.getMessage("projectArea.notFound", dto.projectAreaId())));
            payment.setProjectArea(projectArea);
        } else if (payment.getServiceAssignment() != null && payment.getServiceAssignment().getProjectArea() != null) {
            payment.setProjectArea(payment.getServiceAssignment().getProjectArea());
        } else if (payment.getVehicle() != null && payment.getVehicle().getProjectArea() != null) {
            payment.setProjectArea(payment.getVehicle().getProjectArea());
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

