package PSG.backEnd.model.dto.crewAssignment;

import PSG.backEnd.model.enums.vehicle.CrewReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Request DTO for creating or updating a daily crew report with its assignments.")
public record DailyCrewReportSaveDTO(
    @Schema(description = "Date of the report.", example = "2026-04-04")
    @NotNull(message = "{validation.required}")
    LocalDate date,

    @Schema(description = "Project area ID. Null for MIXED type.", nullable = true)
    Long projectAreaId,

    @Schema(description = "Report type: SECTOR or MIXED.", example = "SECTOR")
    @NotNull(message = "{validation.required}")
    CrewReportType type,

    @Schema(description = "Default departure time.", nullable = true)
    LocalTime departureTime,

    @Schema(description = "Default return time.", nullable = true)
    LocalTime returnTime,

    @Schema(description = "Assignments for this report.")
    @NotNull(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size.max}")
    @Valid
    List<CrewReportAssignmentDTO> assignments
) {}
