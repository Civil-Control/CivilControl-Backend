package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.gasStation.FuelLoadNotFoundException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotFoundException;
import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadFilterDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadResponseDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.mapper.FuelLoadMapper;
import PSG.backEnd.repository.FuelLoadRepository;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import PSG.backEnd.service.implementation.fuelload.FuelLoadFactory;
import PSG.backEnd.service.implementation.fuelload.FuelLoadBatchProcessor;
import PSG.backEnd.service.port.IFuelLoadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FuelLoadService implements IFuelLoadService {

    private final FuelLoadRepository fuelLoadRepository;
    private final FuelLoadMapper fuelLoadMapper;
    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final FuelLoadFactory fuelLoadFactory;
    private final FuelLoadBatchProcessor batchProcessor;
    private final DocumentTotalRecalculator documentTotalRecalculator;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public FuelLoadResponseDTO createFuelLoad(FuelLoadDTO fuelLoadDTO) {
        FuelLoad fuelLoad = fuelLoadFactory.createFuelLoad(fuelLoadDTO);
        fuelLoad.setTransactionalDocument(resolveDocument(fuelLoadDTO.transactionalDocumentId()));
        FuelLoad savedFuelLoad = fuelLoadRepository.save(fuelLoad);
        entityManager.refresh(savedFuelLoad);
        documentTotalRecalculator.recalculateDocumentTotals(fuelLoadDTO.transactionalDocumentId());
        return fuelLoadMapper.toResponseDto(savedFuelLoad);
    }

    @Override
    public FuelLoadBatchResponseDTO createFuelLoadBatch(FuelLoadBatchDTO fuelLoadBatchDTO) {
        return batchProcessor.processBatch(fuelLoadBatchDTO, this::createFuelLoad);
    }

    @Override
    @Transactional(readOnly = true)
    public FuelLoadResponseDTO getFuelLoadById(Long id) {
        FuelLoad fuelLoad = findFuelLoadById(id);
        return fuelLoadMapper.toResponseDto(fuelLoad);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FuelLoadResponseDTO> getAllFuelLoads(FuelLoadFilterDTO filterDTO, Pageable pageable) {
        Page<FuelLoad> fuelLoads = fuelLoadRepository.findAllWithFilters(
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.branchCode(),
                filterDTO.ticketNumber(),
                filterDTO.fuelType(),
                filterDTO.vehicleId(),
                filterDTO.vehicleLicensePlate(),
                filterDTO.projectAreaId(),
                filterDTO.projectAreaName(),
                filterDTO.gasStationId(),
                filterDTO.search(),
                filterDTO.gasStationName(),
                filterDTO.totalAmountMin(),
                filterDTO.totalAmountMax(),
                filterDTO.transactionalDocumentId(),
                pageable
        );

        return fuelLoads.map(fuelLoadMapper::toResponseDto);
    }

    @Override
    public FuelLoadResponseDTO updateFuelLoad(Long id, FuelLoadDTO fuelLoadDTO) {
        FuelLoad existingFuelLoad = findFuelLoadById(id);
        Long oldDocumentId = existingFuelLoad.getTransactionalDocument() != null
                ? existingFuelLoad.getTransactionalDocument().getId() : null;
        fuelLoadFactory.updateFuelLoad(existingFuelLoad, fuelLoadDTO);
        existingFuelLoad.setTransactionalDocument(resolveDocument(fuelLoadDTO.transactionalDocumentId()));
        FuelLoad updatedFuelLoad = fuelLoadRepository.save(existingFuelLoad);
        entityManager.refresh(updatedFuelLoad);
        // Recalculate old document if the link changed
        if (oldDocumentId != null && !oldDocumentId.equals(fuelLoadDTO.transactionalDocumentId())) {
            documentTotalRecalculator.recalculateDocumentTotals(oldDocumentId);
        }
        documentTotalRecalculator.recalculateDocumentTotals(fuelLoadDTO.transactionalDocumentId());
        return fuelLoadMapper.toResponseDto(updatedFuelLoad);
    }

    @Override
    public void deleteFuelLoad(Long id) {
        FuelLoad fuelLoad = findFuelLoadById(id);
        Long docId = fuelLoad.getTransactionalDocument() != null
                ? fuelLoad.getTransactionalDocument().getId() : null;
        fuelLoadRepository.delete(fuelLoad);
        documentTotalRecalculator.recalculateDocumentTotals(docId);
    }

    /**
     * Private method that encapsulates search and exception handling.
     * Follows the DRY (Don't Repeat Yourself) principle.
     */
    private FuelLoad findFuelLoadById(Long id) {
        return fuelLoadRepository.findById(id)
                .orElseThrow(() -> new FuelLoadNotFoundException(id));
    }

    private TransactionalDocument resolveDocument(Long documentId) {
        if (documentId == null) return null;
        return transactionalDocumentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new TransactionalDocumentNotFoundException(documentId));
    }
}
