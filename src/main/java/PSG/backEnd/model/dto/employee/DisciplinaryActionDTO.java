package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.ActionType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record DisciplinaryActionDTO(
    @NotNull(message = "Employee ID cannot be null", groups = OnCreate.class)
    Long employeeId,

    @NotNull(message = "Action type cannot be null", groups = OnCreate.class)
    ActionType actionType,

    @NotBlank(message = "Reason cannot be blank", groups = OnCreate.class)
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters", groups = {OnCreate.class, OnUpdate.class})
    String reason,

    @NotNull(message = "Action date cannot be null", groups = OnCreate.class)
    @PastOrPresent(message = "Action date cannot be in the future", groups = {OnCreate.class, OnUpdate.class})
    LocalDate actionDate,

    LocalDate endDate,

    @Size(max = 1000, message = "Notes must not exceed 1000 characters", groups = {OnCreate.class, OnUpdate.class})
    String notes
) {}

