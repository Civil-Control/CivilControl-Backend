package PSG.backEnd.model.dto.client;

import PSG.backEnd.model.dto.address.AddressDTO;
import PSG.backEnd.model.dto.contactInfo.ContactInfoResponseDTO;
import PSG.backEnd.model.enums.IvaCondition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Full client data returned from the API.")
public record ClientResponseDTO(
    Long id,
    String cuit,
    String businessName,
    String tradeName,
    IvaCondition ivaCondition,
    AddressDTO address,
    List<ContactInfoResponseDTO> contacts,
    Boolean active,
    Boolean deleted
) {}
