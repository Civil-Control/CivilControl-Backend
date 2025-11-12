package PSG.backEnd.model.dto.report;

import PSG.backEnd.model.enums.MoneyOutflowCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a complete money outflow report.
 * Includes filtered data, summary statistics, and metadata about the report generation.
 */
@Builder
@Schema(description = "Complete money outflow report with data, filters applied, and summary statistics")
public record MoneyOutflowReportDTO(

    @Schema(description = "Filters that were applied to generate this report")
    ReportFilterDTO filters,

    @Schema(description = "List of all money outflow items matching the filter criteria")
    List<ReportItemDTO> items,

    @Schema(description = "Total amount of all outflows in this report",
            example = "1250000.75")
    BigDecimal totalAmount,

    @Schema(description = "Total number of outflow records in this report",
            example = "45")
    Integer totalCount,

    @Schema(description = "Date and time when this report was generated",
            example = "2024-11-05T14:30:00")
    LocalDateTime generatedAt,

    @Schema(description = "Summary of total amounts grouped by category")
    Map<MoneyOutflowCategory, BigDecimal> summaryByCategory,

    @Schema(description = "Name of the report for display purposes",
            example = "Reporte de Salidas de Dinero")
    String reportName,

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
     * Returns the average amount per outflow
     */
    public BigDecimal getAverageAmount() {
        if (totalCount == null || totalCount == 0) {
            return BigDecimal.ZERO;
        }
        return totalAmount.divide(BigDecimal.valueOf(totalCount), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Checks if the report has any data
     */
    public boolean hasData() {
        return items != null && !items.isEmpty();
    }
}

