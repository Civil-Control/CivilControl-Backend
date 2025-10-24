package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.ActionType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a disciplinary action. " +
        "Represents a disciplinary measure taken against an employee, including the type of action, " +
        "reason, dates, and additional notes.")
public record DisciplinaryActionDTO(
    @Schema(description = "Unique identifier of the employee receiving the disciplinary action. " +
            "Must reference an existing employee in the system.",
            example = "15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Employee ID cannot be null", groups = OnCreate.class)
    Long employeeId,

    @Schema(description = "Type of disciplinary action being taken. Valid values: " +
            "WARNING (verbal or written warning), SUSPENSION (temporary suspension from duties), " +
            "TERMINATION (employment termination).",
            example = "WARNING",
            allowableValues = {"WARNING", "SUSPENSION", "TERMINATION"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Action type cannot be null", groups = OnCreate.class)
    ActionType actionType,

    @Schema(description = "Detailed reason for the disciplinary action. Must clearly explain the circumstances " +
            "and behavior that led to the action. Minimum 10 characters, maximum 500 characters.",
            example = "Repeated tardiness despite previous warnings. Employee arrived late 5 times in the past two weeks.",
            minLength = 10,
            maxLength = 500,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Reason cannot be blank", groups = OnCreate.class)
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters", groups = {OnCreate.class, OnUpdate.class})
    String reason,

    @Schema(description = "Date when the disciplinary action was taken or officially recorded. " +
            "Cannot be in the future. Must be today or a past date.",
            example = "2025-10-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Action date cannot be null", groups = OnCreate.class)
    @PastOrPresent(message = "Action date cannot be in the future", groups = {OnCreate.class, OnUpdate.class})
    LocalDate actionDate,

    @Schema(description = "Optional end date for temporary disciplinary actions (e.g., suspension end date). " +
            "Relevant for time-limited actions. Leave null for permanent actions like termination or warnings without time limit.",
            example = "2025-10-22",
            nullable = true)
    LocalDate endDate,

    @Schema(description = "Additional notes or comments about the disciplinary action. " +
            "Can include details about meetings held, employee response, follow-up actions, or any other relevant information. " +
            "Maximum 1000 characters.",
            example = "Employee acknowledged the issue and committed to improvement. HR meeting scheduled for follow-up in 30 days.",
            maxLength = 1000,
            nullable = true)
    @Size(max = 1000, message = "Notes must not exceed 1000 characters", groups = {OnCreate.class, OnUpdate.class})
    String notes
) {}

