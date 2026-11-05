package PSG.backEnd.model.dto.treasury;

import PSG.backEnd.model.enums.treasury.BankAccountMovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BankAccountMovementResponseDTO(
        Long id,
        Long bankAccountId,
        String bankAccountName,
        BankAccountMovementType type,
        BigDecimal amount,
        BigDecimal signedAmount,
        BigDecimal balanceAfter,
        LocalDate movementDate,
        String comment,
        LocalDateTime createdAt,
        Long createdByUserId,
        String createdByUserName,
        Long checkPaymentId,
        Long transferPaymentId
) {}
