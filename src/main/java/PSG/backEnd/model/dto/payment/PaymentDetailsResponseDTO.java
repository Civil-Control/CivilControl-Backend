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
        List<Long> paidDocumentIds
) {}