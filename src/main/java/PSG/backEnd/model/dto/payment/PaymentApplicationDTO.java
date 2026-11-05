package PSG.backEnd.model.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO describing how much of a payment is imputed to a single transactional document.
 *
 * <p>Used inside {@link PaymentDetailsDTO#applications()} to represent an explicit, per-document
 * distribution of a payment. The sum of all {@code amountApplied} across this list, plus the
 * payment's {@code onAccountAmount}, must equal the payment's total {@code amount}.
 */
@Schema(description = "Per-document imputation of a payment. Specifies how much of the payment " +
        "amount is applied to a particular invoice/debit note.")
public record PaymentApplicationDTO(

        @Schema(description = "ID of the transactional document (invoice or debit note) being paid.",
                example = "123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{paymentApplication.documentId.required}")
        Long documentId,

        @Schema(description = "Portion of the payment amount imputed to this document. " +
                "Must be strictly positive and not exceed the document's outstanding balance " +
                "(total minus credit notes minus previous payments).",
                example = "1500.00",
                minimum = "0.01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{paymentApplication.amountApplied.required}")
        @DecimalMin(value = "0.01", message = "{paymentApplication.amountApplied.positive}")
        @Digits(integer = 17, fraction = 2, message = "{validation.pattern}")
        BigDecimal amountApplied
) {}
