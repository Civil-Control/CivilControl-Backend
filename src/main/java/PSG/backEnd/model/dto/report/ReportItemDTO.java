package PSG.backEnd.model.dto.report;

import PSG.backEnd.model.enums.MoneyOutflowCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO representing a single money outflow item in a report.
 * Contains all relevant information about a specific payment or expense.
 */
@Builder
@Schema(description = "Represents a single money outflow entry in the report")
public record ReportItemDTO(

    @Schema(description = "Unique identifier of the outflow record",
            example = "123")
    Long id,

    @Schema(description = "Date when the outflow occurred",
            example = "2024-10-15")
    LocalDate date,

    @Schema(description = "Category of the money outflow",
            example = "PAYMENT",
            allowableValues = {"PAYMENT", "SALARY", "SERVICE", "LICENCE_PLATE", "FUEL", "INSURANCE", "REPAIR"})
    MoneyOutflowCategory category,

    @Schema(description = "Detailed description of the outflow",
            example = "Pago a proveedor ABC S.A. - Factura #001-00123456")
    String description,

    @Schema(description = "Amount of money that went out",
            example = "15000.50")
    BigDecimal amount,

    @Schema(description = "Payment method used (if applicable)",
            example = "TRANSFERENCIA",
            nullable = true)
    String paymentMethod,

    @Schema(description = "Name of the beneficiary or recipient",
            example = "ABC S.A.",
            nullable = true)
    String beneficiary,

    @Schema(description = "Reference number or document identifier",
            example = "001-00123456",
            nullable = true)
    String reference,

    @Schema(description = "Additional comments or notes",
            example = "Pago parcial de factura",
            nullable = true)
    String comment
) {

    /**
     * Returns a formatted display string for the amount
     */
    public String getFormattedAmount() {
        return String.format("$ %.2f", amount);
    }

    /**
     * Returns the category display name
     */
    public String getCategoryDisplayName() {
        return category.getDisplayName();
    }
}

