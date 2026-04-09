package PSG.backEnd.model.dto.building;

import PSG.backEnd.model.dto.address.AddressResponseDTO;
import PSG.backEnd.model.enums.BuildingType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response DTO containing complete information about a building, " +
        "including its unique identifier, address, type, and status.")
public record BuildingResponseDTO(

    @Schema(description = "Unique identifier of the building.",
            example = "5")
    Long id,

    @Schema(description = "Name of the building.",
            example = "Planta Central Cordoba")
    String name,

    @Schema(description = "Unique code for the building.",
            example = "PLT-CBA-001")
    String code,

    @Schema(description = "Complete address of the building.")
    AddressResponseDTO address,

    @Schema(description = "Type of the building.",
            example = "PLANTA",
            allowableValues = {"PLANTA", "DEPOSITO", "OFICINA", "SUCURSAL", "GALPON", "RESIDENCIAL", "CONSULTORIO", "TERRENO", "OTRO"})
    BuildingType buildingType,

    @Schema(description = "ID of the project area (sector) to which this building belongs. Null if not assigned to any sector.",
            example = "1",
            nullable = true)
    Long projectAreaId,

    @Schema(description = "Name of the project area (sector) to which this building belongs. Null if not assigned to any sector.",
            example = "Sector Norte",
            nullable = true)
    String projectAreaName,

    @Schema(description = "Color of the project area (sector) to which this building belongs. Null if not assigned to any sector.",
            example = "#3b82f6",
            nullable = true)
    String projectAreaColor,

    @Schema(description = "Indicates if the building is active or inactive.",
            example = "true")
    Boolean active,

    @Schema(description = "Indicates if the building has been soft-deleted.",
            example = "false")
    Boolean deleted
) {}
