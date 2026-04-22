package PSG.backEnd.model.dto.stockPurchase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockPurchaseFilterDTO(
        LocalDate dateFrom,
        LocalDate dateTo,
        Long stockId,
        String stockName,
        String stockCategory,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Long transactionalDocumentId,
        String search,
        Boolean unlinked
) {}
