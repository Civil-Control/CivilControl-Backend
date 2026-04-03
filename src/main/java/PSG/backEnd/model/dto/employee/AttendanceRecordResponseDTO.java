package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Response Data Transfer Object for attendance record. " +
        "Contains complete information about an attendance movement including employee and building details.")
public record AttendanceRecordResponseDTO(
    @Schema(description = "Unique identifier of the attendance record.", example = "150")
    Long id,

    @Schema(description = "Unique identifier of the employee.", example = "25")
    Long employeeId,

    @Schema(description = "First name of the employee.", example = "Juan Carlos")
    String employeeName,

    @Schema(description = "Last name of the employee.", example = "García Pérez")
    String employeeLastName,

    @Schema(description = "DNI of the employee.", example = "12345678")
    String employeeDni,

    @Schema(description = "Date of the attendance movement.", example = "2026-04-01")
    LocalDate date,

    @Schema(description = "Time of the attendance movement.", example = "08:30")
    LocalTime time,

    @Schema(description = "Type of movement: ENTRADA or SALIDA.", example = "ENTRADA")
    MovementType movementType,

    @Schema(description = "ID of the building where the movement was registered.", example = "3", nullable = true)
    Long buildingId,

    @Schema(description = "Name of the building.", example = "Planta Central", nullable = true)
    String buildingName,

    @Schema(description = "ID of the employee's project area.", example = "5", nullable = true)
    Long projectAreaId,

    @Schema(description = "Name of the employee's project area.", example = "Mantenimiento", nullable = true)
    String projectAreaName,

    @Schema(description = "Color of the employee's project area.", example = "#FF5733", nullable = true)
    String projectAreaColor,

    @Schema(description = "Optional observation.", example = "Llegó tarde por lluvia", nullable = true)
    String observation
) {}
