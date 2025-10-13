package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.stock.StockDTO;
import PSG.backEnd.model.dto.stock.StockFilterDTO;
import PSG.backEnd.model.dto.stock.StockResponseDTO;
import PSG.backEnd.model.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface IStockService {
    StockResponseDTO createStock(StockDTO stockDTO);
    StockResponseDTO getStockById(Long id);
    StockResponseDTO updateStock(Long id, StockDTO stockDTO);
    void updateStockQuantity(Long id, BigDecimal quantity);
    void deleteStock(Long id);
    Page<StockResponseDTO> getAllStocks(StockFilterDTO filterDTO, Pageable pageable);
    Stock getEntityById(Long id);
    boolean existsById(Long id);
}

