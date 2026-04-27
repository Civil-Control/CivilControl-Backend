package PSG.backEnd.model.dto.treasury;

import PSG.backEnd.model.enums.treasury.CashBoxMovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CashBoxMovementResponseDTO(
        Long id,
        Long cashBoxId,
        String cashBoxName,
        CashBoxMovementType type,
        BigDecimal amount,
        BigDecimal signedAmount,
        BigDecimal balanceAfter,
        LocalDate movementDate,
        String comment,
        LocalDateTime createdAt,
        Long createdByUserId,
        String createdByUserName,
        Long cashPaymentId
) {}
