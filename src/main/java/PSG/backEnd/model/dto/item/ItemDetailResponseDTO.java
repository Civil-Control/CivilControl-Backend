package PSG.backEnd.model.dto.item;

import java.math.BigDecimal;

public record ItemDetailResponseDTO(
    Long id,
    BigDecimal unitAmount,
    Integer quantity,
    BigDecimal ivaPercentage,
    BigDecimal totalAmount,
    Long itemId,
    String itemName
) {}
