package PSG.backEnd.model.dto.projectArea;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response DTO for a project area task (sub-task)")
public record ProjectAreaTaskResponseDTO(

    @Schema(description = "Task ID", example = "1")
    Long id,

    @Schema(description = "Task name", example = "Cableado")
    String name,

    @Schema(description = "Parent project area ID", example = "5")
    Long projectAreaId,

    @Schema(description = "Parent project area name", example = "Obras Norte")
    String projectAreaName,

    @Schema(description = "Whether the task is soft-deleted")
    Boolean deleted
) {}
