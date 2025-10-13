package PSG.backEnd.model.dto.stock;

import PSG.backEnd.model.enums.StockCategory;

import java.math.BigDecimal;

public record StockResponseDTO(
    Long id,
    String name,
    BigDecimal quantity,
    String location,
    StockCategory stockCategory
) {}

