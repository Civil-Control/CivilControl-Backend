package PSG.backEnd.model.dto.stock;

import PSG.backEnd.model.enums.BuildingType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simplified building information for stock location reference.")
public record BuildingStockDTO(

    @Schema(description = "Unique identifier of the building.",
            example = "1")
    Long id,

    @Schema(description = "Name of the building.",
            example = "Planta Central Cordoba")
    String name,

    @Schema(description = "Unique code for the building.",
            example = "PLT-CBA-001")
    String code,

    @Schema(description = "Type of the building.",
            example = "PLANTA",
            allowableValues = {"PLANTA", "DEPOSITO", "OFICINA", "SUCURSAL", "OTRO"})
    BuildingType buildingType
) {}

