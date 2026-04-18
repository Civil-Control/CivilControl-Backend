package PSG.backEnd.model.dto.report.stockPurchase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Schema(description = "Grouping of stock purchases by category (layer 1)")
public record StockPurchaseReportCategoryGroupDTO(

    @Schema(description = "Category display name", example = "Herramientas manuales")
    String categoryName,

    @Schema(description = "Category enum key", example = "HERRAMIENTAS_MANUALES")
    String categoryKey,

    @Schema(description = "Subtotal amount for this category")
    BigDecimal subtotalAmount,

    @Schema(description = "Number of purchases in this category")
    int purchaseCount,

    @Schema(description = "Total quantity purchased in this category")
    BigDecimal subtotalQuantity,

    @Schema(description = "Stock item groups within this category")
    List<StockPurchaseReportStockGroupDTO> stockGroups
) {}
