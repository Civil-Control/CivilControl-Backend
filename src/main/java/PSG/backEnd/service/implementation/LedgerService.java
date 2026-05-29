package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.model.dto.transactionalDocument.OnAccountApplicationResponseDTO;
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
import java.util.Optional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService implements ILedgerService {

    private final AccountMovementRepository movementRepository;
    private final AccountImputationRepository imputationRepository;

    @Override
    @Transactional
    public void recordDocumentMovement(TransactionalDocument doc) {
        BigDecimal amount = signedAmountFor(doc.getDocumentType(), doc.getTotal());
        Optional<AccountMovement> existing = movementRepository.findOriginalBySourceDocument(doc.getId());
        if (existing.isPresent()) {
            if (existing.get().getAmount().compareTo(amount) != 0) {
                existing.get().setAmount(amount);
                movementRepository.save(existing.get());
            }
            return;
        }
        movementRepository.save(AccountMovement.builder()
                .supplier(doc.getSupplier())
                .movementType(movementTypeFor(doc.getDocumentType()))
                .amount(amount)
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
            if (imputationRepository.existsByOriginMovement_IdAndOnAccountTrue(original.getId())) {
                throw new IllegalStateException(
                        "No se puede eliminar el pago porque tiene imputaciones de saldo a favor aplicadas. " +
                        "Elimine primero las imputaciones de saldo a favor del comprobante correspondiente.");
            }
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

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableOnAccountBalance(Long supplierId, Long tenantId) {
        List<AccountMovement> payments = movementRepository.findPaymentMovementsBySupplier(supplierId, tenantId);
        BigDecimal available = BigDecimal.ZERO;
        for (AccountMovement pm : payments) {
            BigDecimal paid = pm.getAmount().negate();
            BigDecimal applied = imputationRepository.sumAppliedFromOrigin(pm.getId());
            BigDecimal remaining = paid.subtract(applied == null ? BigDecimal.ZERO : applied);
            if (remaining.signum() > 0) {
                available = available.add(remaining);
            }
        }
        return available;
    }

    @Override
    @Transactional
    public List<OnAccountApplicationResponseDTO> applyOnAccountToDocument(TransactionalDocument doc, BigDecimal amount) {
        AccountMovement docMovement = movementRepository.findOriginalBySourceDocumentWithLock(doc.getId())
                .orElseThrow(() -> new IllegalStateException("El comprobante no tiene movimiento de cuenta asociado."));

        Long supplierId = doc.getSupplier().getId();
        Long tenantId = doc.getTenantId();

        List<AccountMovement> paymentMovements = movementRepository.findPaymentMovementsBySupplierWithLock(supplierId, tenantId);

        BigDecimal remaining = amount;
        List<OnAccountApplicationResponseDTO> created = new ArrayList<>();

        for (AccountMovement pm : paymentMovements) {
            if (remaining.signum() <= 0) break;
            BigDecimal paid = pm.getAmount().negate();
            BigDecimal applied = imputationRepository.sumAppliedFromOrigin(pm.getId());
            BigDecimal available = paid.subtract(applied == null ? BigDecimal.ZERO : applied);
            if (available.signum() <= 0) continue;

            BigDecimal toApply = remaining.min(available);
            AccountImputation imp = imputationRepository.save(AccountImputation.builder()
                    .originMovement(pm)
                    .destinationMovement(docMovement)
                    .amountApplied(toApply)
                    .onAccount(true)
                    .build());
            created.add(new OnAccountApplicationResponseDTO(
                    imp.getId(),
                    imp.getAmountApplied(),
                    pm.getSourcePayment() != null ? pm.getSourcePayment().getId() : null,
                    pm.getMovementDate()));
            remaining = remaining.subtract(toApply);
        }

        if (remaining.signum() > 0) {
            throw new IllegalStateException("Saldo a favor insuficiente para cubrir el monto solicitado.");
        }
        return created;
    }

    @Override
    @Transactional
    public void removeOnAccountImputation(Long imputationId, Long tenantId) {
        AccountImputation imp = imputationRepository.findById(imputationId)
                .orElseThrow(() -> new NotFoundException("Imputación no encontrada: " + imputationId));
        if (!imp.isOnAccount()) {
            throw new IllegalArgumentException("La imputación indicada no es de tipo saldo a favor.");
        }
        if (!tenantId.equals(imp.getTenantId())) {
            throw new NotFoundException("Imputación no encontrada: " + imputationId);
        }
        imputationRepository.delete(imp);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOnAccountImputations(TransactionalDocument doc) {
        return movementRepository.findOriginalBySourceDocument(doc.getId())
                .map(m -> !imputationRepository.findByDestinationMovement_IdAndOnAccountTrue(m.getId()).isEmpty())
                .orElse(false);
    }

    @Override
    @Transactional
    public void syncPaymentMovement(PaymentDetails paymentDetails) {
        movementRepository.findOriginalBySourcePayment(paymentDetails.getId()).ifPresent(movement -> {
            BigDecimal newAmount = paymentDetails.getAmount().negate();
            if (movement.getAmount().compareTo(newAmount) != 0) {
                movement.setAmount(newAmount);
                movementRepository.save(movement);
            }
        });
    }

    @Override
    @Transactional
    public void syncPaymentApplicationImputations(PaymentDetails paymentDetails) {
        AccountMovement paymentMovement = movementRepository.findOriginalBySourcePayment(paymentDetails.getId()).orElse(null);
        if (paymentMovement == null) return;
        imputationRepository.deleteByOriginMovement_IdAndOnAccountFalse(paymentMovement.getId());
        if (paymentDetails.getApplications() == null) return;
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
    @Transactional(readOnly = true)
    public void validatePaymentAmountForOnAccount(PaymentDetails paymentDetails, BigDecimal newAmount) {
        movementRepository.findOriginalBySourcePayment(paymentDetails.getId()).ifPresent(movement -> {
            BigDecimal onAccountSum = imputationRepository.sumOnAccountAppliedFromOrigin(movement.getId());
            if (onAccountSum == null) onAccountSum = BigDecimal.ZERO;
            if (newAmount.compareTo(onAccountSum) < 0) {
                throw new IllegalStateException(
                        "No se puede reducir el monto del pago a " + newAmount +
                        " porque tiene " + onAccountSum + " imputados como saldo a favor en comprobantes.");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnAccountApplicationResponseDTO> getOnAccountApplicationsForDocument(TransactionalDocument doc) {
        return movementRepository.findOriginalBySourceDocument(doc.getId())
                .map(m -> imputationRepository.findByDestinationMovement_IdAndOnAccountTrue(m.getId())
                        .stream()
                        .map(imp -> new OnAccountApplicationResponseDTO(
                                imp.getId(),
                                imp.getAmountApplied(),
                                imp.getOriginMovement().getSourcePayment() != null
                                        ? imp.getOriginMovement().getSourcePayment().getId() : null,
                                imp.getOriginMovement().getMovementDate()))
                        .toList())
                .orElse(java.util.Collections.emptyList());
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
