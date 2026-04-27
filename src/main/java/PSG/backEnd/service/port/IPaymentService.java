package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.payment.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPaymentService {
    CashPaymentResponseDTO createCash(CashPaymentDTO dto);
    TransferPaymentResponseDTO createTransfer(TransferPaymentDTO dto);
    CheckPaymentResponseDTO createCheck(CheckPaymentDTO dto);

    Page<PaymentResponseDTO> findAll(PaymentFilterDTO filter, Pageable pageable);

    PaymentResponseDTO getById(Long id);

    CashPaymentResponseDTO getCash(Long id);
    TransferPaymentResponseDTO getTransfer(Long id);
    CheckPaymentResponseDTO getCheck(Long id);

    CashPaymentResponseDTO updateCash(Long id, CashPaymentDTO dto);
    TransferPaymentResponseDTO updateTransfer(Long id, TransferPaymentDTO dto);
    CheckPaymentResponseDTO updateCheck(Long id, CheckPaymentDTO dto);

    /**
     * Transitions the operational status of a check payment, registering the matching
     * treasury movement and audit fields. See {@link CheckStatusUpdateDTO} for the rules.
     */
    CheckPaymentResponseDTO updateCheckStatus(Long checkPaymentId, CheckStatusUpdateDTO dto);

    void deleteCash(Long id);
    void deleteTransfer(Long id);
    void deleteCheck(Long id);
    void deleteById(Long id);

    /** Returns the payment-details ID for the payment that covers the given document, or empty if none. */
    java.util.Optional<Long> findPaymentIdByDocumentId(Long documentId);

    byte[] generatePaymentOrderPdf(Long id);
}
