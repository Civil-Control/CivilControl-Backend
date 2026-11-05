package PSG.backEnd.model.dto.crewAssignment;

import PSG.backEnd.model.enums.vehicle.CrewReportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

@Schema(description = "Summary info for a single crew report (without full assignment details).")
public record DailyReportSummaryItemDTO(
    @Schema(description = "Report ID.", example = "1")
    Long id,

    @Schema(description = "Project area ID. Null for MIXED.", nullable = true)
    Long projectAreaId,
    @Schema(description = "Project area name.", nullable = true)
    String projectAreaName,
    @Schema(description = "Project area color.", nullable = true)
    String projectAreaColor,

    @Schema(description = "Report type.", example = "SECTOR")
    CrewReportType type,

    @Schema(description = "Departure time.", nullable = true)
    LocalTime departureTime,
    @Schema(description = "Return time.", nullable = true)
    LocalTime returnTime,

    @Schema(description = "Number of vehicles in this report.", example = "3")
    int vehicleCount,
    @Schema(description = "Number of employees in this report.", example = "8")
    int employeeCount
) {}
