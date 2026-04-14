package PSG.backEnd.model.dto.projectArea;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO for creating/updating a project area task (sub-task)")
public record ProjectAreaTaskDTO(

    @NotNull(message = "{projectAreaTask.projectAreaId.required}", groups = OnCreate.class)
    @Schema(description = "ID of the parent project area", example = "1")
    Long projectAreaId,

    @NotNull(message = "{projectAreaTask.name.required}", groups = OnCreate.class)
    @Size(min = 1, max = 150, groups = {OnCreate.class, OnUpdate.class})
    @Schema(description = "Name of the sub-task", example = "Cableado")
    String name
) {}
