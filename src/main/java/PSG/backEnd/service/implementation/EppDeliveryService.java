package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.eppDelivery.EppDeliveryNotFoundException;
import PSG.backEnd.exception.eppDelivery.EppDeliveryNotValidException;
import PSG.backEnd.model.dto.employee.EppDeliveryBatchDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryFilterDTO;
import PSG.backEnd.model.dto.employee.EppDeliveryResponseDTO;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.employee.EppDelivery;
import PSG.backEnd.model.mapper.EppDeliveryMapper;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.EppDeliveryRepository;
import PSG.backEnd.service.port.IEppDeliveryService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EppDeliveryService implements IEppDeliveryService {

    private final EppDeliveryRepository eppDeliveryRepository;
    private final EmployeeRepository employeeRepository;
    private final EppDeliveryMapper eppDeliveryMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public EppDeliveryResponseDTO createEppDelivery(EppDeliveryDTO eppDeliveryDTO) {
        // Validate employee exists
        validateEmployeeExists(eppDeliveryDTO.employeeId());

        // Validate business rules
        validateBusinessRules(eppDeliveryDTO);

        // Get the employee entity
        Employee employee = employeeRepository.findByIdAndDeletedFalse(eppDeliveryDTO.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(eppDeliveryDTO.employeeId()));

        // Map DTO to entity
        EppDelivery eppDelivery = eppDeliveryMapper.toEntity(eppDeliveryDTO);
        eppDelivery.setEmployee(employee);

        // Save and return
        EppDelivery savedEppDelivery = eppDeliveryRepository.save(eppDelivery);
        return eppDeliveryMapper.toResponseDto(savedEppDelivery);
    }

    @Override
    @Transactional
    public List<EppDeliveryResponseDTO> createBatchEppDeliveries(EppDeliveryBatchDTO batchDTO) {
        return batchDTO.deliveries().stream()
                .map(this::createEppDelivery)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EppDeliveryResponseDTO> getAllEppDeliveries(EppDeliveryFilterDTO filterDTO, Pageable pageable) {
        return eppDeliveryRepository.findAllWithFilters(
                filterDTO.employeeId(),
                filterDTO.employeeSearch(),
                filterDTO.deliveryDateFrom(),
                filterDTO.deliveryDateTo(),
                filterDTO.itemName(),
                filterDTO.itemType(),
                filterDTO.brand(),
                pageable
        ).map(eppDeliveryMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EppDeliveryResponseDTO getEppDeliveryById(Long id) {
        return eppDeliveryRepository.findByIdAndDeletedFalse(id)
                .map(eppDeliveryMapper::toResponseDto)
                .orElseThrow(() -> new EppDeliveryNotFoundException(id));
    }

    @Override
    @Transactional
    public EppDeliveryResponseDTO updateEppDelivery(Long id, EppDeliveryDTO eppDeliveryDTO) {
        // Find existing EPP delivery
        EppDelivery existingEppDelivery = eppDeliveryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EppDeliveryNotFoundException(id));

        // Validate employee if changed
        if (eppDeliveryDTO.employeeId() != null) {
            validateEmployeeExists(eppDeliveryDTO.employeeId());
        }

        // Validate business rules for update
        validateBusinessRulesForUpdate(eppDeliveryDTO);

        // Update employee if changed
        if (eppDeliveryDTO.employeeId() != null &&
                !existingEppDelivery.getEmployee().getId().equals(eppDeliveryDTO.employeeId())) {
            Employee employee = employeeRepository.findByIdAndDeletedFalse(eppDeliveryDTO.employeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException(eppDeliveryDTO.employeeId()));
            existingEppDelivery.setEmployee(employee);
        }

        // Partial update
        eppDeliveryMapper.partialUpdate(eppDeliveryDTO, existingEppDelivery);

        // Save and return
        EppDelivery updatedEppDelivery = eppDeliveryRepository.save(existingEppDelivery);
        return eppDeliveryMapper.toResponseDto(updatedEppDelivery);
    }

    @Override
    @Transactional
    public void deleteEppDelivery(Long id) {
        EppDelivery eppDelivery = eppDeliveryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EppDeliveryNotFoundException(id));

        // Soft delete
        eppDelivery.setDeleted(true);
        eppDeliveryRepository.save(eppDelivery);
    }

    // ==================== Private Validation Methods ====================

    private void validateEmployeeExists(Long employeeId) {
        if (!employeeRepository.existsByIdAndDeletedFalse(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
    }

    private void validateBusinessRules(EppDeliveryDTO eppDeliveryDTO) {
        // Validate delivery date is not in the future
        if (eppDeliveryDTO.deliveryDate().isAfter(LocalDate.now())) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.deliveryDate.future"));
        }

        // Validate item name is not empty or blank
        if (eppDeliveryDTO.itemName() == null || eppDeliveryDTO.itemName().trim().isEmpty()) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.itemName.empty"));
        }

        // Validate item type is not empty or blank
        if (eppDeliveryDTO.itemType() == null || eppDeliveryDTO.itemType().trim().isEmpty()) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.itemType.empty"));
        }

        // Validate quantity is positive
        if (eppDeliveryDTO.quantity() == null || eppDeliveryDTO.quantity() <= 0) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.quantity.positive"));
        }

        // Validate quantity is not excessively high
        if (eppDeliveryDTO.quantity() > 10000) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.quantity.maxValue"));
        }

        // Validate item name length
        if (eppDeliveryDTO.itemName().length() < 2 || eppDeliveryDTO.itemName().length() > 150) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.itemName.length"));
        }

        // Validate item type length
        if (eppDeliveryDTO.itemType().length() < 2 || eppDeliveryDTO.itemType().length() > 100) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.itemType.length"));
        }

        // Validate brand length if provided
        if (eppDeliveryDTO.brand() != null && eppDeliveryDTO.brand().length() > 100) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.brand.maxLength"));
        }
    }

    private void validateBusinessRulesForUpdate(EppDeliveryDTO eppDeliveryDTO) {
        // Validate delivery date if provided
        if (eppDeliveryDTO.deliveryDate() != null && eppDeliveryDTO.deliveryDate().isAfter(LocalDate.now())) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.deliveryDate.future"));
        }

        // Validate item name if provided
        if (eppDeliveryDTO.itemName() != null) {
            if (eppDeliveryDTO.itemName().trim().isEmpty()) {
                throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.itemName.empty"));
            }
            if (eppDeliveryDTO.itemName().length() < 2 || eppDeliveryDTO.itemName().length() > 150) {
                throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.itemName.length"));
            }
        }

        // Validate item type if provided
        if (eppDeliveryDTO.itemType() != null) {
            if (eppDeliveryDTO.itemType().trim().isEmpty()) {
                throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.itemType.empty"));
            }
            if (eppDeliveryDTO.itemType().length() < 2 || eppDeliveryDTO.itemType().length() > 100) {
                throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.itemType.length"));
            }
        }

        // Validate quantity if provided
        if (eppDeliveryDTO.quantity() != null) {
            if (eppDeliveryDTO.quantity() <= 0) {
                throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.quantity.positive"));
            }
            if (eppDeliveryDTO.quantity() > 10000) {
                throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.quantity.maxValue"));
            }
        }

        // Validate brand if provided
        if (eppDeliveryDTO.brand() != null && eppDeliveryDTO.brand().length() > 100) {
            throw new EppDeliveryNotValidException(messageSourceHelper.getMessage("eppDelivery.brand.maxLength"));
        }
    }
}

