package PSG.backEnd.model.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentDetailsResponseDTO(
        Long id,
        LocalDate paymentDate,
        Long supplierId,
        String supplierName,
        BigDecimal amount,
        String comment,
        List<Long> paidDocumentIds,
        List<PaymentApplicationResponseDTO> applications,
        BigDecimal onAccountAmount,
        List<CreditNoteApplicationResponseDTO> creditNoteApplications
) {
    /** Back-compat constructor for legacy call sites. */
    public PaymentDetailsResponseDTO(Long id, LocalDate paymentDate, Long supplierId, String supplierName,
                                     BigDecimal amount, String comment, List<Long> paidDocumentIds) {
        this(id, paymentDate, supplierId, supplierName, amount, comment, paidDocumentIds, List.of(), BigDecimal.ZERO, List.of());
    }
}