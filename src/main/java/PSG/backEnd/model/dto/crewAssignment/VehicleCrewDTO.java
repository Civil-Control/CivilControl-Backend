package PSG.backEnd.model.dto.crewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A vehicle with its assigned crew members for a day.")
public record VehicleCrewDTO(
    @Schema(description = "Vehicle ID.", example = "10")
    Long vehicleId,
    @Schema(description = "Vehicle license plate.", example = "ABC-123")
    String vehicleLicensePlate,
    @Schema(description = "Vehicle nickname.", nullable = true)
    String vehicleNickName,
    @Schema(description = "Vehicle brand.", nullable = true)
    String vehicleBrand,
    @Schema(description = "Vehicle model.", nullable = true)
    String vehicleModel,
    @Schema(description = "Project area ID.", example = "3")
    Long projectAreaId,
    @Schema(description = "Project area name.", example = "Zona Norte")
    String projectAreaName,
    @Schema(description = "Project area color.", example = "#FF5733", nullable = true)
    String projectAreaColor,
    @Schema(description = "Vehicle mileage registered for this day.", example = "45000", nullable = true)
    Integer km,
    @Schema(description = "Crew members assigned to this vehicle.")
    List<CrewMemberDTO> members
) {}
