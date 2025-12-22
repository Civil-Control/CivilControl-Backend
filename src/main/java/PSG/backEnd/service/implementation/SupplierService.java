package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.supplier.SupplierAlreadyExistsException;
import PSG.backEnd.exception.supplier.SupplierDataConflictException;
import PSG.backEnd.exception.supplier.SupplierNotFoundException;
import PSG.backEnd.model.dto.supplier.SupplierDTO;
import PSG.backEnd.model.dto.supplier.SupplierFilterDTO;
import PSG.backEnd.model.dto.supplier.SupplierResponseDTO;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.mapper.SupplierMapper;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.service.port.ISupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SupplierService implements ISupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional
    public SupplierResponseDTO createSupplier(SupplierDTO supplierDTO) {
        validateNewSupplier(supplierDTO);
        Optional<Supplier> deletedSupplier = findDeletedSupplier(supplierDTO);

        if (deletedSupplier.isPresent()) {
            return reactivateSupplier(deletedSupplier.get(), supplierDTO);
        }

        return createNewSupplier(supplierDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDTO> getAllSuppliers(SupplierFilterDTO filterDTO, Pageable pageable) {
        return supplierRepository.findAllWithFilters(
                filterDTO.cuit(),
                filterDTO.legalName(),
                filterDTO.tradeName(),
                filterDTO.city(),
                filterDTO.minDiscountPercentage(),
                filterDTO.maxDiscountPercentage(),
                filterDTO.active(),
                pageable
        ).map(supplierMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO getSupplierById(Long id) {
        return supplierRepository.findByIdAndDeletedFalse(id)
                .map(supplierMapper::toResponseDto)
                .orElseThrow(() -> new SupplierNotFoundException(id));
    }

    @Override
    @Transactional
    public SupplierResponseDTO updateSupplier(Long id, SupplierDTO supplierDTO) {
        Supplier existingSupplier = supplierRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SupplierNotFoundException(id));

        validateUniqueFieldsForUpdate(supplierDTO, existingSupplier);

        try {
            supplierMapper.partialUpdate(supplierDTO, existingSupplier);
            Supplier updatedSupplier = supplierRepository.save(existingSupplier);
            return supplierMapper.toResponseDto(updatedSupplier);
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, supplierDTO);
            throw e;
        }
    }

    @Override
    @Transactional
    public void updateSupplierBalance(Long supplierId, BigDecimal amount) {
        Supplier supplier = supplierRepository.findByIdAndDeletedFalse(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
        supplier.setPendingBalance(supplier.getPendingBalance().subtract(amount));
        supplierRepository.save(supplier);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SupplierNotFoundException(id));

        supplier.setDeleted(true);
        supplierRepository.save(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return supplierRepository.existsById(id);
    }

    private void validateNewSupplier(SupplierDTO supplierDTO) {
        if (supplierRepository.existsByCuitAndDeletedFalse(supplierDTO.cuit())) {
            throw new SupplierAlreadyExistsException("There is already an active supplier with the CUIT: " + supplierDTO.cuit());
        }
        if (supplierRepository.existsByLegalNameAndDeletedFalse(supplierDTO.legalName())) {
            throw new SupplierAlreadyExistsException("There is already an active supplier with the legalName: " + supplierDTO.legalName());
        }
    }

    private Optional<Supplier> findDeletedSupplier(SupplierDTO supplierDTO) {
        Optional<Supplier> deletedSupplier = supplierRepository.findByCuitAndDeletedTrue(supplierDTO.cuit());
        if (deletedSupplier.isEmpty()) {
            deletedSupplier = supplierRepository.findByLegalNameAndDeletedTrue(supplierDTO.legalName());
        }
        return deletedSupplier;
    }

    private SupplierResponseDTO reactivateSupplier(Supplier supplier, SupplierDTO supplierDTO) {
        supplierMapper.partialUpdate(supplierDTO, supplier);
        supplier.setDeleted(false);
        supplier.setActive(true);
        return supplierMapper.toResponseDto(supplierRepository.save(supplier));
    }

    private SupplierResponseDTO createNewSupplier(SupplierDTO supplierDTO) {
        Supplier supplier = supplierMapper.toEntity(supplierDTO);
        supplier.setDeleted(false);
        supplier.setActive(true);
        supplier.setPendingBalance(BigDecimal.ZERO);
        return supplierMapper.toResponseDto(supplierRepository.save(supplier));
    }

    @Override
    @Transactional(readOnly = true)
    public Supplier getEntityById(Long id) {
        return supplierRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SupplierNotFoundException(id));
    }

    private void validateUniqueFieldsForUpdate(SupplierDTO supplierDTO, Supplier existingSupplier) {
        // Validar CUIT si está siendo actualizado
        if (supplierDTO.cuit() != null && !supplierDTO.cuit().equals(existingSupplier.getCuit())) {
            if (supplierRepository.existsByCuitAndDeletedFalse(supplierDTO.cuit())) {
                throw new SupplierAlreadyExistsException("Cannot update supplier: There is already an active supplier with the CUIT: " + supplierDTO.cuit());
            }
        }

        // Validar legalName si está siendo actualizado
        if (supplierDTO.legalName() != null && !supplierDTO.legalName().equals(existingSupplier.getLegalName())) {
            if (supplierRepository.existsByLegalNameAndDeletedFalse(supplierDTO.legalName())) {
                throw new SupplierAlreadyExistsException("Cannot update supplier: There is already an active supplier with the legal name: " + supplierDTO.legalName());
            }
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e, SupplierDTO supplierDTO) {
        String errorMessage = e.getMessage().toLowerCase();

        // Detectar violación de constraint de CUIT
        if (errorMessage.contains("cuit") || errorMessage.contains("uk_") && errorMessage.contains("cuit")) {
            throw new SupplierDataConflictException(
                "Cannot update supplier: CUIT '" + supplierDTO.cuit() + "' is already in use by another supplier",
                e
            );
        }

        // Detectar violación de constraint de legalName
        if (errorMessage.contains("legal_name") || errorMessage.contains("uk_") && errorMessage.contains("legal")) {
            throw new SupplierDataConflictException(
                "Cannot update supplier: Legal name '" + supplierDTO.legalName() + "' is already in use by another supplier",
                e
            );
        }

        // Si es una violación de integridad pero no podemos determinar el campo específico
        throw new SupplierDataConflictException(
            "Cannot update supplier due to a data integrity violation. Please verify that all unique fields (CUIT, Legal Name) are not already in use",
            e
        );
    }
}
