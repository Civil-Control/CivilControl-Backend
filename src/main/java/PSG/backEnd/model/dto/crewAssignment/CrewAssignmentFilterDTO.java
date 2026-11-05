package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Filter DTO for crew assignment queries.")
public record CrewAssignmentFilterDTO(
    @Schema(description = "Filter by employee ID.", nullable = true)
    Long employeeId,
    @Schema(description = "Filter by employee first name (partial match).", nullable = true)
    String employeeName,
    @Schema(description = "Filter by employee last name (partial match).", nullable = true)
    String employeeLastName,
    @Schema(description = "Filter by employee DNI.", nullable = true)
    String employeeDni,
    @Schema(description = "Filter by vehicle ID.", nullable = true)
    Long vehicleId,
    @Schema(description = "Filter by vehicle license plate (partial match).", nullable = true)
    String vehicleLicensePlate,
    @Schema(description = "Filter by project area ID.", nullable = true)
    Long projectAreaId,
    @Schema(description = "Filter by minimum date (inclusive).", nullable = true)
    LocalDate dateFrom,
    @Schema(description = "Filter by maximum date (inclusive).", nullable = true)
    LocalDate dateTo,
    @Schema(description = "Filter by exact date.", nullable = true)
    LocalDate dateExact,
    @Schema(description = "Filter by driver flag.", nullable = true)
    Boolean isDriver,
    @Schema(description = "Generic search across employee name, last name, DNI, license plate.", nullable = true)
    String search
) {}
