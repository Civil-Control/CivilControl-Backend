package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.SalesCreditNoteApplication;
import PSG.backEnd.model.entity.ledger.ClientAccountImputation;
import PSG.backEnd.model.entity.ledger.ClientAccountMovement;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import PSG.backEnd.model.enums.ledger.ClientAccountMovementType;
import PSG.backEnd.repository.ledger.ClientAccountImputationRepository;
import PSG.backEnd.repository.ledger.ClientAccountMovementRepository;
import PSG.backEnd.service.port.IClientLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Client-side counterpart of {@link LedgerService}. Mirrors the document-movement subset only —
 * there is no client-side Payment entity, so no payment/on-account methods exist here.
 */
@Service
@RequiredArgsConstructor
public class ClientLedgerService implements IClientLedgerService {

    private final ClientAccountMovementRepository movementRepository;
    private final ClientAccountImputationRepository imputationRepository;

    @Override
    @Transactional
    public void recordDocumentMovement(SalesDocument doc) {
        BigDecimal amount = signedAmountFor(doc.getDocumentType(), doc.getTotal());
        Optional<ClientAccountMovement> existing = movementRepository.findOriginalBySourceDocument(doc.getId());
        if (existing.isPresent()) {
            if (existing.get().getAmount().compareTo(amount) != 0) {
                existing.get().setAmount(amount);
                movementRepository.save(existing.get());
            }
            movementRepository.deleteReversalsBySourceDocument(doc.getId());
            return;
        }
        movementRepository.save(ClientAccountMovement.builder()
                .client(doc.getClient())
                .movementType(movementTypeFor(doc.getDocumentType()))
                .amount(amount)
                .movementDate(doc.getDate())
                .sourceDocument(doc)
                .build());
    }

    @Override
    @Transactional
    public void recordDocumentReversal(SalesDocument doc) {
        movementRepository.findOriginalBySourceDocument(doc.getId()).ifPresent(original -> {
            // Fix vs. the purchases-side LedgerService: clean up any imputations hanging off
            // this movement (as origin) before reversing, so a deleted credit note's applications
            // don't keep affecting remainingBalance on invoices it used to credit.
            imputationRepository.deleteByOriginMovement_Id(original.getId());
            movementRepository.save(ClientAccountMovement.builder()
                    .client(doc.getClient())
                    .movementType(ClientAccountMovementType.REVERSAL)
                    .amount(original.getAmount().negate())
                    .movementDate(LocalDate.now())
                    .sourceDocument(doc)
                    .reversalOf(original)
                    .build());
        });
    }

    @Override
    @Transactional
    public void syncCreditNoteApplicationImputations(SalesDocument creditNote) {
        movementRepository.findOriginalBySourceDocument(creditNote.getId()).ifPresent(cnMovement -> {
            imputationRepository.deleteByOriginMovement_Id(cnMovement.getId());
            if (creditNote.getCreditNoteApplications() == null) return;
            for (SalesCreditNoteApplication app : creditNote.getCreditNoteApplications()) {
                if (app.getInvoice() == null || app.getAmountApplied() == null) continue;
                movementRepository.findOriginalBySourceDocumentWithLock(app.getInvoice().getId())
                        .ifPresent(invoiceMovement -> imputationRepository.save(ClientAccountImputation.builder()
                                .originMovement(cnMovement)
                                .destinationMovement(invoiceMovement)
                                .amountApplied(app.getAmountApplied())
                                .build()));
            }
        });
    }

    @Override
    @Transactional
    public void syncDocumentMovement(SalesDocument doc) {
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
    public BigDecimal getRemainingBalance(SalesDocument doc) {
        return movementRepository.findOriginalBySourceDocument(doc.getId())
                .map(movement -> {
                    BigDecimal applied = imputationRepository.sumAppliedToDestination(movement.getId());
                    BigDecimal result = movement.getAmount().subtract(applied == null ? BigDecimal.ZERO : applied);
                    return result.signum() < 0 ? BigDecimal.ZERO : result;
                })
                .orElse(doc.getTotal() != null ? doc.getTotal() : BigDecimal.ZERO);
    }

    /**
     * True client balance (debe − haber) across every ledger movement: invoices and debit notes
     * are debits, credit notes are credits. Mirrors LedgerService.getSupplierLedgerBalance so both
     * the client detail screen and any future report agree.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getClientLedgerBalance(Long clientId, Long tenantId) {
        return movementRepository.sumBalanceByClient(clientId, tenantId);
    }

    private ClientAccountMovementType movementTypeFor(SalesDocumentType type) {
        return switch (type) {
            case FACTURA_A, FACTURA_B, FACTURA_C -> ClientAccountMovementType.INVOICE;
            case NOTA_DEBITO_A, NOTA_DEBITO_B, NOTA_DEBITO_C -> ClientAccountMovementType.DEBIT_NOTE;
            case NOTA_CREDITO_A, NOTA_CREDITO_B, NOTA_CREDITO_C -> ClientAccountMovementType.CREDIT_NOTE;
        };
    }

    private BigDecimal signedAmountFor(SalesDocumentType type, BigDecimal total) {
        if (total == null) return BigDecimal.ZERO;
        return switch (type) {
            case FACTURA_A, FACTURA_B, FACTURA_C, NOTA_DEBITO_A, NOTA_DEBITO_B, NOTA_DEBITO_C -> total;
            case NOTA_CREDITO_A, NOTA_CREDITO_B, NOTA_CREDITO_C -> total.negate();
        };
    }
}
