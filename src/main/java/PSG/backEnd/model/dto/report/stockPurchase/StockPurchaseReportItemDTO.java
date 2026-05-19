package PSG.backEnd.model.dto.report.stockPurchase;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A single stock purchase entry within the report")
public record StockPurchaseReportItemDTO(

    @Schema(description = "Stock purchase ID")
    Long id,

    @Schema(description = "Date of the purchase")
    LocalDate date,

    @Schema(description = "Stock item name (for flat view)")
    String stockName,

    @Schema(description = "Stock category display name (for flat view)")
    String stockCategoryName,

    @Schema(description = "Quantity purchased")
    BigDecimal quantity,

    @Schema(description = "Unit price", nullable = true)
    BigDecimal unitPrice,

    @Schema(description = "Total amount")
    BigDecimal totalAmount,

    @Schema(description = "IVA percentage applied when linked to a document", nullable = true)
    BigDecimal ivaPercentage,

    @Schema(description = "Total including IVA. Null when not linked to a transactional document.", nullable = true)
    BigDecimal totalWithIva,

    @Schema(description = "Purchase notes", nullable = true)
    String notes,

    @Schema(description = "Whether a transactional document is linked")
    boolean hasLinkedDocument
) {}
