package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response for batch crew assignment creation, including created records and warnings.")
public record CrewAssignmentBatchResponseDTO(
    @Schema(description = "Successfully created assignments.")
    List<CrewAssignmentResponseDTO> created,
    @Schema(description = "Non-blocking warnings generated during creation.")
    List<CrewAssignmentWarningDTO> warnings
) {}
