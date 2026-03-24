package PSG.backEnd.model.dto.client;

import PSG.backEnd.model.dto.address.AddressDTO;
import PSG.backEnd.model.dto.contactInfo.ContactInfoResponseDTO;
import PSG.backEnd.model.enums.IvaCondition;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Full client data returned from the API.")
public record ClientResponseDTO(
    Long id,
    String cuit,
    String businessName,
    String tradeName,
    IvaCondition ivaCondition,
    AddressDTO address,
    ContactInfoResponseDTO contactInfo,
    Boolean active,
    Boolean deleted
) {}
