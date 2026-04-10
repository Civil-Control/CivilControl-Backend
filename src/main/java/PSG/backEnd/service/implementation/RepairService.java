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
                filterDTO.vehicleId(),
                filterDTO.vehicleLicensePlate(),
                filterDTO.projectAreaId(),
                filterDTO.minCost(),
                filterDTO.maxCost(),
                filterDTO.supplierId(),
                filterDTO.supplierLegalName(),
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
                .forEach(documentTotalRecalculator::recalculateDocumentTotals);
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
        docIds.forEach(documentTotalRecalculator::recalculateDocumentTotals);
    }

    @Override
    @Transactional
    public RepairResponseDTO linkToDocument(Long repairId, Long documentId) {
        Repair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new RepairNotFoundException(repairId));
        TransactionalDocument doc = resolveDocument(documentId);
        repair.getItems().forEach(item -> item.setTransactionalDocument(doc));
        Repair saved = repairRepository.save(repair);
        recalculateItemDocuments(saved);
        return repairMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public RepairResponseDTO unlinkFromDocument(Long repairId) {
        Repair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new RepairNotFoundException(repairId));
        Set<Long> oldDocIds = repair.getItems().stream()
                .filter(i -> i.getTransactionalDocument() != null)
                .map(i -> i.getTransactionalDocument().getId())
                .collect(Collectors.toSet());
        repair.getItems().forEach(item -> item.setTransactionalDocument(null));
        repairRepository.save(repair);
        oldDocIds.forEach(documentTotalRecalculator::recalculateDocumentTotals);
        return repairMapper.toResponseDto(repair);
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
                .forEach(documentTotalRecalculator::recalculateDocumentTotals);
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
