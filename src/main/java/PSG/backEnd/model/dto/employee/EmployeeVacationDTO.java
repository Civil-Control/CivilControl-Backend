package PSG.backEnd.model.dto.employee;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
@Schema(description = "Data Transfer Object for creating or updating an employee vacation period. " +
        "Represents a vacation request or record including start date, end date, duration, and optional notes.")
public record EmployeeVacationDTO(
    @Schema(description = "Unique identifier of the employee taking the vacation. " +
            "Must reference an existing employee in the system.",
            example = "25",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    Long employeeId,
    @Schema(description = "Start date of the vacation period. " +
            "Cannot be in the distant past. Should be a reasonable date for vacation planning.",
            example = "2025-12-20",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    LocalDate startDate,
    @Schema(description = "End date of the vacation period. " +
            "Must be equal to or after the start date. Cannot be in the distant past.",
            example = "2025-12-31",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    LocalDate endDate,
    @Schema(description = "Total number of vacation days. Must be a positive integer. " +
            "Should be consistent with the date range (business days between start and end date).",
            example = "10",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @Min(value = 1, message = "{vacation.totalDays.positive}", groups = {OnCreate.class, OnUpdate.class})
    @Max(value = 365, message = "{validation.max}", groups = {OnCreate.class, OnUpdate.class})
    Integer totalDays,
    @Schema(description = "Optional observations, notes, or comments about the vacation period. " +
            "Can include reasons, special considerations, or administrative notes. Maximum 500 characters.",
            example = "Vacaciones de fin de a?o - Aprobadas por gerencia",
            maxLength = 500,
            nullable = true)
    @Size(max = 500, message = "{vacation.observations.size}", groups = {OnCreate.class, OnUpdate.class})
    String observations
) {}
