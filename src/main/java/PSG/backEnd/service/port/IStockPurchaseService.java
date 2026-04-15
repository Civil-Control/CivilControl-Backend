package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.batch.BatchResponseDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseBatchDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseFilterDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IStockPurchaseService {
    StockPurchaseResponseDTO createStockPurchase(StockPurchaseDTO dto);
    BatchResponseDTO<StockPurchaseResponseDTO> createBatchStockPurchases(StockPurchaseBatchDTO batchDTO);
    StockPurchaseResponseDTO getStockPurchaseById(Long id);
    StockPurchaseResponseDTO updateStockPurchase(Long id, StockPurchaseDTO dto);
    void deleteStockPurchase(Long id);
    Page<StockPurchaseResponseDTO> getAllStockPurchases(StockPurchaseFilterDTO filterDTO, Pageable pageable);
}
