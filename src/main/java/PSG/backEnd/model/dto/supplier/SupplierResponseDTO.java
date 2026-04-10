package PSG.backEnd.model.dto.supplier;

import PSG.backEnd.model.dto.contactInfo.ContactInfoResponseDTO;
import PSG.backEnd.model.dto.address.AddressResponseDTO;
import PSG.backEnd.model.enums.IvaCondition;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

public record SupplierResponseDTO(
    Long id,
    String cuit,
    String legalName,
    String tradeName,
    String alias,
    List<PaymentMethod> allowedPaymentMethods,
    AddressResponseDTO address,
    List<ContactInfoResponseDTO> contacts,
    BigDecimal pendingBalance,
    BigDecimal defaultDiscountPercentage,
    String comment,
    boolean active,
    IvaCondition ivaCondition
) {}
