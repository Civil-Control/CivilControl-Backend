package PSG.backEnd.model.dto.employee;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
@Schema(description = "Response Data Transfer Object for employee vacation. " +
        "Contains complete information about a vacation period including employee details and vacation specifics.")
public record EmployeeVacationResponseDTO(
    @Schema(description = "Unique identifier of the vacation record.",
            example = "15")
    Long id,
    @Schema(description = "Unique identifier of the employee taking the vacation.",
            example = "25")
    Long employeeId,
    @Schema(description = "First name of the employee taking the vacation.",
            example = "Juan Carlos")
    String employeeName,
    @Schema(description = "Last name of the employee taking the vacation.",
            example = "Garc?a P?rez")
    String employeeLastName,
    @Schema(description = "Start date of the vacation period.",
            example = "2025-12-20")
    LocalDate startDate,
    @Schema(description = "End date of the vacation period.",
            example = "2025-12-31")
    LocalDate endDate,
    @Schema(description = "Total number of vacation days.",
            example = "10")
    Integer totalDays,
    @Schema(description = "Observations, notes, or comments about the vacation period.",
            example = "Vacaciones de fin de a?o - Aprobadas por gerencia",
            nullable = true)
    String observations
) {}
