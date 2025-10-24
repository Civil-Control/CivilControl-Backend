package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.ActionType;

import java.time.LocalDate;

public record DisciplinaryActionResponseDTO(
    Long id,
    Long employeeId,
    String employeeName,
    String employeeLastName,
    ActionType actionType,
    String reason,
    LocalDate actionDate,
    LocalDate endDate,
    String notes
) {}

