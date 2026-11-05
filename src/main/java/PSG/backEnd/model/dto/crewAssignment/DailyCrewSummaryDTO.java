package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Daily crew summary grouped by vehicle.")
public record DailyCrewSummaryDTO(
    @Schema(description = "The date of the summary.", example = "2026-04-04")
    LocalDate date,
    @Schema(description = "List of vehicles with their crew members.")
    List<VehicleCrewDTO> vehicles
) {}
