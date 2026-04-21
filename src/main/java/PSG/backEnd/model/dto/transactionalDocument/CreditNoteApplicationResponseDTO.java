package PSG.backEnd.model.dto.transactionalDocument;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Lightweight DTO describing one Credit Note application:
 * which invoice/debit-note is being credited, by which credit note, and how much.
 *
 * Returned in {@link TransactionalDocumentResponseDTO#creditApplications()} for credit notes,
 * and in {@link TransactionalDocumentResponseDTO#appliedCredits()} for invoices/debit notes.
 */
@Schema(description = "Single application of a credit note against an invoice/debit note.")
public record CreditNoteApplicationResponseDTO(

    @Schema(description = "ID of the application row.")
    Long id,

    @Schema(description = "ID of the credit note (CREDIT_NOTE_*) involved in this application.")
    Long creditNoteId,

    @Schema(description = "Display label of the credit note (e.g. \"Nota de crédito A 00001-00000045\").")
    String creditNoteLabel,

    @Schema(description = "ID of the invoice/debit note that is being credited.")
    Long invoiceId,

    @Schema(description = "Display label of the invoice/debit note (e.g. \"Factura A 00001-00000123\").")
    String invoiceLabel,

    @Schema(description = "Total of the invoice/debit note being credited.")
    BigDecimal invoiceTotal,

    @Schema(description = "Amount of the credit note that is applied to this specific invoice.")
    BigDecimal amountApplied
) {}
