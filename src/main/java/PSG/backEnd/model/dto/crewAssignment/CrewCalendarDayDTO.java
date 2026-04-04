package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Summary of a single day for the calendar view.")
public record CrewCalendarDayDTO(
    @Schema(description = "The date.", example = "2026-04-04")
    LocalDate date,
    @Schema(description = "Number of vehicles that went out.", example = "4")
    int vehicleCount,
    @Schema(description = "Number of employees assigned.", example = "12")
    int employeeCount
) {}
