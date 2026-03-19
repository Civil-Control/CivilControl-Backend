package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.employee.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Filter Data Transfer Object for disciplinary actions. " +
        "Used to filter and search disciplinary actions by multiple criteria including employee, action type, and date ranges.")
public record DisciplinaryActionFilterDTO(
    @Schema(description = "Filter by employee ID. Returns only disciplinary actions for the specified employee.",
            example = "15",
            nullable = true)
    Long employeeId,

    @Schema(description = "Generic search across employee name and last name (case-insensitive partial match).",
            example = "García",
            nullable = true)
    String employeeSearch,

    @Schema(description = "Filter by action type. Values: WARNING, SUSPENSION, TERMINATION.",
            example = "WARNING",
            nullable = true)
    ActionType actionType,

    @Schema(description = "Filter by minimum action date. Returns disciplinary actions from this date onwards.",
            example = "2025-01-01",
            nullable = true)
    LocalDate actionDateFrom,

    @Schema(description = "Filter by maximum action date. Returns disciplinary actions up to this date.",
            example = "2025-12-31",
            nullable = true)
    LocalDate actionDateTo,

    @Schema(description = "Filter by minimum end date. Returns disciplinary actions with end date from this date onwards.",
            example = "2025-01-01",
            nullable = true)
    LocalDate endDateFrom,

    @Schema(description = "Filter by maximum end date. Returns disciplinary actions with end date up to this date.",
            example = "2025-12-31",
            nullable = true)
    LocalDate endDateTo
) {}

