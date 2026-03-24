package PSG.backEnd.model.dto.stockPurchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StockPurchaseBatchDTO(
        @NotEmpty
        @Valid
        List<StockPurchaseDTO> purchases
) {}
