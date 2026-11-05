package PSG.backEnd.model.dto.treasury;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashBoxResponseDTO(
        Long id,
        String name,
        String description,
        BigDecimal balance,
        Boolean negativeBalance,
        Boolean active,
        int movementCount,
        LocalDateTime lastMovementAt
) {}
