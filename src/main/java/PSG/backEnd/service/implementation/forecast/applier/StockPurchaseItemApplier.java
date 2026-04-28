package PSG.backEnd.service.implementation.forecast.applier;

import PSG.backEnd.exception.forecast.BudgetForecastApplyException;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseDTO;
import PSG.backEnd.model.dto.stockPurchase.StockPurchaseResponseDTO;
import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import PSG.backEnd.service.port.IStockPurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Materializa un item COMPRA_STOCK en un {@code StockPurchase}. Calcula
 * unitPrice = expectedAmount / stockQuantity (sin documento transaccional).
 */
@Component
@RequiredArgsConstructor
public class StockPurchaseItemApplier implements BudgetForecastItemApplier {

    public static final String ENTITY_TYPE = "StockPurchase";

    private final IStockPurchaseService stockPurchaseService;

    @Override
    public BudgetForecastItemType supportedType() {
        return BudgetForecastItemType.COMPRA_STOCK;
    }

    @Override
    public void validate(BudgetForecastItem item) {
        if (item.getStock() == null) {
            throw new BudgetForecastApplyException("Item COMPRA_STOCK requiere artículo de stock.");
        }
        if (item.getStockQuantity() == null || item.getStockQuantity().signum() <= 0) {
            throw new BudgetForecastApplyException("Item COMPRA_STOCK requiere cantidad > 0.");
        }
        if (item.getExpectedAmount() == null || item.getExpectedDate() == null) {
            throw new BudgetForecastApplyException("Item COMPRA_STOCK requiere monto y fecha esperados.");
        }
    }

    @Override
    public AppliedEntityRef apply(BudgetForecastItem item) {
        validate(item);
        BigDecimal qty = item.getStockQuantity().setScale(2, RoundingMode.HALF_UP);
        BigDecimal unitPrice = item.getExpectedAmount().divide(qty, 2, RoundingMode.HALF_UP);
        StockPurchaseDTO dto = new StockPurchaseDTO(
                item.getExpectedDate(),
                item.getStock().getId(),
                qty,
                unitPrice,
                item.getExpectedAmount(),
                "Generado desde previsión #" + item.getBudgetForecast().getId(),
                null,                       // transactionalDocumentId
                new BigDecimal("21.00"),    // ivaPercentage
                null                        // documentSortOrder
        );
        StockPurchaseResponseDTO created = stockPurchaseService.createStockPurchase(dto);
        return AppliedEntityRef.of(ENTITY_TYPE, created.id());
    }

    @Override
    public void revert(BudgetForecastItem item) {
        if (item.getAppliedEntityId() == null) return;
        stockPurchaseService.deleteStockPurchase(item.getAppliedEntityId());
    }
}
