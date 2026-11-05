package PSG.backEnd.model.dto.transactionalDocument;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One credit-note application sent from the client when creating/updating a credit note.
 * Identifies the target invoice (or debit note) and the amount of credit to apply against it.
 */
@Schema(description = "One application of a credit note against an invoice/debit note.")
public record CreditNoteApplicationInputDTO(

    @Schema(description = "ID of the invoice or debit note being credited.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}")
    Long invoiceId,

    @Schema(description = "Amount of the credit note to apply to the target invoice. Must be positive.",
            example = "10000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0.01", message = "{validation.positive}")
    BigDecimal amountApplied
) {}
