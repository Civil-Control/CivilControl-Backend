package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A non-blocking warning generated during crew assignment creation.")
public record CrewAssignmentWarningDTO(
    @Schema(description = "Warning code (W1, W2).", example = "W1")
    String code,
    @Schema(description = "Human-readable warning message.")
    String message,
    @Schema(description = "Related employee ID.", example = "25")
    Long employeeId,
    @Schema(description = "Related vehicle ID.", example = "10")
    Long vehicleId
) {}
