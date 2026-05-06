package PSG.backEnd.model.dto.payment;

import java.math.BigDecimal;

public record CreditNoteApplicationResponseDTO(
        Long creditNoteId,
        String creditNoteLabel,
        Long invoiceId,
        String invoiceLabel,
        BigDecimal amountApplied
) {}
