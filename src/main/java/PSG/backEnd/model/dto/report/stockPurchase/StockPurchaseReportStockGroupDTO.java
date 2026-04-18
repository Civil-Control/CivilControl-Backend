package PSG.backEnd.model.dto.report.stockPurchase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Schema(description = "Grouping of stock purchases by stock item within a category (layer 2)")
public record StockPurchaseReportStockGroupDTO(

    @Schema(description = "Stock item ID")
    Long stockId,

    @Schema(description = "Stock item name")
    String stockName,

    @Schema(description = "Total amount for this stock item")
    BigDecimal totalAmount,

    @Schema(description = "Number of purchases for this stock item")
    int purchaseCount,

    @Schema(description = "Total quantity purchased for this stock item")
    BigDecimal totalQuantity,

    @Schema(description = "Individual purchases for this stock item")
    List<StockPurchaseReportItemDTO> purchases
) {}
