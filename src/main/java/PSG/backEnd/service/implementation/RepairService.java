package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.supplier.SupplierNotValidException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotFoundException;
import PSG.backEnd.exception.vehicle.RepairNotFoundException;
import PSG.backEnd.exception.vehicle.VehicleNotValidException;
import PSG.backEnd.model.dto.vehicle.RepairDTO;
import PSG.backEnd.model.dto.vehicle.RepairFilterDTO;
import PSG.backEnd.model.dto.vehicle.RepairItemDTO;
import PSG.backEnd.model.dto.vehicle.RepairResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.entity.vehicle.RepairItem;
import PSG.backEnd.model.mapper.RepairMapper;
import PSG.backEnd.repository.RepairItemRepository;
import PSG.backEnd.repository.RepairRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.service.port.IRepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairService implements IRepairService {

    private final RepairRepository repairRepository;
    private final RepairMapper repairMapper;
    private final VehicleRepository vehicleRepository;
    private final SupplierRepository supplierRepository;
    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final DocumentTotalRecalculator documentTotalRecalculator;
    private final RepairItemRepository repairItemRepository;

    @Override
    @Transactional
    public RepairResponseDTO createRepair(RepairDTO repairDTO) {
        validateVehicleExists(repairDTO.vehicleId());
        if (repairDTO.supplierId() != null) {
            validateSupplierExists(repairDTO.supplierId());
        }

        Repair repair = repairMapper.toEntity(repairDTO);
        applyItems(repair, repairDTO.items());

        Repair savedRepair = repairRepository.save(repair);
        recalculateItemDocuments(savedRepair);

        return repairMapper.toResponseDto(savedRepair);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepairResponseDTO> getAllRepairs(RepairFilterDTO filterDTO, Pageable pageable) {
        if (filterDTO.vehicleId() != null) {
            validateVehicleExists(filterDTO.vehicleId());
        }
        if (filterDTO.supplierId() != null) {
            validateSupplierExists(filterDTO.supplierId());
        }

        Page<Repair> repairs = repairRepository.findAllWithFilters(
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.date(),
                filterDTO.vehicleId(),
                filterDTO.vehicleLicensePlate(),
                filterDTO.projectAreaId(),
                filterDTO.minCost(),
                filterDTO.maxCost(),
                filterDTO.supplierId(),
                filterDTO.supplierLegalName(),
                filterDTO.description(),
                filterDTO.itemDescription(),
                filterDTO.minMileage(),
                filterDTO.maxMileage(),
                filterDTO.search(),
                filterDTO.transactionalDocumentId(),
                Boolean.TRUE.equals(filterDTO.unlinked()),
                pageable
        );

        return repairs.map(repairMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RepairResponseDTO getRepairById(Long id) {
        Repair repair = repairRepository.findById(id)
                .orElseThrow(() -> new RepairNotFoundException(id));

        return repairMapper.toResponseDto(repair);
    }

    @Override
    @Transactional
    public RepairResponseDTO updateRepair(Long id, RepairDTO repairDTO) {
        Repair existingRepair = repairRepository.findById(id)
                .orElseThrow(() -> new RepairNotFoundException(id));

        // Collect old document IDs from items before update
        Set<Long> oldDocIds = existingRepair.getItems().stream()
                .filter(i -> i.getTransactionalDocument() != null)
                .map(i -> i.getTransactionalDocument().getId())
                .collect(Collectors.toSet());

        if (repairDTO.vehicleId() != null) {
            validateVehicleExists(repairDTO.vehicleId());
        }
        if (repairDTO.supplierId() != null) {
            validateSupplierExists(repairDTO.supplierId());
        }

        repairMapper.partialUpdate(repairDTO, existingRepair);

        if (repairDTO.items() != null) {
            applyItems(existingRepair, repairDTO.items());
        }

        Repair updatedRepair = repairRepository.save(existingRepair);

        // Recalculate old documents that may have lost items
        Set<Long> newDocIds = updatedRepair.getItems().stream()
                .filter(i -> i.getTransactionalDocument() != null)
                .map(i -> i.getTransactionalDocument().getId())
                .collect(Collectors.toSet());
        oldDocIds.stream()
                .filter(docId -> !newDocIds.contains(docId))
                .forEach(documentTotalRecalculator::recalculateAndRecover);
        recalculateItemDocuments(updatedRepair);

        return repairMapper.toResponseDto(updatedRepair);
    }

    @Override
    @Transactional
    public void deleteRepair(Long id) {
        Repair repair = repairRepository.findById(id)
                .orElseThrow(() -> new RepairNotFoundException(id));

        Set<Long> docIds = repair.getItems().stream()
                .filter(i -> i.getTransactionalDocument() != null)
                .map(i -> i.getTransactionalDocument().getId())
                .collect(Collectors.toSet());

        repairRepository.delete(repair);
        docIds.forEach(documentTotalRecalculator::recalculateAndRecover);
    }

    @Override
    @Transactional
    public void linkItemToDocument(Long itemId, Long documentId) {
        linkItemToDocument(itemId, documentId, null, null);
    }

    @Override
    @Transactional
    public void linkItemToDocument(Long itemId, Long documentId, java.math.BigDecimal ivaPercentage, Integer sortOrder) {
        RepairItem item = repairItemRepository.findById(itemId)
                .orElseThrow(() -> new RepairNotFoundException(itemId));
        TransactionalDocument doc = resolveDocument(documentId);
        item.setTransactionalDocument(doc);
        if (ivaPercentage != null) {
            item.setIvaPercentage(ivaPercentage);
        }
        if (sortOrder != null) {
            item.setSortOrder(sortOrder);
        }
        repairItemRepository.save(item);
        documentTotalRecalculator.recalculateAndRecover(documentId);
    }

    @Override
    @Transactional
    public void unlinkItem(Long itemId) {
        RepairItem item = repairItemRepository.findById(itemId)
                .orElseThrow(() -> new RepairNotFoundException(itemId));
        Long oldDocId = item.getTransactionalDocument() != null
                ? item.getTransactionalDocument().getId() : null;
        item.setTransactionalDocument(null);
        repairItemRepository.save(item);
        if (oldDocId != null) {
            documentTotalRecalculator.recalculateAndRecover(oldDocId);
        }
    }

    @Override
    @Transactional
    public void updateItemAmount(Long itemId, java.math.BigDecimal amount) {
        updateItemAmount(itemId, amount, null);
    }

    @Override
    @Transactional
    public void updateItemAmount(Long itemId, java.math.BigDecimal amount, java.math.BigDecimal quantity) {
        RepairItem item = repairItemRepository.findById(itemId)
                .orElseThrow(() -> new RepairNotFoundException(itemId));
        if (amount != null) item.setAmount(amount);
        if (quantity != null) item.setQuantity(quantity);
        repairItemRepository.save(item);
        if (item.getTransactionalDocument() != null) {
            documentTotalRecalculator.recalculateAndRecover(item.getTransactionalDocument().getId());
        }
    }

    /**
     * Applies items from DTOs to the repair entity, handling orphan removal.
     */
    public void applyItems(Repair repair, List<RepairItemDTO> itemDTOs) {
        if (itemDTOs == null) return;

        repair.getItems().clear();

        int sortOrder = 0;
        for (RepairItemDTO itemDTO : itemDTOs) {
            RepairItem item = RepairItem.builder()
                    .repair(repair)
                    .itemType(itemDTO.itemType())
                    .description(itemDTO.description())
                    .amount(itemDTO.amount())
                    .quantity(itemDTO.quantity() != null
                            ? itemDTO.quantity()
                            : java.math.BigDecimal.ONE)
                    .ivaPercentage(itemDTO.ivaPercentage() != null
                            ? itemDTO.ivaPercentage()
                            : new java.math.BigDecimal("21.00"))
                    .transactionalDocument(resolveDocument(itemDTO.transactionalDocumentId()))
                    .sortOrder(sortOrder++)
                    .build();
            repair.getItems().add(item);
        }
    }

    private void recalculateItemDocuments(Repair repair) {
        repair.getItems().stream()
                .filter(i -> i.getTransactionalDocument() != null)
                .map(i -> i.getTransactionalDocument().getId())
                .distinct()
                .forEach(documentTotalRecalculator::recalculateAndRecover);
    }

    private void validateVehicleExists(Long vehicleId) {
        if (!vehicleRepository.existsByIdAndDeletedFalse(vehicleId)) {
            throw new VehicleNotValidException(vehicleId);
        }
    }

    private void validateSupplierExists(Long supplierId) {
        if (!supplierRepository.existsByIdAndDeletedFalse(supplierId)) {
            throw new SupplierNotValidException(supplierId);
        }
    }

    private TransactionalDocument resolveDocument(Long documentId) {
        if (documentId == null) return null;
        return transactionalDocumentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new TransactionalDocumentNotFoundException(documentId));
    }

}
