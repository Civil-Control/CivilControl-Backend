package PSG.backEnd.model.dto;

import PSG.backEnd.model.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

public record SupplierResponseDTO(
    Long id,
    String cuit,
    String legalName,
    String tradeName,
    List<PaymentMethod> allowedPaymentMethods,
    AddressResponseDTO address,
    ContactInfoResponseDTO contactInfo,
    BigDecimal pendingBalance,
    BigDecimal defaultDiscountPercentage,
    String comment
) {}
