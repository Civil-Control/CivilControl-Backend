package PSG.backEnd.model.dto.building;

import PSG.backEnd.model.enums.BuildingType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filter DTO for searching and filtering buildings based on various criteria.")
public record BuildingFilterDTO(

    @Schema(description = "Filter by building name. Partial match search.",
            example = "Planta")
    String name,

    @Schema(description = "Filter by building code. Partial match search.",
            example = "PLT-CBA")
    String code,

    @Schema(description = "Filter by building type.",
            example = "PLANTA",
            allowableValues = {"PLANTA", "DEPOSITO", "OFICINA", "SUCURSAL", "GALPON", "RESIDENCIAL", "CONSULTORIO", "TERRENO", "OTRO"})
    BuildingType buildingType,

    @Schema(description = "Filter by project area (sector) ID.",
            example = "1")
    Long projectAreaId,

    @Schema(description = "Filter by active status. Returns active buildings if true, inactive if false.",
            example = "true")
    Boolean active,

    @Schema(description = "Generic search across building name and code (case-insensitive partial match).",
            example = "PLT")
    String search
) {}

