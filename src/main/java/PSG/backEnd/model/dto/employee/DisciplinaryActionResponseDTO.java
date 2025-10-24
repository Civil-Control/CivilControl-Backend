package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Response Data Transfer Object for disciplinary action. " +
        "Contains complete information about a disciplinary action including employee details and action specifics.")
public record DisciplinaryActionResponseDTO(
    @Schema(description = "Unique identifier of the disciplinary action record.",
            example = "42")
    Long id,

    @Schema(description = "Unique identifier of the employee who received the disciplinary action.",
            example = "15")
    Long employeeId,

    @Schema(description = "First name of the employee who received the disciplinary action.",
            example = "Juan Carlos")
    String employeeName,

    @Schema(description = "Last name of the employee who received the disciplinary action.",
            example = "García Pérez")
    String employeeLastName,

    @Schema(description = "Type of disciplinary action taken. Values: WARNING, SUSPENSION, TERMINATION.",
            example = "WARNING")
    ActionType actionType,

    @Schema(description = "Detailed reason for the disciplinary action.",
            example = "Repeated tardiness despite previous warnings. Employee arrived late 5 times in the past two weeks.")
    String reason,

    @Schema(description = "Date when the disciplinary action was officially taken.",
            example = "2025-10-15")
    LocalDate actionDate,

    @Schema(description = "End date for temporary disciplinary actions. Null for permanent actions.",
            example = "2025-10-22",
            nullable = true)
    LocalDate endDate,

    @Schema(description = "Additional notes or comments about the disciplinary action.",
            example = "Employee acknowledged the issue and committed to improvement.",
            nullable = true)
    String notes
) {}

