package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.transactionalDocument.LinkedRecordsSummaryDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentFilterDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ITransactionalDocumentService {

    TransactionalDocumentResponseDTO createTransactionalDocument(TransactionalDocumentDTO transactionalDocumentDTO);
    Page<TransactionalDocumentResponseDTO> getAllTransactionalDocuments(TransactionalDocumentFilterDTO filterDTO, Pageable pageable);
    TransactionalDocumentResponseDTO getTransactionalDocumentById(Long id);
    TransactionalDocumentResponseDTO updateTransactionalDocument(Long id, TransactionalDocumentDTO transactionalDocumentDTO);
    void updateTransactionalDocumentStatus(Long documentId, Long supplierId, BigDecimal amount);
    void revertTransactionalDocumentStatusIfExists(Long documentId, Long supplierId);

    /**
     * Recomputes the {@code paid} flag of the given document based on the current
     * {@link PSG.backEnd.model.entity.payment.PaymentApplication} rows + applied credit notes:
     * {@code paid = sum(payment_applications.amountApplied) + sum(credit_note_applications.amountApplied) >= total}.
     *
     * <p>Pure derivation, no balance side-effects: balance changes are owned by the caller
     * (PaymentService for payment-driven changes, TransactionalDocumentService for create/delete-driven changes).
     * Idempotent and safe to call after every payment-application mutation.
     */
    void recomputePaidStatus(Long documentId);
    TransactionalDocumentResponseDTO markDocumentUnpaid(Long id);
    TransactionalDocumentResponseDTO markCreditNoteApplied(Long id, boolean applied);
    LinkedRecordsSummaryDTO getLinkedRecordsSummary(Long id);
    void deleteTransactionalDocument(Long id, boolean deleteLinkedRecords);
    TransactionalDocument getEntityById(Long id);
    TransactionalDocumentResponseDTO recalculateTotals(Long id);
}