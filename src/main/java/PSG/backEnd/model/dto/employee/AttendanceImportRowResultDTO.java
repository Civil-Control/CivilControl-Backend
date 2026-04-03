package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.RowStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AttendanceImportRowResultDTO(
    int rowNumber,
    RowStatus status,
    String dni,
    String employeeName,
    LocalDate date,
    LocalTime time,
    String movementType,
    String buildingName,
    String observation,
    List<String> messages
) {}
