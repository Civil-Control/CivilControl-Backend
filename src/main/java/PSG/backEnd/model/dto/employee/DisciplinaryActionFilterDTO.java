package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.ActionType;

import java.time.LocalDate;

public record DisciplinaryActionFilterDTO(
    Long employeeId,
    ActionType actionType,
    LocalDate actionDateFrom,
    LocalDate actionDateTo,
    LocalDate endDateFrom,
    LocalDate endDateTo
) {}

