package PSG.backEnd.model.dto.treasury;

import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.treasury.BankAccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankAccountResponseDTO(
        Long id,
        String name,
        String bankName,
        BankAccountType accountType,
        String accountNumber,
        String cbu,
        String alias,
        Currency currency,
        BigDecimal balance,
        Boolean active,
        int movementCount,
        LocalDateTime lastMovementAt,
        int activeCheckbookCount
) {}
