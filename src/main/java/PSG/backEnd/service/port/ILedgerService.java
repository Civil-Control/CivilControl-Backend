package PSG.backEnd.service.port;

import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.payment.PaymentDetails;

public interface ILedgerService {

    void recordDocumentMovement(TransactionalDocument document);

    void recordDocumentReversal(TransactionalDocument document);

    void syncCreditNoteApplicationImputations(TransactionalDocument creditNote);

    void recordPaymentMovement(PaymentDetails paymentDetails);

    void recordPaymentReversal(PaymentDetails paymentDetails);

    void recordPaymentApplicationImputations(PaymentDetails paymentDetails);

    void recordPaymentCreditNoteImputations(PaymentDetails paymentDetails);
}
