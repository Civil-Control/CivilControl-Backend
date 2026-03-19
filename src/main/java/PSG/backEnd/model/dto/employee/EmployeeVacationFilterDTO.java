package PSG.backEnd.model.dto.employee;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
@Schema(description = "Filter Data Transfer Object for employee vacations. " +
        "Used to filter and search vacation records by employee, date range, and minimum/maximum days.")
public record EmployeeVacationFilterDTO(
    @Schema(description = "Filter by employee ID. Returns only vacation records for the specified employee.",
            example = "25",
            nullable = true)
    Long employeeId,

    @Schema(description = "Filter by employee last name. Partial match search (case-insensitive).",
            example = "García",
            nullable = true)
    String employeeLastName,
    @Schema(description = "Filter by minimum start date. Returns vacation periods starting from this date onwards.",
            example = "2025-01-01",
            nullable = true)
    LocalDate startDateFrom,
    @Schema(description = "Filter by maximum start date. Returns vacation periods starting up to this date.",
            example = "2025-12-31",
            nullable = true)
    LocalDate startDateTo,
    @Schema(description = "Filter by minimum end date. Returns vacation periods ending from this date onwards.",
            example = "2025-01-01",
            nullable = true)
    LocalDate endDateFrom,
    @Schema(description = "Filter by maximum end date. Returns vacation periods ending up to this date.",
            example = "2025-12-31",
            nullable = true)
    LocalDate endDateTo,
    @Schema(description = "Filter by minimum number of days. Returns vacations with at least this many days.",
            example = "5",
            nullable = true)
    Integer minTotalDays,
    @Schema(description = "Filter by maximum number of days. Returns vacations with at most this many days.",
            example = "30",
            nullable = true)
    Integer maxTotalDays
) {}
