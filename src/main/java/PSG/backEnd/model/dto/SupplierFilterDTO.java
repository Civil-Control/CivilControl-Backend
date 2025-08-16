package PSG.backEnd.model.dto;

import java.math.BigDecimal;

public record SupplierFilterDTO(
    String cuit,
    String legalName,
    String tradeName,
    String city,
    BigDecimal minDiscountPercentage,
    BigDecimal maxDiscountPercentage,
    Boolean active
) {}
