package PSG.backEnd.model.dto.client;

import PSG.backEnd.model.enums.IvaCondition;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lightweight client summary for use in dropdowns and references.")
public record ClientSummaryDTO(
    Long id,
    String cuit,
    String businessName,
    String tradeName,
    IvaCondition ivaCondition
) {}
