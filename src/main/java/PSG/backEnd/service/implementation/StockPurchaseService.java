package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.stock.StockNotFoundException;
import PSG.backEnd.exception.stockPurchase.StockPurchaseNotFoundException;
import PSG.backEnd.model.dto.batch.BatchResponseDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseBatchDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseFilterDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseResponseDTO;
import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.entity.StockPurchase;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.mapper.StockPurchaseMapper;
import PSG.backEnd.repository.StockPurchaseRepository;
import PSG.backEnd.repository.StockRepository;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import PSG.backEnd.service.port.IStockPurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockPurchaseService implements IStockPurchaseService {

    private final StockPurchaseRepository stockPurchaseRepository;
    private final StockPurchaseMapper stockPurchaseMapper;
    private final StockRepository stockRepository;
    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final DocumentTotalRecalculator documentTotalRecalculator;
    private final BatchProcessor batchProcessor;

    /** Loads the linked document (or null) for response enrichment. */
    private TransactionalDocument loadLinkedDocument(Long id) {
        if (id == null) return null;
        return transactionalDocumentRepository.findByIdAndDeletedFalse(id).orElse(null);
    }

    @Override
    @Transactional
    public StockPurchaseResponseDTO createStockPurchase(StockPurchaseDTO dto) {
        Stock stock = stockRepository.findByIdAndDeletedFalse(dto.stockId())
                .orElseThrow(() -> new StockNotFoundException(dto.stockId()));

        StockPurchase purchase = stockPurchaseMapper.toEntity(dto);

        // Compute totalAmount if not provided
        if (purchase.getTotalAmount() == null && purchase.getUnitPrice() != null) {
            purchase.setTotalAmount(purchase.getUnitPrice().multiply(purchase.getQuantity()));
        }

        StockPurchase saved = stockPurchaseRepository.save(purchase);

        // Increase stock quantity
        stock.setQuantity(stock.getQuantity().add(dto.quantity()));
        stockRepository.save(stock);

        documentTotalRecalculator.recalculateDocumentTotals(dto.transactionalDocumentId());
        return stockPurchaseMapper.toResponseDto(saved, stock, loadLinkedDocument(saved.getTransactionalDocumentId()));
    }

    @Override
    public BatchResponseDTO<StockPurchaseResponseDTO> createBatchStockPurchases(StockPurchaseBatchDTO batchDTO) {
        return batchProcessor.process(batchDTO.purchases(), this::createStockPurchase);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockPurchaseResponseDTO> getAllStockPurchases(StockPurchaseFilterDTO filterDTO, Pageable pageable) {
        Page<StockPurchase> purchases = stockPurchaseRepository.findAllWithFilters(
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.stockId(),
                filterDTO.stockName(),
                filterDTO.stockCategory(),
                filterDTO.minQuantity(),
                filterDTO.maxQuantity(),
                filterDTO.minAmount(),
                filterDTO.maxAmount(),
                filterDTO.transactionalDocumentId(),
                filterDTO.search(),
                Boolean.TRUE.equals(filterDTO.unlinked()),
                pageable
        );

        return purchases.map(purchase -> {
            Stock stock = stockRepository.findById(purchase.getStockId()).orElse(null);
            TransactionalDocument doc = loadLinkedDocument(purchase.getTransactionalDocumentId());
            return stockPurchaseMapper.toResponseDto(purchase, stock, doc);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public StockPurchaseResponseDTO getStockPurchaseById(Long id) {
        StockPurchase purchase = stockPurchaseRepository.findById(id)
                .orElseThrow(() -> new StockPurchaseNotFoundException(id));

        Stock stock = stockRepository.findById(purchase.getStockId())
                .orElseThrow(() -> new StockNotFoundException(purchase.getStockId()));

        return stockPurchaseMapper.toResponseDto(purchase, stock, loadLinkedDocument(purchase.getTransactionalDocumentId()));
    }

    @Override
    @Transactional
    public StockPurchaseResponseDTO updateStockPurchase(Long id, StockPurchaseDTO dto) {
        StockPurchase existing = stockPurchaseRepository.findById(id)
                .orElseThrow(() -> new StockPurchaseNotFoundException(id));

        BigDecimal oldQuantity = existing.getQuantity();
        Long oldDocumentId = existing.getTransactionalDocumentId();

        if (dto.stockId() != null) {
            stockRepository.findByIdAndDeletedFalse(dto.stockId())
                    .orElseThrow(() -> new StockNotFoundException(dto.stockId()));
        }

        stockPurchaseMapper.partialUpdate(dto, existing);

        // Handle transactionalDocumentId explicitly (mapper ignores it to support unlinking via null).
        // The stock purchase form always includes this field; null means "no document linked".
        existing.setTransactionalDocumentId(dto.transactionalDocumentId());

        // Recompute totalAmount if unitPrice or quantity changed
        if (existing.getUnitPrice() != null && existing.getQuantity() != null) {
            existing.setTotalAmount(existing.getUnitPrice().multiply(existing.getQuantity()));
        }

        StockPurchase updated = stockPurchaseRepository.save(existing);

        // Adjust stock quantity: remove old, add new
        if (dto.quantity() != null) {
            Stock stock = stockRepository.findByIdAndDeletedFalse(updated.getStockId())
                    .orElseThrow(() -> new StockNotFoundException(updated.getStockId()));
            BigDecimal adjustment = dto.quantity().subtract(oldQuantity);
            stock.setQuantity(stock.getQuantity().add(adjustment));
            stockRepository.save(stock);
        }

        // Recalculate old document if the link changed
        if (oldDocumentId != null && !oldDocumentId.equals(dto.transactionalDocumentId())) {
            documentTotalRecalculator.recalculateDocumentTotals(oldDocumentId);
        }
        documentTotalRecalculator.recalculateDocumentTotals(dto.transactionalDocumentId());

        Stock stock = stockRepository.findById(updated.getStockId()).orElse(null);
        return stockPurchaseMapper.toResponseDto(updated, stock, loadLinkedDocument(updated.getTransactionalDocumentId()));
    }

    @Override
    @Transactional
    public void deleteStockPurchase(Long id) {
        StockPurchase purchase = stockPurchaseRepository.findById(id)
                .orElseThrow(() -> new StockPurchaseNotFoundException(id));

        Long docId = purchase.getTransactionalDocumentId();

        // Reverse the stock quantity increase
        stockRepository.findByIdAndDeletedFalse(purchase.getStockId()).ifPresent(stock -> {
            BigDecimal newQty = stock.getQuantity().subtract(purchase.getQuantity());
            stock.setQuantity(newQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newQty);
            stockRepository.save(stock);
        });

        stockPurchaseRepository.delete(purchase);
        documentTotalRecalculator.recalculateDocumentTotals(docId);
    }
}
