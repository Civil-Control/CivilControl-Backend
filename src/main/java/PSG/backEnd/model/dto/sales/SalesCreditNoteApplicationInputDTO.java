package PSG.backEnd.model.dto.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "One credit-note-to-invoice application, sent as part of a credit note's create/update payload.")
public record SalesCreditNoteApplicationInputDTO(

    @Schema(description = "ID of the invoice/debit note this credit note applies to.")
    @NotNull
    Long invoiceId,

    @Schema(description = "Amount of the credit note applied to this specific invoice.")
    @NotNull
    @DecimalMin(value = "0.01")
    BigDecimal amountApplied
) {}
