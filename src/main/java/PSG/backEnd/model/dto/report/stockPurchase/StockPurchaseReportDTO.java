package PSG.backEnd.model.dto.report.stockPurchase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Root DTO for the stock purchase report")
public record StockPurchaseReportDTO(

    @Schema(description = "Filters used to generate this report")
    StockPurchaseReportFilterDTO filters,

    @Schema(description = "Category groups (layer 1)")
    List<StockPurchaseReportCategoryGroupDTO> categoryGroups,

    @Schema(description = "Grand total amount")
    BigDecimal totalAmount,

    @Schema(description = "Total number of purchases")
    int totalCount,

    @Schema(description = "Total quantity purchased across all items")
    BigDecimal totalQuantity,

    @Schema(description = "Totals by category display name")
    Map<String, BigDecimal> totalsByCategory,

    @Schema(description = "Report generation timestamp")
    LocalDateTime generatedAt,

    @Schema(description = "Report name")
    String reportName,

    @Schema(description = "Human-readable period description")
    String periodDescription
) {}
