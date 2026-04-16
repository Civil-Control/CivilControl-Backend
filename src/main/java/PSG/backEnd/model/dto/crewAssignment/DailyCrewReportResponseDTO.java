package PSG.backEnd.model.dto.crewAssignment;

import PSG.backEnd.model.enums.vehicle.CrewReportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Full response for a daily crew report including vehicles and members.")
public record DailyCrewReportResponseDTO(
    @Schema(description = "Report ID.", example = "1")
    Long id,

    @Schema(description = "Report date.", example = "2026-04-04")
    LocalDate date,

    @Schema(description = "Project area ID. Null for MIXED type.", nullable = true)
    Long projectAreaId,
    @Schema(description = "Project area name.", nullable = true)
    String projectAreaName,
    @Schema(description = "Project area color.", nullable = true)
    String projectAreaColor,

    @Schema(description = "Report type.", example = "SECTOR")
    CrewReportType type,

    @Schema(description = "Default departure time.", nullable = true)
    LocalTime departureTime,
    @Schema(description = "Default return time.", nullable = true)
    LocalTime returnTime,

    @Schema(description = "Vehicles with crew members.")
    List<VehicleCrewDTO> vehicles,

    @Schema(description = "Non-blocking warnings.")
    List<CrewAssignmentWarningDTO> warnings
) {}
