package PSG.backEnd.model.dto.supplier;

import PSG.backEnd.model.dto.contactInfo.ContactInfoResponseDTO;
import PSG.backEnd.model.dto.address.AddressResponseDTO;
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
    String comment,
    boolean active
) {}
