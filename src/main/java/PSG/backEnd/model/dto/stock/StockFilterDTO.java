package PSG.backEnd.model.dto.stock;

import PSG.backEnd.model.enums.StockCategory;

import java.math.BigDecimal;

public record StockFilterDTO(
    String name,
    String location,
    StockCategory stockCategory,
    BigDecimal minQuantity,
    BigDecimal maxQuantity
) {}
