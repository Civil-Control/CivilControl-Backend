package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.payment.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPaymentService {
    CashPaymentResponseDTO createCash(CashPaymentDTO dto);
    TransferPaymentResponseDTO createTransfer(TransferPaymentDTO dto);
    CheckPaymentResponseDTO createCheck(CheckPaymentDTO dto);

    Page<PaymentResponseDTO> findAll(PaymentFilterDTO filter, Pageable pageable);

    CashPaymentResponseDTO getCash(Long id);
    TransferPaymentResponseDTO getTransfer(Long id);
    CheckPaymentResponseDTO getCheck(Long id);

    CashPaymentResponseDTO updateCash(Long id, CashPaymentDTO dto);
    TransferPaymentResponseDTO updateTransfer(Long id, TransferPaymentDTO dto);
    CheckPaymentResponseDTO updateCheck(Long id, CheckPaymentDTO dto);

    void deleteCash(Long id);
    void deleteTransfer(Long id);
    void deleteCheck(Long id);
}
