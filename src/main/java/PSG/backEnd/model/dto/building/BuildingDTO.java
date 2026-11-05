package PSG.backEnd.model.dto.building;

import PSG.backEnd.model.dto.address.AddressDTO;
import PSG.backEnd.model.enums.BuildingType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Data Transfer Object for creating or updating buildings. " +
        "Represents physical facilities used in construction projects, including plants, warehouses, offices, and branches.")
public record BuildingDTO(

    @Schema(description = "Name of the building. Must be descriptive and unique.",
            example = "Planta Central Cordoba",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 100)
    @NotBlank(groups = OnCreate.class, message = "{validation.required}")
    @Size(max = 100, groups = {OnCreate.class, OnUpdate.class}, message = "{building.name.size}")
    String name,

    @Schema(description = "Unique code for the building. Used for internal identification and references.",
            example = "PLT-CBA-001",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 50)
    @NotBlank(groups = OnCreate.class, message = "{validation.required}")
    @Size(max = 50, groups = {OnCreate.class, OnUpdate.class}, message = "{building.code.size}")
    String code,

    @Schema(description = "Complete address of the building including street, number, city, state, country, and zip code.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(groups = OnCreate.class, message = "{validation.required}")
    @Valid
    AddressDTO address,

    @Schema(description = "Type of the building for classification purposes.",
            example = "PLANTA",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"PLANTA", "DEPOSITO", "OFICINA", "SUCURSAL", "GALPON", "RESIDENCIAL", "CONSULTORIO", "TERRENO", "OTRO"})
    @NotNull(groups = OnCreate.class, message = "{validation.required}")
    BuildingType buildingType,

    @Schema(description = "ID of the project area (sector) to which this building belongs. Optional field.",
            example = "1",
            nullable = true)
    Long projectAreaId,

    @Schema(description = "ID of the project area task (sub-task) for this building. Optional.",
            example = "1",
            nullable = true)
    Long projectAreaTaskId,

    @Schema(description = "Indicates if the building is active or inactive. Active buildings can be used for operations.",
            example = "true",
            defaultValue = "true")
    Boolean active
) {}

