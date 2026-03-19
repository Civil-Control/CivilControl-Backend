package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.building.BuildingNotFoundException;
import PSG.backEnd.exception.serviceSupplier.DuplicateReferenceNumberException;
import PSG.backEnd.exception.serviceSupplier.ServicePaymentNotFoundException;
import PSG.backEnd.exception.serviceSupplier.ServicePaymentNotValidException;
import PSG.backEnd.exception.serviceSupplier.ServiceSupplierNotFoundException;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServicePaymentResponseDTO;
import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
import PSG.backEnd.model.entity.serviceSupplier.ServiceSupplier;
import PSG.backEnd.model.mapper.ServicePaymentMapper;
import PSG.backEnd.repository.BuildingRepository;
import PSG.backEnd.repository.ServicePaymentRepository;
import PSG.backEnd.repository.ServiceSupplierRepository;
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
    private final ServiceSupplierRepository serviceSupplierRepository;
    private final BuildingRepository buildingRepository;
    private final ServicePaymentMapper servicePaymentMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    public ServicePaymentResponseDTO createServicePayment(ServicePaymentDTO servicePaymentDTO) {
        validateServiceSupplierExists(servicePaymentDTO.serviceSupplierId());
        validateBuildingExists(servicePaymentDTO.buildingId());
        validateServiceTypeProvidedBySupplier(servicePaymentDTO.serviceSupplierId(), servicePaymentDTO.serviceType());
        validateBusinessRules(servicePaymentDTO);
        validateUniqueReferenceNumber(servicePaymentDTO.referenceNumber(), null);

        ServiceSupplier serviceSupplier = serviceSupplierRepository.findByIdAndDeletedFalse(servicePaymentDTO.serviceSupplierId())
                .orElseThrow(() -> new ServiceSupplierNotFoundException(servicePaymentDTO.serviceSupplierId()));

        Building building = buildingRepository.findByIdAndDeletedFalse(servicePaymentDTO.buildingId())
                .orElseThrow(() -> new BuildingNotFoundException(servicePaymentDTO.buildingId()));

        ServicePayment servicePayment = servicePaymentMapper.toEntity(servicePaymentDTO);
        servicePayment.setServiceSupplier(serviceSupplier);
        servicePayment.setBuilding(building);
        servicePayment.setDeleted(false);

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
                filterDTO.serviceSupplierId(),
                filterDTO.buildingId(),
                filterDTO.projectAreaId(),
                filterDTO.serviceType(),
                filterDTO.startDate(),
                filterDTO.endDate(),
                filterDTO.minAmount(),
                filterDTO.maxAmount(),
                filterDTO.referenceNumber(),
                filterDTO.supplierName(),
                pageable
        );

        return servicePayments.map(servicePaymentMapper::toResponseDto);
    }

    @Override
    public ServicePaymentResponseDTO updateServicePayment(Long id, ServicePaymentDTO servicePaymentDTO) {
        ServicePayment existingServicePayment = servicePaymentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServicePaymentNotFoundException(id));

        if (servicePaymentDTO.serviceSupplierId() != null) {
            validateServiceSupplierExists(servicePaymentDTO.serviceSupplierId());

            if (!existingServicePayment.getServiceSupplier().getId().equals(servicePaymentDTO.serviceSupplierId())) {
                if (servicePaymentDTO.serviceType() != null) {
                    validateServiceTypeProvidedBySupplier(servicePaymentDTO.serviceSupplierId(), servicePaymentDTO.serviceType());
                } else {
                    validateServiceTypeProvidedBySupplier(servicePaymentDTO.serviceSupplierId(), existingServicePayment.getServiceType());
                }
                updateServiceSupplierRelation(existingServicePayment, servicePaymentDTO.serviceSupplierId());
            }
        }

        if (servicePaymentDTO.buildingId() != null) {
            validateBuildingExists(servicePaymentDTO.buildingId());

            if (!existingServicePayment.getBuilding().getId().equals(servicePaymentDTO.buildingId())) {
                updateBuildingRelation(existingServicePayment, servicePaymentDTO.buildingId());
            }
        }

        if (servicePaymentDTO.serviceType() != null &&
            !existingServicePayment.getServiceType().equals(servicePaymentDTO.serviceType())) {
            validateServiceTypeProvidedBySupplier(
                    existingServicePayment.getServiceSupplier().getId(),
                    servicePaymentDTO.serviceType()
            );
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

    private void validateServiceTypeProvidedBySupplier(Long serviceSupplierId, PSG.backEnd.model.enums.ServiceType serviceType) {
        ServiceSupplier serviceSupplier = serviceSupplierRepository.findByIdAndDeletedFalse(serviceSupplierId)
                .orElseThrow(() -> new ServiceSupplierNotFoundException(serviceSupplierId));

        if (!serviceSupplier.getProvidedServices().contains(serviceType)) {
            throw new ServicePaymentNotValidException(
                    String.format("Service supplier with id %d does not provide service type %s",
                            serviceSupplierId, serviceType));
        }
    }

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

    private void updateServiceSupplierRelation(ServicePayment servicePayment, Long newServiceSupplierId) {
        ServiceSupplier newServiceSupplier = serviceSupplierRepository.findByIdAndDeletedFalse(newServiceSupplierId)
                .orElseThrow(() -> new ServiceSupplierNotFoundException(newServiceSupplierId));
        servicePayment.setServiceSupplier(newServiceSupplier);
    }

    private void updateBuildingRelation(ServicePayment servicePayment, Long newBuildingId) {
        Building newBuilding = buildingRepository.findByIdAndDeletedFalse(newBuildingId)
                .orElseThrow(() -> new BuildingNotFoundException(newBuildingId));
        servicePayment.setBuilding(newBuilding);
    }
}

