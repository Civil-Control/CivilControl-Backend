package PSG.backEnd.model.dto.client;

import PSG.backEnd.model.enums.IvaCondition;

public record ClientFilterDTO(
    String cuit,
    String businessName,
    String tradeName,
    IvaCondition ivaCondition,
    Boolean active,
    String search
) {}
