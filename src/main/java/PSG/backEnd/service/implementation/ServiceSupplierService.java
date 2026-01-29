package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.serviceSupplier.DuplicateServiceTypeException;
import PSG.backEnd.exception.serviceSupplier.ServiceSupplierAlreadyExistsException;
import PSG.backEnd.exception.serviceSupplier.ServiceSupplierNotFoundException;
import PSG.backEnd.exception.supplier.SupplierNotFoundException;
import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierFilterDTO;
import PSG.backEnd.model.dto.serviceSupplier.ServiceSupplierResponseDTO;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.serviceSupplier.ServiceSupplier;
import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.mapper.ServiceSupplierMapper;
import PSG.backEnd.repository.ServiceSupplierRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.service.port.IServiceSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceSupplierService implements IServiceSupplierService {

    private final ServiceSupplierRepository serviceSupplierRepository;
    private final SupplierRepository supplierRepository;
    private final ServiceSupplierMapper serviceSupplierMapper;

    @Override
    public ServiceSupplierResponseDTO createServiceSupplier(ServiceSupplierDTO serviceSupplierDTO) {
        validateSupplierExists(serviceSupplierDTO.supplierId());
        validateNoDuplicateServices(serviceSupplierDTO.providedServices());
        validateServiceSupplierNotExists(serviceSupplierDTO.supplierId());

        Optional<ServiceSupplier> deletedServiceSupplier =
                serviceSupplierRepository.findBySupplier_IdAndDeletedTrue(serviceSupplierDTO.supplierId());

        return deletedServiceSupplier
                .map(ss -> reactivateServiceSupplier(ss, serviceSupplierDTO))
                .orElseGet(() -> createNewServiceSupplier(serviceSupplierDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceSupplierResponseDTO getServiceSupplierById(Long id) {
        ServiceSupplier serviceSupplier = serviceSupplierRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceSupplierNotFoundException(id));

        return serviceSupplierMapper.toResponseDto(serviceSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceSupplierResponseDTO> getAllServiceSuppliers(
            ServiceSupplierFilterDTO filterDTO, Pageable pageable) {
        Page<ServiceSupplier> serviceSuppliers = serviceSupplierRepository.findAllWithFilters(
                filterDTO.supplierName(),
                filterDTO.serviceType(),
                filterDTO.cuit(),
                pageable
        );

        return serviceSuppliers.map(serviceSupplierMapper::toResponseDto);
    }

    @Override
    public ServiceSupplierResponseDTO updateServiceSupplier(Long id, ServiceSupplierDTO serviceSupplierDTO) {
        ServiceSupplier existingServiceSupplier = serviceSupplierRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceSupplierNotFoundException(id));

        if (serviceSupplierDTO.supplierId() != null) {
            validateSupplierExists(serviceSupplierDTO.supplierId());

            if (!existingServiceSupplier.getSupplier().getId().equals(serviceSupplierDTO.supplierId())) {
                validateServiceSupplierNotExists(serviceSupplierDTO.supplierId());
                updateSupplierRelation(existingServiceSupplier, serviceSupplierDTO.supplierId());
            }
        }

        if (serviceSupplierDTO.providedServices() != null) {
            validateNoDuplicateServices(serviceSupplierDTO.providedServices());
        }

        serviceSupplierMapper.partialUpdate(serviceSupplierDTO, existingServiceSupplier);
        return serviceSupplierMapper.toResponseDto(serviceSupplierRepository.save(existingServiceSupplier));
    }

    @Override
    public void deleteServiceSupplier(Long id) {
        ServiceSupplier serviceSupplier = serviceSupplierRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceSupplierNotFoundException(id));

        serviceSupplier.setDeleted(true);
        serviceSupplierRepository.save(serviceSupplier);
    }

    // ============= Private validation methods =============

    private void validateSupplierExists(Long supplierId) {
        if (!supplierRepository.existsByIdAndDeletedFalse(supplierId)) {
            throw new SupplierNotFoundException(supplierId);
        }
    }

    private void validateNoDuplicateServices(List<ServiceType> providedServices) {
        Set<ServiceType> uniqueServices = new HashSet<>(providedServices);
        if (uniqueServices.size() != providedServices.size()) {
            List<ServiceType> duplicates = providedServices.stream()
                    .filter(service -> providedServices.indexOf(service) != providedServices.lastIndexOf(service))
                    .distinct()
                    .toList();

            throw new DuplicateServiceTypeException(
                    "Duplicate service types found in the list: " + duplicates);
        }
    }

    private void validateServiceSupplierNotExists(Long supplierId) {
        if (serviceSupplierRepository.existsBySupplier_IdAndDeletedFalse(supplierId)) {
            throw new ServiceSupplierAlreadyExistsException(
                    "A service supplier already exists for supplier with id: " + supplierId);
        }
    }

    private void updateSupplierRelation(ServiceSupplier serviceSupplier, Long newSupplierId) {
        Supplier newSupplier = supplierRepository.findByIdAndDeletedFalse(newSupplierId)
                .orElseThrow(() -> new SupplierNotFoundException(newSupplierId));
        serviceSupplier.setSupplier(newSupplier);
    }

    private ServiceSupplierResponseDTO reactivateServiceSupplier(
            ServiceSupplier serviceSupplier, ServiceSupplierDTO serviceSupplierDTO) {
        serviceSupplierMapper.partialUpdate(serviceSupplierDTO, serviceSupplier);
        serviceSupplier.setDeleted(false);
        return serviceSupplierMapper.toResponseDto(serviceSupplierRepository.save(serviceSupplier));
    }

    private ServiceSupplierResponseDTO createNewServiceSupplier(ServiceSupplierDTO serviceSupplierDTO) {
        ServiceSupplier serviceSupplier = serviceSupplierMapper.toEntity(serviceSupplierDTO);

        Supplier supplier = supplierRepository.findByIdAndDeletedFalse(serviceSupplierDTO.supplierId())
                .orElseThrow(() -> new SupplierNotFoundException(serviceSupplierDTO.supplierId()));

        serviceSupplier.setSupplier(supplier);
        serviceSupplier.setDeleted(false);

        return serviceSupplierMapper.toResponseDto(serviceSupplierRepository.save(serviceSupplier));
    }
}

