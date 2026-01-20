package PSG.backEnd.model.dto.report;

import PSG.backEnd.model.enums.MoneyOutflowCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for filtering money outflow reports.
 * Contains all available filter criteria for generating customized reports.
 */
@Schema(description = "Filter criteria for money outflow reports. All fields are optional and can be combined.")
public record ReportFilterDTO(

    @Schema(description = "Start date for the report period (inclusive). If not provided, will include all records from the beginning.",
            example = "2024-01-01")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,

    @Schema(description = "End date for the report period (inclusive). If not provided, will include all records until today.",
            example = "2024-12-31")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate,

    @Schema(description = "List of categories to filter by. If not provided or empty, will include all categories. Can specify multiple categories.",
            example = "[\"PAYMENT\", \"SALARY\"]",
            allowableValues = {"INVOICE", "PAYMENT", "SALARY", "SERVICE", "LICENCE_PLATE", "FUEL", "INSURANCE", "REPAIR"})
    List<MoneyOutflowCategory> categories,

    @Schema(description = "Minimum amount to filter by (inclusive). Must be greater than zero.",
            example = "1000.00")
    @DecimalMin(value = "0.01", message = "{validation.positive}")
    @Digits(integer = 10, fraction = 2, message = "{validation.pattern}")
    BigDecimal minAmount,

    @Schema(description = "Maximum amount to filter by (inclusive). Must be greater than zero.",
            example = "50000.00")
    @DecimalMin(value = "0.01", message = "{validation.positive}")
    @Digits(integer = 10, fraction = 2, message = "{validation.pattern}")
    BigDecimal maxAmount,

    @Schema(description = "Field to sort by. Default is 'date'.",
            example = "date",
            allowableValues = {"date", "amount", "category"})
    String sortBy,

    @Schema(description = "Sort order: 'asc' for ascending, 'desc' for descending. Default is 'desc'.",
            example = "desc",
            allowableValues = {"asc", "desc"})
    String sortOrder
) {

    /**
     * Constructor with default values for optional fields
     */
    public ReportFilterDTO {
        // Set default sort by date if not provided
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "date";
        }

        // Set default sort order to descending if not provided
        if (sortOrder == null || sortOrder.isBlank()) {
            sortOrder = "desc";
        }
    }

    /**
     * Validates that minAmount is not greater than maxAmount if both are provided
     */
    public boolean isValidAmountRange() {
        if (minAmount != null && maxAmount != null) {
            return minAmount.compareTo(maxAmount) <= 0;
        }
        return true;
    }

    /**
     * Validates that startDate is not after endDate if both are provided
     */
    public boolean isValidDateRange() {
        if (startDate != null && endDate != null) {
            return !startDate.isAfter(endDate);
        }
        return true;
    }
}

