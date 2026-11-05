package PSG.backEnd.model.dto.report.stockPurchase;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filters for the stock purchase report")
public record StockPurchaseReportFilterDTO(

    @Schema(description = "Start date (inclusive)")
    LocalDate startDate,

    @Schema(description = "End date (inclusive)")
    LocalDate endDate,

    @Schema(description = "Filter by stock category enum names")
    List<String> stockCategories,

    @Schema(description = "Filter by specific stock item ID")
    Long stockId,

    @Schema(description = "Minimum amount (inclusive)")
    BigDecimal minAmount,

    @Schema(description = "Maximum amount (inclusive)")
    BigDecimal maxAmount
) {}
