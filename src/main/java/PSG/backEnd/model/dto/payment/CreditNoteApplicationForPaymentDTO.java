package PSG.backEnd.model.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Credit note application from the payment form: links a credit note to a specific invoice.")
public record CreditNoteApplicationForPaymentDTO(

    @Schema(description = "ID of the credit note to apply.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}")
    Long creditNoteId,

    @Schema(description = "ID of the invoice or debit note to apply the credit against.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}")
    Long invoiceId,

    @Schema(description = "Amount of the credit note to apply. Must be positive and within available balance.",
            example = "10000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0.01", message = "{validation.positive}")
    @Digits(integer = 17, fraction = 2, message = "{validation.pattern}")
    BigDecimal amountApplied
) {}
