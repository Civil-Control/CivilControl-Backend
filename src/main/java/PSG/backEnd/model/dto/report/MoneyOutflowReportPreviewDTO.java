package PSG.backEnd.model.dto.report;

import PSG.backEnd.model.enums.MoneyOutflowCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO representing a preview/summary of a money outflow report.
 * Contains only statistics and totals without the detailed items list.
 * Useful for quick previews before generating the full report.
 */
@Builder
@Schema(description = "Preview summary of a money outflow report with statistics only (no detailed items)")
public record MoneyOutflowReportPreviewDTO(

    @Schema(description = "Filters that were applied to generate this preview")
    ReportFilterDTO filters,

    @Schema(description = "Total amount of all outflows matching the filters",
            example = "1250000.75")
    BigDecimal totalAmount,

    @Schema(description = "Total number of outflow records matching the filters",
            example = "45")
    Integer totalCount,

    @Schema(description = "Average amount per outflow",
            example = "27777.79")
    BigDecimal averageAmount,

    @Schema(description = "Date and time when this preview was generated",
            example = "2024-11-05T14:30:00")
    LocalDateTime generatedAt,

    @Schema(description = "Summary of total amounts grouped by category")
    Map<MoneyOutflowCategory, BigDecimal> summaryByCategory,

    @Schema(description = "Summary of count of records grouped by category")
    Map<MoneyOutflowCategory, Integer> countByCategory,

    @Schema(description = "Description of the report period",
            example = "Período: 01/01/2024 - 31/12/2024")
    String periodDescription
) {

    /**
     * Returns a formatted display string for the total amount
     */
    public String getFormattedTotalAmount() {
        return String.format("$ %.2f", totalAmount);
    }

    /**
     * Returns a formatted display string for the average amount
     */
    public String getFormattedAverageAmount() {
        return String.format("$ %.2f", averageAmount);
    }

    /**
     * Checks if there is any data in the preview
     */
    public boolean hasData() {
        return totalCount != null && totalCount > 0;
    }
}

