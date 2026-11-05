package PSG.backEnd.model.dto.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Sales item detail as returned from the API.")
public record SalesItemDetailResponseDTO(
    Long id,
    Long itemId,
    String itemName,
    BigDecimal unitAmount,
    Integer quantity,
    BigDecimal ivaPercentage,
    BigDecimal totalAmount
) {}
