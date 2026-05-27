package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.CreditNoteApplication;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.ledger.AccountImputation;
import PSG.backEnd.model.entity.ledger.AccountMovement;
import PSG.backEnd.model.entity.payment.PaymentApplication;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.model.enums.documents.DocumentType;
import PSG.backEnd.model.enums.ledger.AccountMovementType;
import PSG.backEnd.repository.ledger.AccountImputationRepository;
import PSG.backEnd.repository.ledger.AccountMovementRepository;
import PSG.backEnd.service.port.ILedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LedgerService implements ILedgerService {

    private final AccountMovementRepository movementRepository;
    private final AccountImputationRepository imputationRepository;

    @Override
    @Transactional
    public void recordDocumentMovement(TransactionalDocument doc) {
        if (movementRepository.findOriginalBySourceDocument(doc.getId()).isPresent()) return;
        movementRepository.save(AccountMovement.builder()
                .supplier(doc.getSupplier())
                .movementType(movementTypeFor(doc.getDocumentType()))
                .amount(signedAmountFor(doc.getDocumentType(), doc.getTotal()))
                .movementDate(doc.getDate())
                .sourceDocument(doc)
                .build());
    }

    @Override
    @Transactional
    public void recordDocumentReversal(TransactionalDocument doc) {
        movementRepository.findOriginalBySourceDocument(doc.getId()).ifPresent(original -> movementRepository.save(
                AccountMovement.builder()
                        .supplier(doc.getSupplier())
                        .movementType(AccountMovementType.REVERSAL)
                        .amount(original.getAmount().negate())
                        .movementDate(LocalDate.now())
                        .sourceDocument(doc)
                        .reversalOf(original)
                        .build()));
    }

    @Override
    @Transactional
    public void syncCreditNoteApplicationImputations(TransactionalDocument creditNote) {
        movementRepository.findOriginalBySourceDocument(creditNote.getId()).ifPresent(cnMovement -> {
            imputationRepository.deleteByOriginMovement_Id(cnMovement.getId());
            if (creditNote.getCreditNoteApplications() == null) return;
            for (CreditNoteApplication app : creditNote.getCreditNoteApplications()) {
                if (app.getInvoice() == null || app.getAmountApplied() == null) continue;
                movementRepository.findOriginalBySourceDocumentWithLock(app.getInvoice().getId())
                        .ifPresent(invoiceMovement -> imputationRepository.save(AccountImputation.builder()
                                .originMovement(cnMovement)
                                .destinationMovement(invoiceMovement)
                                .amountApplied(app.getAmountApplied())
                                .build()));
            }
        });
    }

    @Override
    @Transactional
    public void syncDocumentMovement(TransactionalDocument doc) {
        movementRepository.findOriginalBySourceDocument(doc.getId()).ifPresent(movement -> {
            BigDecimal newAmount = signedAmountFor(doc.getDocumentType(), doc.getTotal());
            if (movement.getAmount().compareTo(newAmount) != 0) {
                movement.setAmount(newAmount);
                movementRepository.save(movement);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRemainingBalance(TransactionalDocument doc) {
        return movementRepository.findOriginalBySourceDocument(doc.getId())
                .map(movement -> {
                    BigDecimal applied = imputationRepository.sumAppliedToDestination(movement.getId());
                    BigDecimal result = movement.getAmount().subtract(applied == null ? BigDecimal.ZERO : applied);
                    return result.signum() < 0 ? BigDecimal.ZERO : result;
                })
                .orElse(doc.getTotal() != null ? doc.getTotal() : BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void recordPaymentMovement(PaymentDetails paymentDetails) {
        if (movementRepository.findOriginalBySourcePayment(paymentDetails.getId()).isPresent()) return;
        movementRepository.save(AccountMovement.builder()
                .supplier(paymentDetails.getSupplier())
                .movementType(AccountMovementType.PAYMENT)
                .amount(paymentDetails.getAmount().negate())
                .movementDate(paymentDetails.getPaymentDate())
                .sourcePayment(paymentDetails)
                .build());
    }

    @Override
    @Transactional
    public void recordPaymentReversal(PaymentDetails paymentDetails) {
        movementRepository.findOriginalBySourcePayment(paymentDetails.getId()).ifPresent(original -> {
            imputationRepository.deleteByOriginMovement_Id(original.getId());
            movementRepository.save(AccountMovement.builder()
                    .supplier(paymentDetails.getSupplier())
                    .movementType(AccountMovementType.REVERSAL)
                    .amount(original.getAmount().negate())
                    .movementDate(LocalDate.now())
                    .sourcePayment(paymentDetails)
                    .reversalOf(original)
                    .build());
        });
    }

    @Override
    @Transactional
    public void recordPaymentApplicationImputations(PaymentDetails paymentDetails) {
        if (paymentDetails.getApplications() == null) return;
        AccountMovement paymentMovement = movementRepository.findOriginalBySourcePayment(paymentDetails.getId()).orElse(null);
        if (paymentMovement == null) return;
        for (PaymentApplication app : paymentDetails.getApplications()) {
            if (app.getDocument() == null || app.getAmountApplied() == null) continue;
            movementRepository.findOriginalBySourceDocumentWithLock(app.getDocument().getId())
                    .ifPresent(docMovement -> imputationRepository.save(AccountImputation.builder()
                            .originMovement(paymentMovement)
                            .destinationMovement(docMovement)
                            .amountApplied(app.getAmountApplied())
                            .build()));
        }
    }

    @Override
    @Transactional
    public void recordPaymentCreditNoteImputations(PaymentDetails paymentDetails) {
        if (paymentDetails.getCreditNoteApplications() == null) return;
        for (CreditNoteApplication app : paymentDetails.getCreditNoteApplications()) {
            if (app.getCreditNote() == null || app.getInvoice() == null || app.getAmountApplied() == null) continue;
            AccountMovement cnMovement = movementRepository.findOriginalBySourceDocument(app.getCreditNote().getId()).orElse(null);
            AccountMovement invoiceMovement = movementRepository.findOriginalBySourceDocumentWithLock(app.getInvoice().getId()).orElse(null);
            if (cnMovement == null || invoiceMovement == null) continue;
            imputationRepository.save(AccountImputation.builder()
                    .originMovement(cnMovement)
                    .destinationMovement(invoiceMovement)
                    .amountApplied(app.getAmountApplied())
                    .build());
        }
    }

    private AccountMovementType movementTypeFor(DocumentType dt) {
        return switch (dt) {
            case BILL_A, BILL_B, BILL_C -> AccountMovementType.INVOICE;
            case DEBIT_NOTE_A, DEBIT_NOTE_B, DEBIT_NOTE_C -> AccountMovementType.DEBIT_NOTE;
            case CREDIT_NOTE_A, CREDIT_NOTE_B, CREDIT_NOTE_C -> AccountMovementType.CREDIT_NOTE;
            default -> AccountMovementType.OTHER;
        };
    }

    private BigDecimal signedAmountFor(DocumentType dt, BigDecimal total) {
        if (total == null) return BigDecimal.ZERO;
        return switch (dt) {
            case BILL_A, BILL_B, BILL_C, DEBIT_NOTE_A, DEBIT_NOTE_B, DEBIT_NOTE_C -> total;
            case CREDIT_NOTE_A, CREDIT_NOTE_B, CREDIT_NOTE_C -> total.negate();
            default -> BigDecimal.ZERO;
        };
    }
}
