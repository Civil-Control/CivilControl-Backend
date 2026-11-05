package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Batch creation request for crew assignments (drag & drop).")
public record CrewAssignmentBatchDTO(
    @Schema(description = "List of crew assignments to create.")
    @NotNull(message = "{validation.required}")
    @Size(min = 1, max = 200, message = "{validation.size.range}")
    @Valid
    List<CrewAssignmentDTO> assignments
) {}
