package PSG.backEnd.model.dto.projectArea;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Data Transfer Object for creating or updating project areas. " +
        "Project areas represent organizational divisions or departments within the company, " +
        "such as construction sites, operational zones, or business units. " +
        "They are used to group employees, vehicles, and other resources.")
public record ProjectAreaDTO(

    @Schema(description = "Name of the project area. Must be unique and descriptive. " +
            "Minimum 1 character, maximum 100 characters.",
            example = "Obras Públicas - Zona Norte",
            minLength = 1,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.notBlank}", groups = {OnCreate.class})
    @Size(min = 1, max = 100, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String name,

    @Schema(description = "Detailed description of the project area, including its purpose, scope, or location details. " +
            "Optional field. Minimum 1 character (if provided), maximum 200 characters.",
            example = "Área encargada de proyectos de infraestructura pública en la región norte de la provincia.",
            minLength = 1,
            maxLength = 200,
            nullable = true)
    @Size(max = 200, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String description,

    @Schema(description = "Indicates if the project area is currently active. " +
            "Active areas can be assigned to employees and vehicles. Optional field, defaults to true.",
            example = "true",
            defaultValue = "true",
            nullable = true)
    Boolean active,

    @Schema(description = "Hex color code for visual identification of the area in the UI. Optional field.",
            example = "#3b82f6",
            nullable = true)
    @Size(max = 20, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String color
) {}