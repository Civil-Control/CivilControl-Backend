package PSG.backEnd.model.dto.transactionalDocument;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OnAccountApplicationResponseDTO(
    Long id,
    BigDecimal amountApplied,
    Long sourcePaymentId,
    LocalDate paymentDate
) {}
