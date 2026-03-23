package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.stock.StockAlreadyExistsException;
import PSG.backEnd.exception.stock.StockNotFoundException;
import PSG.backEnd.exception.stock.StockNotValidException;
import PSG.backEnd.model.dto.stock.StockDTO;
import PSG.backEnd.model.dto.stock.StockFilterDTO;
import PSG.backEnd.model.dto.stock.StockResponseDTO;
import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.mapper.StockMapper;
import PSG.backEnd.repository.StockRepository;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotFoundException;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import PSG.backEnd.service.port.IBuildingService;
import PSG.backEnd.service.port.IStockService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockService implements IStockService {

    private final StockRepository stockRepository;
    private final StockMapper stockMapper;
    private final IBuildingService buildingService;
    private final MessageSourceHelper messageSourceHelper;
    private final TransactionalDocumentRepository transactionalDocumentRepository;

    @Override
    @Transactional
    public StockResponseDTO createStock(StockDTO stockDTO) {
        validateNewStock(stockDTO);
        Optional<Stock> deletedStock = findDeletedStock(stockDTO);

        if (deletedStock.isPresent()) {
            return reactivateStock(deletedStock.get(), stockDTO);
        }

        return createNewStock(stockDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockResponseDTO> getAllStocks(StockFilterDTO filterDTO, Pageable pageable) {
        return stockRepository.findAllWithFilters(
                filterDTO.name(),
                filterDTO.buildingId(),
                filterDTO.stockCategory(),
                filterDTO.minQuantity(),
                filterDTO.maxQuantity(),
                filterDTO.search(),
                filterDTO.transactionalDocumentId(),
                pageable
        ).map(stockMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponseDTO getStockById(Long id) {
        return stockRepository.findByIdAndDeletedFalse(id)
                .map(stockMapper::toResponseDto)
                .orElseThrow(() -> new StockNotFoundException(id));
    }

    @Override
    @Transactional
    public StockResponseDTO updateStock(Long id, StockDTO stockDTO) {
        Stock existingStock = stockRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new StockNotFoundException(id));

        validateStockUpdate(id, stockDTO);
        stockMapper.partialUpdate(stockDTO, existingStock);

        // Update building if provided
        if (stockDTO.buildingId() != null) {
            Building building = buildingService.getEntityById(stockDTO.buildingId());
            existingStock.setBuilding(building);
        }

        existingStock.setTransactionalDocument(resolveDocument(stockDTO.transactionalDocumentId()));
        Stock updatedStock = stockRepository.save(existingStock);
        return stockMapper.toResponseDto(updatedStock);
    }

    @Override
    @Transactional
    public void updateStockQuantity(Long stockId, BigDecimal quantity) {
        if (quantity == null) {
            throw new StockNotValidException(messageSourceHelper.getMessage("stock.quantity.null"));
        }
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new StockNotValidException(messageSourceHelper.getMessage("stock.quantity.negative"));
        }

        Stock stock = stockRepository.findByIdAndDeletedFalse(stockId)
                .orElseThrow(() -> new StockNotFoundException(stockId));
        stock.setQuantity(quantity);
        stockRepository.save(stock);
    }

    @Override
    @Transactional
    public void deleteStock(Long id) {
        Stock stock = stockRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new StockNotFoundException(id));

        stock.setDeleted(true);
        stockRepository.save(stock);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return stockRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Stock getEntityById(Long id) {
        return stockRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new StockNotFoundException(id));
    }

    // Private validation methods
    private void validateNewStock(StockDTO stockDTO) {
        if (stockDTO.name() == null || stockDTO.name().trim().isEmpty()) {
            throw new StockNotValidException(messageSourceHelper.getMessage("stock.name.empty"));
        }
        if (stockRepository.existsByNameAndDeletedFalse(stockDTO.name())) {
            throw new StockAlreadyExistsException(messageSourceHelper.getMessage("stock.name.alreadyExists", stockDTO.name()));
        }
        if (stockDTO.quantity() == null) {
            throw new StockNotValidException(messageSourceHelper.getMessage("stock.quantity.null"));
        }
        if (stockDTO.quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new StockNotValidException(messageSourceHelper.getMessage("stock.quantity.negative"));
        }
        if (stockDTO.stockCategory() == null) {
            throw new StockNotValidException(messageSourceHelper.getMessage("stock.stockCategory.null"));
        }
        if (stockDTO.buildingId() == null) {
            throw new StockNotValidException(messageSourceHelper.getMessage("stock.buildingId.null"));
        }
        // Validate that building exists and is active
        if (!buildingService.existsById(stockDTO.buildingId())) {
            throw new StockNotValidException(messageSourceHelper.getMessage("stock.buildingId.notFound", stockDTO.buildingId()));
        }
    }

    private void validateStockUpdate(Long id, StockDTO stockDTO) {
        if (stockDTO.name() != null && !stockDTO.name().trim().isEmpty()) {
            Optional<Stock> existingWithSameName = stockRepository.findByIdAndDeletedFalse(id);
            if (existingWithSameName.isPresent() && !existingWithSameName.get().getId().equals(id)) {
                if (stockRepository.existsByNameAndDeletedFalse(stockDTO.name())) {
                    throw new StockAlreadyExistsException(messageSourceHelper.getMessage("stock.name.alreadyExists", stockDTO.name()));
                }
            }
        }
        if (stockDTO.quantity() != null && stockDTO.quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new StockNotValidException(messageSourceHelper.getMessage("stock.quantity.negative"));
        }
        // Validate building if provided
        if (stockDTO.buildingId() != null) {
            if (!buildingService.existsById(stockDTO.buildingId())) {
                throw new StockNotValidException(messageSourceHelper.getMessage("stock.buildingId.notFound", stockDTO.buildingId()));
            }
        }
    }

    private Optional<Stock> findDeletedStock(StockDTO stockDTO) {
        return stockRepository.findByNameAndDeletedTrue(stockDTO.name());
    }

    private StockResponseDTO reactivateStock(Stock stock, StockDTO stockDTO) {
        stockMapper.partialUpdate(stockDTO, stock);
        stock.setDeleted(false);
        // Update building if provided
        if (stockDTO.buildingId() != null) {
            Building building = buildingService.getEntityById(stockDTO.buildingId());
            stock.setBuilding(building);
        }
        stock.setTransactionalDocument(resolveDocument(stockDTO.transactionalDocumentId()));
        return stockMapper.toResponseDto(stockRepository.save(stock));
    }

    private StockResponseDTO createNewStock(StockDTO stockDTO) {
        Stock stock = stockMapper.toEntity(stockDTO);
        stock.setDeleted(false);
        // Set building relationship
        Building building = buildingService.getEntityById(stockDTO.buildingId());
        stock.setBuilding(building);
        stock.setTransactionalDocument(resolveDocument(stockDTO.transactionalDocumentId()));
        return stockMapper.toResponseDto(stockRepository.save(stock));
    }

    private TransactionalDocument resolveDocument(Long documentId) {
        if (documentId == null) return null;
        return transactionalDocumentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new TransactionalDocumentNotFoundException(documentId));
    }
}

