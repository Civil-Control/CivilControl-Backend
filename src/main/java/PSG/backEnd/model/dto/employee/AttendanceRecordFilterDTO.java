package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Filter Data Transfer Object for attendance records. " +
        "Used to filter and search attendance records by employee, date range, time range, movement type, and building.")
public record AttendanceRecordFilterDTO(
    @Schema(description = "Filter by employee ID.", example = "25", nullable = true)
    Long employeeId,

    @Schema(description = "Filter by employee first name. Case-insensitive partial match.", example = "Juan", nullable = true)
    String firstName,

    @Schema(description = "Filter by employee last name. Case-insensitive partial match.", example = "García", nullable = true)
    String lastName,

    @Schema(description = "Filter by employee DNI.", example = "12345678", nullable = true)
    String dni,

    @Schema(description = "Filter by movement type: ENTRADA or SALIDA.", example = "ENTRADA", nullable = true)
    MovementType movementType,

    @Schema(description = "Filter by building ID.", example = "3", nullable = true)
    Long buildingId,

    @Schema(description = "Filter by project area ID.", example = "5", nullable = true)
    Long projectAreaId,

    @Schema(description = "Filter by minimum date (inclusive).", example = "2026-04-01", nullable = true)
    LocalDate dateFrom,

    @Schema(description = "Filter by maximum date (inclusive).", example = "2026-04-30", nullable = true)
    LocalDate dateTo,

    @Schema(description = "Filter by minimum time (inclusive).", example = "08:00", nullable = true)
    LocalTime timeFrom,

    @Schema(description = "Filter by maximum time (inclusive).", example = "18:00", nullable = true)
    LocalTime timeTo,

    @Schema(description = "Generic search across employee name and last name (case-insensitive partial match).", nullable = true)
    String search
) {}
