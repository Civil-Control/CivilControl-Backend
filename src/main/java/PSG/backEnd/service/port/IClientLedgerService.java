package PSG.backEnd.service.port;

import PSG.backEnd.model.entity.sales.SalesDocument;

import java.math.BigDecimal;

/**
 * Client-side counterpart of {@link ILedgerService}. Only the document-movement subset —
 * there is no client-side Payment entity, so no payment/on-account methods exist here.
 */
public interface IClientLedgerService {

    void recordDocumentMovement(SalesDocument document);

    void recordDocumentReversal(SalesDocument document);

    void syncCreditNoteApplicationImputations(SalesDocument creditNote);

    void syncDocumentMovement(SalesDocument document);

    BigDecimal getRemainingBalance(SalesDocument document);

    BigDecimal getClientLedgerBalance(Long clientId, Long tenantId);
}
