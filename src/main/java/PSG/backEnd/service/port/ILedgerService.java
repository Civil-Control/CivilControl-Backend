package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.transactionalDocument.OnAccountApplicationResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.payment.PaymentDetails;

import java.math.BigDecimal;
import java.util.List;

public interface ILedgerService {

    void recordDocumentMovement(TransactionalDocument document);

    void recordDocumentReversal(TransactionalDocument document);

    void syncCreditNoteApplicationImputations(TransactionalDocument creditNote);

    void syncDocumentMovement(TransactionalDocument document);

    BigDecimal getRemainingBalance(TransactionalDocument document);

    void recordPaymentMovement(PaymentDetails paymentDetails);

    void recordPaymentReversal(PaymentDetails paymentDetails);

    void recordPaymentApplicationImputations(PaymentDetails paymentDetails);

    void recordPaymentCreditNoteImputations(PaymentDetails paymentDetails);

    BigDecimal getAvailableOnAccountBalance(Long supplierId, Long tenantId);

    List<OnAccountApplicationResponseDTO> applyOnAccountToDocument(TransactionalDocument doc, BigDecimal amount);

    void removeOnAccountImputation(Long imputationId, Long tenantId);

    boolean hasOnAccountImputations(TransactionalDocument doc);

    List<OnAccountApplicationResponseDTO> getOnAccountApplicationsForDocument(TransactionalDocument doc);

    void syncPaymentMovement(PaymentDetails paymentDetails);

    void syncPaymentApplicationImputations(PaymentDetails paymentDetails);

    void validatePaymentAmountForOnAccount(PaymentDetails paymentDetails, BigDecimal newAmount);

    void clearPaymentApplicationImputations(PaymentDetails paymentDetails);

    void clearPaymentCreditNoteImputations(java.util.Collection<PSG.backEnd.model.entity.CreditNoteApplication> apps);
}
